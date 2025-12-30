package com.studyblock.domain.payment.dto;

import com.studyblock.domain.course.dto.CourseDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 결제 페이지 정보 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPageResponse {
    
    // 강의 정보 (단일 강의 결제용 - 하위 호환성 유지)
    private CourseDetailResponse course;
    
    // 강의 목록 (여러 강의 결제용)
    private List<CourseDetailResponse> courses;
    
    // 사용자 정보
    private UserInfo user;
    
    // 사용 가능한 쿠폰 목록 (선택)
    private List<AvailableCoupon> availableCoupons;
    
    // 쿠키 잔액 (선택)
    private Long cookieBalance;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String nickname;
        private String phone;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableCoupon {
        private Long userCouponId;
        private String couponName;
        private Integer discountAmount;
        private Integer discountPercentage;
        private Integer minimumAmount;
    }
}

