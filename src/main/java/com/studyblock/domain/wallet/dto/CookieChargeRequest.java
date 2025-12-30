package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//쿠키 충전 요청 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieChargeRequest {
    private Long bundleId;      // 쿠키 번들 ID
    private Integer quantity;   // 충전 수량 (일반적으로 1)
    private Long userCouponId;  // 선택 쿠폰(선택)

    // 토스 결제 정보 (토스 연동 시 필요)
    private String paymentKey;  // 토스 결제 키
    private String orderId;     // 주문 ID
    private Integer amount;     // 결제 금액
}
