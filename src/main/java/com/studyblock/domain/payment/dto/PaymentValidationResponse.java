package com.studyblock.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//결제 검증 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentValidationResponse {
    private boolean valid;
    private String message;
    private String redirectUrl;  // 쿠키 충전 시 리다이렉트 URL
    private String orderNumber;  // 사전 생성된 주문번호 (PENDING)
}