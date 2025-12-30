package com.studyblock.domain.coupon.dto;

import lombok.Getter;
import lombok.Setter;

// 쿠폰 검증 요청 DTO
@Getter
@Setter
public class CouponValidationRequest {
    private String couponCode;
    private Integer totalAmount;
}
