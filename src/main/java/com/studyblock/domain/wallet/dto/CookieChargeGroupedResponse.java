package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieChargeGroupedResponse {
    private Long orderId;
    private String orderNumber;
    private Integer totalQty;   // paidQty + bonusQty
    private Integer paidQty;    // 유료 쿠키 합
    private Integer bonusQty;   // 보너스 쿠키 합
    private Long amount;        // 결제 금액(원)
    private LocalDateTime chargedAt; // 결제일
}


