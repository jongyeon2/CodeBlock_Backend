package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//쿠키 충전 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieChargeResponse {
    private Long orderId;           // 주문 ID
    private Long paymentId;         // 결제 ID
    private Long bundleId;          // 쿠키 번들 ID
    private String bundleName;      // 쿠키 번들명
    private Integer cookieQuantity; // 충전된 쿠키 수량
    private Integer cashAmount;     // 결제한 현금 금액
    private Long newBalance;        // 충전 후 쿠키 잔액
    private LocalDateTime chargedAt; // 충전 시간
}
