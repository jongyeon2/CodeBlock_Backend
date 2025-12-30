package com.studyblock.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmResponse {
    
    private boolean success;            // 결제 성공 여부
    private String message;             // 응답 메시지
    
    // 결제 정보
    private String orderId;             // 주문 번호
    private String paymentKey;          // 결제 키
    private Integer amount;             // 결제 금액
    private String paymentMethod;       // 결제 수단 (카드, 계좌이체 등)
    
    // 주문 정보
    private Long orderDatabaseId;       // DB에 저장된 Order ID
    private LocalDateTime paidAt;       // 결제 완료 시간
    
    // 쿠폰/할인 정보
    private Integer discountAmount;     // 할인 금액
    private Integer cookieAmount;       // 쿠키 사용 금액
    
    // 성공 응답
    public static PaymentConfirmResponse success(String orderId, String paymentKey, Integer amount) {
        return PaymentConfirmResponse.builder()
                .success(true)
                .message("결제가 완료되었습니다")
                .orderId(orderId)
                .paymentKey(paymentKey)
                .amount(amount)
                .paidAt(LocalDateTime.now())
                .build();
    }
    
    // 실패 응답
    public static PaymentConfirmResponse fail(String message) {
        return PaymentConfirmResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}

