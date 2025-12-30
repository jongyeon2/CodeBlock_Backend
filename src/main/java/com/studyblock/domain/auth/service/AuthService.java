package com.studyblock.domain.auth.service;

/*
    Local 회원가입을 위한 비즈니스 로직
 */


import com.studyblock.domain.activitylog.enums.ActionType;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.auth.dto.LocalSignupRequest;
import com.studyblock.domain.auth.dto.LoginRequest;
import com.studyblock.domain.auth.dto.UserInfo;
import com.studyblock.domain.auth.entity.Role;
import com.studyblock.domain.auth.entity.UserRole;
import com.studyblock.domain.auth.enums.RoleCode;
import com.studyblock.domain.auth.repository.RoleRepository;
import com.studyblock.domain.auth.repository.UserRoleRepository;
import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.category.entity.UserCategory;
import com.studyblock.domain.category.repository.CategoryRepository;
import com.studyblock.domain.category.repository.UserCategoryRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.enums.UserStatus;
import com.studyblock.domain.user.enums.JoinType;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.global.security.jwt.JwtTokenProvider;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int PROFILE_IMAGE_URL_EXPIRATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final S3StorageService s3StorageService;
    private final ActivityLogService activityLogService;
    private final CategoryRepository categoryRepository;
    private final UserCategoryRepository userCategoryRepository;

    /*
        로컬 회원 가입 처리
        - 아이디/이메일 중복 체크
        - 비밀번호 암호화
        - User 엔티티 생성 및 저장
        - JWT 토큰 발급
     */
    @Transactional
    public Map<String, String> registerLocalUser(LocalSignupRequest request) {
        // 1. 중복 검사
        // DB에서 요청 받은 아이디랑 검증시 isPresent()로 확인시 있으면?
        if (userRepository.findByMemberId(request.getMemberId()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. User 엔티티 생성
        User user = User.builder()
                .memberId(request.getMemberId())
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .birth(request.getBirth())
                .phone(request.getPhone())
                .gender(request.getGender())
                .jointype(JoinType.LOCAL.getValue()) // 0 = LOCAL
                .oauthProviderId(null) // 로컬 가입은 OAuth ID 없기 때문에
                // 프로필 초기값 (UserProfile 테이블 병합으로 추가된 필드)
                .nickname(request.getName()) // 초기 닉네임 = 이름
                .intro("")
                .img(null)
                .interests("")
                .build();

        // 4. DB 저장
        // save() 메서드는 Spring JPA 기능 떄문에 가능 (기본 CRUD)
        User savedUser = userRepository.save(user);

        // 로컬 회원가입 한 USER에게 기본 카테고리 추가
        addDefaultCategories(savedUser);

        // 로그 남기기
        log.info("로컬 회원가입 완료 - userId: {}, memberId: {}, email: {}, isCreator: {}",
                savedUser.getId(), savedUser.getMemberId(), savedUser.getEmail(), request.getIsCreator());

        // 활동 로그 저장
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("memberId", savedUser.getMemberId());
        metadata.put("email", savedUser.getEmail());
        metadata.put("isCreator", request.getIsCreator());
        metadata.put("joinType", "LOCAL");

        activityLogService.createLog(
                savedUser.getId(),
                ActionType.SIGNUP,
                "USER",
                savedUser.getId(),
                String.format("%s 님이 회원가입 하였습니다.", savedUser.getName()),
                null,
                metadata
        );

        // 4.5. 역할 자동 부여 (isCreator에 따라 분기)
        if (Boolean.TRUE.equals(request.getIsCreator())) {
            // 강사 회원가입: isCreator=true 설정 + INSTRUCTOR 역할 부여
            savedUser.promoteToCreator();
            userRepository.save(savedUser);  // isCreator 변경사항 저장

            Role instructorRole = roleRepository.findByCode(RoleCode.INSTRUCTOR)
                    .orElseThrow(() -> new IllegalStateException("INSTRUCTOR 역할이 데이터베이스에 존재하지 않습니다."));

            UserRole instructorRoleMapping = UserRole.builder()
                    .user(savedUser)
                    .role(instructorRole)
                    .grantedBy(null)  // 자동 부여이므로 null
                    .build();

            userRoleRepository.save(instructorRoleMapping);

            log.info("INSTRUCTOR 역할 자동 부여 완료 - userId: {}", savedUser.getId());
        } else {
            // 일반 회원가입: USER 역할 부여
            Role userRole = roleRepository.findByCode(RoleCode.USER)
                    .orElseThrow(() -> new IllegalStateException("USER 역할이 데이터베이스에 존재하지 않습니다."));

            UserRole userRoleMapping = UserRole.builder()
                    .user(savedUser)
                    .role(userRole)
                    .grantedBy(null)  // 자동 부여이므로 null
                    .build();

            userRoleRepository.save(userRoleMapping);

            log.info("USER 역할 자동 부여 완료 - userId: {}", savedUser.getId());
        }

        // 5. JWT 토큰 발급 (회원가입 즉시 로그인 처리)
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());

        // 엑세스 토큰과 리프레쉬 토큰 다시 반환
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    // 유효성 검사

    // 아이디 중복 확인
    public boolean isMemberIdAvailable(String memberId) {
        return userRepository.findByMemberId(memberId).isEmpty();
    }

    // 이메일 중복 확인
    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }

    // 이메일 인증 코드 전송 (추후 구현)
    public void sendVerificationCode(String email) {
        // TODO: EmailService를 통해 인증 코드 전송
        log.info("이메일 인증 코드 전송 요청 - email: {}", email);
        // emailService.sendVerificationCode(email);
    }

    // 이메일 인증 코드 확인 (추후 구현)
    public boolean verifyEmailCode(String email, String code) {
        // TODO: Redis나 DB에 저장된 인증 코드와 비교
        log.info("이메일 인증 코드 확인 - email: {}, code: {}", email, code);
        // return emailService.verifyCode(email, code);
        return true; // 임시로 true 반환 (이메일 인증 기능 구현 전까지)
    }

    /*
        로컬 로그인 처리
        - memberId로 사용자 조회
        - 비밀번호 검증
        - JWT 토큰 발급
        - RefreshToken을 Redis에 저장한다.

        @param loginRequest 로그인 요청 (memberId, password)
        @return Map<String, String> (accessToken, refreshToken)
     */

    @Transactional
    public Map<String, Object> loginLocalUser(LoginRequest request) {

        // 1. memberId로 사용자 조회 (역할 정보 포함 - Fetch Join)
        // QueryDSL을 사용하여 User + UserRole + Role을 한 번의 쿼리로 조회
        User user = userRepository.findByMemberIdWithRoles(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 2. 비밀번호 검증
        // passwordEncoder를 사용해 비교한다.
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 2-1. 사용자 활성 상태 확인
        if (!user.isActive()) {
            UserStatus status = user.getStatusEnum();
            String statusMessage = switch (status) {
                case SUSPENDED -> "차단된 사용자입니다. 관리자에게 문의하세요.";
                case WITHDRAWN -> "탈퇴한 사용자입니다.";
                case DORMANT -> "휴면 상태인 사용자입니다.";
                default -> "비활성화된 사용자입니다.";
            };
            throw new IllegalArgumentException(statusMessage);
        }

        // 3. 역할 정보 추출
        // UserRole → Role → RoleCode를 문자열로 변환 (예: ["ADMIN"], ["USER"], ["INSTRUCTOR"])
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getCode().name())
                .collect(java.util.stream.Collectors.toList());

        // 4. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId()); // 액세스 토큰 발급
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId()); // 리프레쉬 토큰 발급

        // 5. RefreshToken을 Redis에 저장 (TTL 7일)
        redisRefreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        // 로그인에 성공한 유저의 아이디와 역할 로그 남김
        log.info("로그인 성공 - userId: {}, memberId: {}, roles: {}",
                user.getId(), user.getMemberId(), roles);

        // 6. 토큰과 사용자 정보 반환 (역할 정보 포함)
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.getId(),
                "memberId", user.getMemberId(),
                "name", user.getName(),
                "roles", roles  // 역할 정보 추가!
        );
    }

    // 기존 getCurrentUserInfo() 메서드를 다음과 같이 수정
    public UserInfo getCurrentUserInfo(String accessToken) {
        // 1. JWT 토큰 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            log.info("JWT 토큰 검증 실패");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }

        // 2. 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 3. DB에서 사용자 정보 조회 (역할 정보 포함 - Fetch Join)
        log.info("DB에서 사용자 조회 시도 - userId: {}", userId);
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없습니다. - userId: {}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        log.info("사용자 조회 성공 - userId: {}, memberId: {}, name: {}",
                user.getId(), user.getMemberId(), user.getName());

        // 4. 사용자 활성 상태 확인
        if (!user.isActive()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다");
        }

        // 5. 역할 정보 추출
        // UserRole → Role → RoleCode를 문자열로 변환 (예: ["ADMIN"], ["USER"], ["INSTRUCTOR"])
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getCode().name())
                .collect(java.util.stream.Collectors.toList());

        log.info("사용자 역할 정보 - userId: {}, roles: {}", userId, roles);

        // 6. 사용자 정보 반환 (역할 정보 포함)
        String originalImageUrl = user.getImg();
        String profileImageUrl = null;
        if (originalImageUrl != null && !originalImageUrl.isBlank()) {
            try {
                profileImageUrl = s3StorageService.generatePresignedUrl(originalImageUrl, PROFILE_IMAGE_URL_EXPIRATION_MINUTES);
            } catch (RuntimeException e) {
                log.warn("AuthService presigned URL 생성 실패 - userId: {}, key: {}", userId, originalImageUrl, e);
            }
        }

        return UserInfo.builder()
                .id(user.getId())
                .memberId(user.getMemberId())
                .name(user.getName())
                .email(user.getEmail())
                .img(originalImageUrl)
                .profileImageUrl(profileImageUrl)
                .roles(roles)  // 실제 역할 정보 반환
                .build();
    }

    /**
     * 비밀번호 찾기 - 비밀번호 재설정
     * - 이메일로 User 찾기 (이메일 통합 정책: 1 이메일 = 1 계정)
     * - 비밀번호 암호화 후 업데이트
     * - DB 저장
     *
     * @param email       사용자 이메일
     * @param newPassword 새 비밀번호 (평문)
     */
    @Transactional
    public void resetPassword(String email, String newPassword) {
        // 1. 이메일로 User 찾기 (이메일 통합 정책: 1 이메일 = 1 계정)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("비밀번호 재설정 실패 - 가입되지 않은 이메일: {}", email);
                    return new IllegalArgumentException("가입되지 않은 이메일입니다.");
                });

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 3. User 엔티티의 비밀번호 업데이트
        user.updatePassword(encodedPassword);

        // 4. DB 저장
        userRepository.save(user);

        log.info("비밀번호 재설정 완료 - userId: {}, email: {}", user.getId(), email);
    }

    //회원가입 시 카테고리 6개 추가
    private void addDefaultCategories(User user){

        List<Long> defaultCategoryIds = List.of(7L, 12L, 17L, 21L, 28L, 31L);

        for(Long categoryId: defaultCategoryIds) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                UserCategory userCategory = UserCategory.of(user, category);
                userCategoryRepository.save(userCategory);
            }
        }

        log.info("사용자 {}에게 기본 6개 카테고리 추가 완료", user.getId());
    }

}