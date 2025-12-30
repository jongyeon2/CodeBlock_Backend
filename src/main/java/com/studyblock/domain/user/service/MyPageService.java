package com.studyblock.domain.user.service;

import com.studyblock.domain.community.dto.PostResponse;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.repository.CommentRepository;
import com.studyblock.domain.community.repository.PostRepository;
import com.studyblock.domain.coupon.dto.UserCouponResponseDTO;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.domain.course.dto.CourseResponse;
import com.studyblock.domain.course.dto.CourseReviewResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.CourseReviewRepository;
import com.studyblock.domain.upload.dto.ImageUploadResponse;
import com.studyblock.domain.upload.enums.ImageType;
import com.studyblock.domain.upload.service.ImageUploadService;
import com.studyblock.domain.user.dto.OwnedSectionResponse;
import com.studyblock.domain.user.entity.Wishlist;
import com.studyblock.domain.user.dto.PasswordChangeRequest;
import com.studyblock.domain.user.repository.LectureOwnershipRepository;
import com.studyblock.domain.user.dto.UserProfileResponse;
import com.studyblock.domain.user.dto.UserUpdateDTO;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.user.repository.WishlistRepository;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Transactional
@Slf4j
public class MyPageService {
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final LectureOwnershipRepository lectureOwnershipRepository;
    private final UserCouponRepository userCouponRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CourseRepository courseRepository;
    private final ImageUploadService imageUploadService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final S3StorageService s3StorageService;
    private final CourseReviewRepository courseReviewRepository;

    public MyPageService(UserRepository userRepository, WishlistRepository wishlistRepository, RedisTemplate<String, Object> redisTemplate, PasswordEncoder passwordEncoder, LectureOwnershipRepository lectureOwnershipRepository, UserCouponRepository userCouponRepository, CourseRepository courseRepository, ImageUploadService imageUploadService, PostRepository postRepository, CommentRepository commentRepository, S3StorageService s3StorageService, CourseReviewRepository courseReviewRepository) {
        this.userRepository = userRepository;
        this.wishlistRepository = wishlistRepository;
        this.lectureOwnershipRepository = lectureOwnershipRepository;
        this.userCouponRepository = userCouponRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.courseRepository = courseRepository;
        this.imageUploadService = imageUploadService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.s3StorageService = s3StorageService;
        this.courseReviewRepository = courseReviewRepository;
    }

    //유저 정보 불러오기
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + userId));

        //프로필 이미지 presigned URL 생성
        String profileImageUrl = user.getImg();
        if(profileImageUrl != null && !profileImageUrl.isEmpty()){
            profileImageUrl = applyPresignedURL(profileImageUrl);
        }
        return UserProfileResponse.builder()
                .name(user.getName())
                .intro(user.getIntro())
                .memberId(user.getMemberId())
                .nickname(user.getNickname())
                .email(user.getEmail()).phone(user.getPhone()).birth(user.getBirth()).status(user.getStatusEnum())
                .gender(user.getGenderEnum()).created_at(user.getCreatedAt()).jointype(user.getJoinTypeEnum())
                .img(profileImageUrl)  // presigned URL (30분)
                .build();
    }
    // Presigned URL 변환 (캐시 버스터 추가)
    private String applyPresignedURL(String url){
        if(url == null || url.isEmpty()) return url;
        try {
//            return s3StorageService.generatePresignedUrl(url, 30);
            String presignedUrl = s3StorageService.generatePresignedUrl(url, 30);
            //브라우저에 캐싱 방지를 위한 타임스탬프 추가
            String cacheBuster = "&t=" + System.currentTimeMillis();
            return presignedUrl + cacheBuster;
        } catch (RuntimeException e) {
            return url;
        }
    }

    //유저 프로필 정보 업데이트
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        user.updateProfileWithoutImage(dto.getNickname(), dto.getIntro(), null, dto.getBirth());
    }

    //유저 찜 목록 불러오기
    public List<CourseResponse> getWishlistCourses(Long userId){
        List<Course> courses = wishlistRepository.findCoursesByUserId(userId);

        return courses.stream()
                .map(CourseResponse::from)
                .toList();

    }

    //유저 찜 목록 삭제
    public void removeWish(Long userId, Long courseId){
        wishlistRepository.deleteByUser_IdAndCourse_Id(userId, courseId);
    }

    //유저 찜 토글 (있으면 삭제, 없으면 추가)
    public boolean toggleWishlist(Long userId, Long courseId) {
        Wishlist existingWishlist = wishlistRepository.findByUserIdAndCourseId(userId, courseId);

        if (existingWishlist != null) {
            // 이미 찜한 경우 -> 삭제
            wishlistRepository.delete(existingWishlist);
            return false; // 찜 해제됨
        } else {
            // 찜하지 않은 경우 -> 추가
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + userId));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 코스를 찾을 수 없습니다. ID=" + courseId));

            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .course(course)
                    .build();

            wishlistRepository.save(wishlist);
            return true; // 찜 추가됨
        }
    }

    //유저 찜 여부 확인
    public boolean isInWishlist(Long userId, Long courseId) {
        return wishlistRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    //유저 탈퇴 기능
    public void withdraw(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.updateStatus(2); // 2: 탈퇴로 유저 상태 변경
    }

    //유저 보유 강의
    public List<OwnedSectionResponse> getUserOwn(Long userId){
        List<LectureOwnership> ownerships = lectureOwnershipRepository.findActiveOwnershipsByUserId(userId);
        return ownerships.stream()
                .map(OwnedSectionResponse::from)
                .toList();

    }

    //유저 비밀번호 변경
    //1. 현재 비밀번호 검증
    public String verifyCurrentPassword(Long userId, String currentPassword){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //비밀번호 검증 (로그인과 같은 방식)
        if(!passwordEncoder.matches(currentPassword, user.getPassword())){
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        //임시 토큰 생성 + redis에 저장
        String tempToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "password-change:" + userId,
                tempToken,
                Duration.ofMinutes(5) //5분간 유효
        );

        return tempToken;
    }

    //2. 새 비밀번호로 변경
    public void changePassword(Long userId, PasswordChangeRequest request){
        //redis 토큰 검증
        String storedToken = (String) redisTemplate.opsForValue().get("password-change:" + userId);
        if(storedToken == null || !storedToken.equals(request.getTempToken())){
            throw new IllegalArgumentException("유효하지 않은 요청입니다. 다시 시도해주세요.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        //회원가입과 동일한 방식으로 암호화
        String encodeNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodeNewPassword);

        //redis 토큰 삭제
        redisTemplate.delete("password-change:" + userId);
    }

    //프로필 이미지 업로드
    public String uploadProfileImage(Long userId, MultipartFile file){
        //유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        //S3에 이미지 업로드
        ImageUploadResponse response = imageUploadService.uploadImage(file, ImageType.PROFILE);
        if(!response.getSuccess()){
            throw new RuntimeException("이미지 업로드 실패" + response.getMessage());
        }
        //유저 db 업데이트 (원본 S3 url 저장)
        user.updateImg(response.getUrl());

        //Presigned URL 생성해서 반환
        return applyPresignedURL(response.getUrl());
    }

    //유저 보유 쿠폰 조회
    @Transactional(readOnly = true)
    public List<UserCouponResponseDTO> getUserCoupons(Long userId) {
        // 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + userId);
        }

        try {
            // 사용자 쿠폰 조회 (JOIN FETCH로 LazyInitializationException 방지)
            List<UserCoupon> userCoupons = userCouponRepository.findByUser_IdWithCoupon(userId);

            if (userCoupons == null || userCoupons.isEmpty()) {
                return List.of(); // 빈 리스트 반환
            }

            // DTO로 변환 (null 체크 포함)
            // 트랜잭션 내에서 DTO 변환 완료 (LazyInitializationException 방지)
            List<UserCouponResponseDTO> result = userCoupons.stream()
                    .filter(uc -> uc != null && uc.getCoupon() != null && uc.getUser() != null)
                    .map(UserCouponResponseDTO::from)
                    .toList();

            return result;

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 의도된 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외는 RuntimeException으로 래핑
            throw new RuntimeException("쿠폰 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    //문의 내역 불러오기
    public List<PostResponse> getMyQnA(Long userId){
        List<Post> posts = postRepository.findActiveByUserIdAndBoardIdOrderByCreatedAtDesc(userId, 5L);

        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> commentCountMap = commentRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                   arr -> (Long) arr[0],
                   arr -> ((Long) arr[1]).intValue()
                ));
        return posts.stream()
                .map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0)))
                .map(this::applyPresignedUrls)
                .collect(Collectors.toList());
    }
//    s3 persigned 적용
    private PostResponse applyPresignedUrls(PostResponse response){
        if(response == null || response.getImageUrls() == null) return response;
        List<String> signedUrls = response.getImageUrls().stream()
                .map(url -> {
                    try {
                        return s3StorageService.generatePresignedUrl(url, 30); //30분 유효
                    } catch (RuntimeException e){
                        return url; //실패 시 원본 url 유지
                    }
                })
                .collect(Collectors.toList());
        response.setImageUrls(signedUrls);
        return response;
    }


    //내가 작성한 강의 리뷰
    @Transactional(readOnly = true)
    public List<CourseReviewResponse> getMyReviews(Long userId) {
        // 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + userId);
        }

        // DTO Projection 방식: 필요한 데이터만 조회하여 성능 최적화
        // Repository에서 이미 DTO로 변환되어 반환되므로 별도의 변환 불필요
        return courseReviewRepository.findByUserIdWithDetails(userId);
    }

    //내가 작성한 강의평 카운트
    @Transactional(readOnly = true)
    public long getMyReviewCount(Long userId) {
        return courseReviewRepository.countByUserId(userId);  // Repository 메서드 호출만!
    }

}
