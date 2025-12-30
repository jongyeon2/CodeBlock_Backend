package com.studyblock.domain.coupon.dto;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponAdminResponse {
    private Long id;
    private String name;
    private String description;
    private CouponType type;
    private Integer discountValue;
    private Integer minimumAmount;
    private Integer maximumDiscount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    private Integer usedCountFromCoupon;  // Coupon 테이블의 usedCount (쿠폰 사용 한도 관련)
    private Boolean isActive;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ✅ 집계 정보 (user_coupons 테이블에서 계산)
    private Long issuedCount;  // user_coupons에서 coupon_id = id인 레코드 수 (COUNT)
    private Long usedCount;  // user_coupons에서 coupon_id = id AND is_used = true인 레코드 수 (COUNT)
    // Note: Coupon 테이블의 usedCount는 쿠폰 사용 한도 관련 필드로, 여기서는 user_coupons 테이블의 실제 사용 횟수를 집계

    public static CouponAdminResponse from(Coupon coupon) {
        return CouponAdminResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .discountValue(coupon.getDiscountValue())
                .minimumAmount(coupon.getMinimumAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .usedCountFromCoupon(coupon.getUsedCount())
                .isActive(coupon.getIsActive())
                .createdById(coupon.getCreatedBy() != null ? coupon.getCreatedBy().getId() : null)
                .createdByName(coupon.getCreatedBy() != null ? coupon.getCreatedBy().getName() : null)
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .issuedCount(0L)  // 기본값, 컨트롤러에서 설정
                .usedCount(0L)  // 기본값, 컨트롤러에서 설정
                .build();
    }
    
    // 집계 정보 포함 생성 메서드
    public static CouponAdminResponse from(Coupon coupon, Long issuedCount, Long usedCountFromUserCoupons) {
        return CouponAdminResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .discountValue(coupon.getDiscountValue())
                .minimumAmount(coupon.getMinimumAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .usedCountFromCoupon(coupon.getUsedCount())
                .isActive(coupon.getIsActive())
                .createdById(coupon.getCreatedBy() != null ? coupon.getCreatedBy().getId() : null)
                .createdByName(coupon.getCreatedBy() != null ? coupon.getCreatedBy().getName() : null)
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .issuedCount(issuedCount != null ? issuedCount : 0L)
                .usedCount(usedCountFromUserCoupons != null ? usedCountFromUserCoupons : 0L)
                .build();
    }
}
