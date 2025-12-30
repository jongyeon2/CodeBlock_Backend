package com.studyblock.global.security.oauth2;

import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.category.entity.UserCategory;
import com.studyblock.domain.category.repository.CategoryRepository;
import com.studyblock.domain.category.repository.UserCategoryRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.enums.JoinType;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 로그인 처리 서비스
 * - User와 UserProfile 테이블 병합 완료
 * - 이메일 통합 정책 적용: 1 이메일 = 1 계정
 * - 같은 이메일로 여러 SNS 로그인 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserCategoryRepository userCategoryRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. SNS에서 사용자 정보 가져오기
        OAuth2User oauth2User = super.loadUser(userRequest);

        // 2. 제공자 구분 (kakao, google, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 제공자별 사용자 정보 파싱
        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oauth2User.getAttributes());
        JoinType providerType = JoinType.fromString(userInfo.getProvider());

        // ===== 핵심 변경: 이메일 우선 조회 (이메일 통합 정책) =====
        String email = userInfo.getEmail();

        // 4. 이메일로 기존 User 찾기 (핵심!)
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    log.info("✅ 기존 User 발견 - email: {}, 최초 가입경로: {}, 새 로그인: {}",
                            email, existingUser.getJoinTypeEnum(), providerType);

                    // 정보 업데이트
                    existingUser.updateOAuth2Info(email, userInfo.getName());
                    existingUser.updateOAuthProvider(providerType, userInfo.getProviderId());

                    return existingUser;
                })
                // 5. 없으면 신규 User 생성
                .orElseGet(() -> {
                    log.info("🆕 신규 User 생성 - email: {}, provider: {}", email, providerType);

                    User newUser = createNewUser(userInfo, providerType);

                    userRepository.save(newUser);
                    addDefaultCategories(newUser);

                    return newUser;
                    //return createNewUser(userInfo, providerType);
                });

        // 6. 저장 (업데이트 또는 신규 생성)
        userRepository.save(user);

        // 7. Spring Security가 사용할 객체 반환
        return new PrincipalDetails(user, oauth2User.getAttributes());
    }

    /**
     * 제공자별 UserInfo 객체 생성 (카카오, 네이버, 구글 지원)
     */
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + registrationId);
        };
    }

    /**
     * 신규 OAuth2 사용자 생성
     * - UserProfile 테이블과 병합되어 프로필 정보도 함께 생성
     */
    private User createNewUser(OAuth2UserInfo userInfo, JoinType providerType) {
        return User.createOAuth2User(
                providerType,
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getProfileImageUrl(),
                LocalDate.of(2000, 1, 1),  // 기본 생년월일 (마이페이지에서 수정)
                0  // 기본 성별: FEMALE (마이페이지에서 수정)
        );
    }

    //회원가입 시 기본으로 즐겨찾는 카테고리 6개 추가
    private void addDefaultCategories(User user) {
        List<Long> defaultCategoryIds = List.of(7L, 12L, 17L, 21L, 28L, 31L);

        for (Long categoryId : defaultCategoryIds) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                UserCategory userCategory = UserCategory.of(user, category);
                userCategoryRepository.save(userCategory);
            }
        }

        log.info("OAuth 신규 사용자 {}에게 기본 6개 카테고리 추가 완료", user.getId());
    }
}
