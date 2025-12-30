package com.studyblock.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailureLogRequest {
    
    private String orderId;         // 주문 번호 (필수)
    private String paymentKey;      // 결제 키 (선택)
    private String errorCode;       // 에러 코드 (SDK 또는 HTTP 코드)
    private String message;         // 에러 메시지
    private String reason;          // 실패 사유 (cancel, network, timeout 등)
    private String userAgent;       // 사용자 브라우저 정보
}

