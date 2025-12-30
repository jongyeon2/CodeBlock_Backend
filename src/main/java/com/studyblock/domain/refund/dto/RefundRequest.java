package com.studyblock.domain.refund.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    private Long orderId;         // 환불할 주문 ID
    private String reason;        // 환불 사유
    private String idempotencyKey; // 멱등성 키 (중복 요청 방지용)
    // 부분환불 지원: 항목 기반 또는 금액 기반 중 하나 사용
    private java.util.List<Long> orderItemIds; // 부분환불 대상 order_item ID 목록(선택)
    private Integer partialAmount;             // 부분환불 총 금액(선택, KRW)
}

