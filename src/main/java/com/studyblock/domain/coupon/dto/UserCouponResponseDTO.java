package com.studyblock.domain.coupon.dto;

import com.studyblock.domain.coupon.entity.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCouponResponseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private CouponResponse coupon;
    private String couponCode;
    private Boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static UserCouponResponseDTO from(UserCoupon userCoupon) {
        if (userCoupon == null) {
            throw new IllegalArgumentException("UserCoupon은 null일 수 없습니다");
        }
        
        // null 체크
        if (userCoupon.getUser() == null) {
            throw new IllegalStateException("UserCoupon에 User 정보가 없습니다. userCouponId: " + userCoupon.getId());
        }
        
        if (userCoupon.getCoupon() == null) {
            throw new IllegalStateException("UserCoupon에 Coupon 정보가 없습니다. userCouponId: " + userCoupon.getId());
        }
        
        return UserCouponResponseDTO.builder()
                .id(userCoupon.getId())
                .userId(userCoupon.getUser().getId())
                .userName(userCoupon.getUser().getName())
                .userEmail(userCoupon.getUser().getEmail())
                .coupon(CouponResponse.from(userCoupon.getCoupon()))
                .couponCode(userCoupon.getCouponCode())
                .isUsed(userCoupon.getIsUsed())
                .usedAt(userCoupon.getUsedAt())
                .expiresAt(userCoupon.getExpiresAt())
                .createdAt(userCoupon.getCreatedAt())
                .build();
    }
}
