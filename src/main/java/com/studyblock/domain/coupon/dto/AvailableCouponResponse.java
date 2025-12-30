package com.studyblock.domain.coupon.dto;

import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 사용 가능한 쿠폰 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCouponResponse {
    
    private Long userCouponId;
    private Long couponId;
    private String couponCode;
    private String couponName;
    private String description;
    private CouponType type;
    private Integer discountValue;
    private Integer minimumAmount;
    private Integer maximumDiscount;
    private LocalDateTime expiresAt;
    
    public static AvailableCouponResponse from(UserCoupon userCoupon) {
        if (userCoupon == null) {
            throw new IllegalArgumentException("UserCoupon은 null일 수 없습니다");
        }
        
        var coupon = userCoupon.getCoupon();
        if (coupon == null) {
            throw new IllegalStateException("UserCoupon에 Coupon 정보가 없습니다. userCouponId: " + userCoupon.getId());
        }
        
        return AvailableCouponResponse.builder()
                .userCouponId(userCoupon.getId())
                .couponId(coupon.getId())
                .couponCode(userCoupon.getCouponCode())
                .couponName(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .discountValue(coupon.getDiscountValue())
                .minimumAmount(coupon.getMinimumAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .expiresAt(userCoupon.getExpiresAt())
                .build();
    }
}

