package com.studyblock.domain.coupon.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {

    private Long id;                   // 쿠폰 ID
    private String name;               // 쿠폰 이름
    private String description;        // 쿠폰 설명
    private String type;               // 쿠폰 종류 (e.g. PERCENT, FIXED)
    private Integer discountValue;     // 할인 값 (퍼센트 또는 금액)
    private Integer minimumAmount;     // 최소 사용 금액
    private Integer maximumDiscount;   // 최대 할인 금액
    private LocalDateTime validFrom;   // 유효 시작일
    private LocalDateTime validUntil;  // 유효 종료일
    private Integer usageLimit;        // 전체 사용 가능 횟수
    private Integer usedCount;         // 현재까지 사용된 횟수
    private Boolean isActive;          // 활성 여부
    private Long createdBy;          // 생성자 (관리자 ID)
    private LocalDateTime createdAt;   // 생성 시각
    private LocalDateTime updatedAt;   // 수정 시각

    // Entity → DTO 변환용 정적 메서드
    public static CouponResponse from(com.studyblock.domain.coupon.entity.Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType().name()) // enum
                .discountValue(coupon.getDiscountValue())
                .minimumAmount(coupon.getMinimumAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .isActive(coupon.getIsActive())
//                .createdBy(coupon.getCreatedBy().getId())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
