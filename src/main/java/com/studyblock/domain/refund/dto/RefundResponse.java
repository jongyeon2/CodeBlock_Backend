package com.studyblock.domain.refund.dto;

import com.studyblock.domain.refund.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    
    private Long refundId;                // 환불 ID
    private Long orderId;                 // 주문 ID
    private String orderNumber;           // 주문 번호
    private RefundStatus status;          // 환불 상태
    private Integer amount;               // 총 환불 금액
    private Integer refundAmountCash;     // 현금 환불 금액
    private Integer refundAmountCookie;   // 쿠키 환불 금액
    private String refundRoute;           // 환불 경로 (CASH/COOKIE)
    private String reason;                // 환불 사유
    private LocalDateTime requestedAt;    // 요청 일시
    private LocalDateTime processedAt;    // 처리 일시
    
    // Refund 엔티티로부터 생성하는 정적 팩토리 메서드
    public static RefundResponse from(com.studyblock.domain.refund.entity.Refund refund) {
        return RefundResponse.builder()
                .refundId(refund.getId())
                .orderId(refund.getOrder().getId())
                .orderNumber(refund.getOrder().getOrderNumber())
                .status(refund.getStatus())
                .amount(refund.getAmount())
                .refundAmountCash(refund.getRefundAmountCash())
                .refundAmountCookie(refund.getRefundAmountCookie())
                .refundRoute(refund.getRefundRoute())
                .reason(refund.getReason())
                .requestedAt(refund.getRequestedAt())
                .processedAt(refund.getProcessedAt())
                .build();
    }
}

