package com.studyblock.domain.payment.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.domain.course.dto.CourseDetailResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.payment.dto.PaymentPageResponse;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

//결제페이지정보조회 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPageService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCouponRepository userCouponRepository;
    private final WalletService walletService;

    //결제페이지정보조회 (단일 강의)
    @Transactional(readOnly = true)
    public PaymentPageResponse getPaymentPageInfo(Long courseId, Long userId) {
        log.info("결제 페이지 정보 조회 시작 - courseId: {}, userId: {}", courseId, userId);
        
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // 2. 강의 정보 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다"));
        
        // 강의 총 강의 수 계산
        int totalLectures = course.getLectures() != null ? course.getLectures().size() : 0;
        CourseDetailResponse courseResponse = CourseDetailResponse.from(course, totalLectures);
        
        // 3. 사용자 정보 생성
        PaymentPageResponse.UserInfo userInfo = createUserInfo(user);
        
        // 4. 사용 가능한 쿠폰 목록 조회
        List<PaymentPageResponse.AvailableCoupon> couponList = getAvailableCoupons(userId);
        
        // 5. 쿠키 잔액 조회
        Long cookieBalance = walletService.getCookieBalance(userId);
        
        // 6. 응답 생성
        PaymentPageResponse response = PaymentPageResponse.builder()
                .course(courseResponse)
                .courses(java.util.Collections.singletonList(courseResponse)) // 여러 강의 지원을 위해 추가
                .user(userInfo)
                .availableCoupons(couponList)
                .cookieBalance(cookieBalance)
                .build();
        
        log.info("결제 페이지 정보 조회 완료 - courseId: {}, userId: {}", courseId, userId);
        return response;
    }
    
    //결제페이지정보조회 (여러 강의)
    @Transactional(readOnly = true)
    public PaymentPageResponse getPaymentPageInfoForMultipleCourses(List<Long> courseIds, Long userId) {
        log.info("결제 페이지 정보 조회 시작 (여러 강의) - courseIds: {}, userId: {}", courseIds, userId);
        
        // 1. 기본 검증
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("강의 ID 목록이 비어있습니다");
        }
        
        // 2. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // 3. 강의 정보 조회 (모든 강의를 한 번에 조회)
        List<CourseDetailResponse> courseResponses = new java.util.ArrayList<>();
        List<Long> notFoundCourseIds = new java.util.ArrayList<>();
        
        for (Long courseId : courseIds) {
            try {
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID: " + courseId));
                
                // 강의가 공개 상태인지 확인
                if (!course.getIsPublished()) {
                    throw new IllegalStateException("구매할 수 없는 강의입니다. ID: " + courseId);
                }
                
                // 강의 가격이 설정되어 있는지 확인
                if (course.getPrice() == null || course.getPrice() < 0) {
                    throw new IllegalStateException("강의 가격 정보가 올바르지 않습니다. ID: " + courseId);
                }
                
                int totalLectures = course.getLectures() != null ? course.getLectures().size() : 0;
                CourseDetailResponse courseResponse = CourseDetailResponse.from(course, totalLectures);
                courseResponses.add(courseResponse);
                
                log.info("강의 정보 조회 성공 - courseId: {}, title: {}", courseId, course.getTitle());
            } catch (Exception e) {
                log.error("강의 정보 조회 실패 - courseId: {}, error: {}", courseId, e.getMessage());
                notFoundCourseIds.add(courseId);
            }
        }
        
        // 4. 찾지 못한 강의가 있으면 상세한 오류 메시지 반환
        if (!notFoundCourseIds.isEmpty()) {
            String errorMessage = String.format(
                "다음 강의 정보를 불러올 수 없습니다: %s. 총 %d개 중 %d개 강의를 찾을 수 없습니다.",
                notFoundCourseIds.toString(),
                courseIds.size(),
                notFoundCourseIds.size()
            );
            throw new IllegalArgumentException(errorMessage);
        }
        
        // 5. 사용자 정보 생성
        PaymentPageResponse.UserInfo userInfo = createUserInfo(user);
        
        // 6. 사용 가능한 쿠폰 목록 조회
        List<PaymentPageResponse.AvailableCoupon> couponList = getAvailableCoupons(userId);
        
        // 7. 쿠키 잔액 조회
        Long cookieBalance = walletService.getCookieBalance(userId);
        
        // 8. 응답 생성 (첫 번째 강의를 단일 강의 필드에도 설정 - 하위 호환성)
        PaymentPageResponse response = PaymentPageResponse.builder()
                .course(courseResponses.isEmpty() ? null : courseResponses.get(0))
                .courses(courseResponses)
                .user(userInfo)
                .availableCoupons(couponList)
                .cookieBalance(cookieBalance)
                .build();
        
        log.info("결제 페이지 정보 조회 완료 (여러 강의) - courseIds: {}, userId: {}, found: {}",
                courseIds, userId, courseResponses.size());
        return response;
    }
    
    // 사용자 정보 생성 헬퍼 메서드
    private PaymentPageResponse.UserInfo createUserInfo(User user) {
        return PaymentPageResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }
    
    // 사용 가능한 쿠폰 목록 조회 헬퍼 메서드
    private List<PaymentPageResponse.AvailableCoupon> getAvailableCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> availableCoupons = userCouponRepository.findAvailableCouponsByUserId(userId, now);
        
        return availableCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = userCoupon.getCoupon();
                    return PaymentPageResponse.AvailableCoupon.builder()
                            .userCouponId(userCoupon.getId())
                            .couponName(coupon.getName())
                            .discountAmount(coupon.getDiscountValue())
                            .discountPercentage(null) // 필요시 추가
                            .minimumAmount(coupon.getMinimumAmount())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
