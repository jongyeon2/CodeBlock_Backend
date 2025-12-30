package com.studyblock.domain.refund.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestFrontend {

    @JsonProperty("paymentId")
    private Long paymentId;          // 결제 ID (paymentId 또는 orderNumber 중 하나 필수)
    
    @JsonProperty(value = "orderNumber", required = false)
    private String orderNumber;      // 주문번호 (paymentId 또는 orderNumber 중 하나 필수)
    
    // 프론트엔드에서 다른 필드명으로 보낼 수 있으므로 별도 필드로 받기
    @JsonProperty(value = "order_number", required = false)
    private String orderNumberSnake;  // order_number (스네이크 케이스)
    
    @JsonProperty(value = "orderId", required = false)
    private String orderId;          // orderId (혼동 가능한 필드명)
    
    private Long courseId;           // 강의 ID (부분 환불 시 단일 강의용, 호환성 유지)
    private java.util.List<Long> courseIds;  // 강의 ID 목록 (부분 환불 시 여러 강의 선택 가능)
    private java.util.List<Long> refundItemIds;  // 부분 환불 시 선택된 강의 ID 배열
    private java.util.List<Long> orderItemIds;  // 주문 항목 ID 목록 (부분 환불 시 직접 지정 가능)
    private String reason;           // 환불 사유
    private String refundType;       // "FULL" | "PARTIAL"
    private Integer partialPercent;  // 부분 환불 비율(%) (단일 강의 환불 시 사용, 하위 호환성 유지)
    private String idempotencyKey;   // 멱등성 키(선택)
    
    // orderNumber를 가져오는 헬퍼 메서드 (여러 필드명 지원)
    public String getEffectiveOrderNumber() {
        if (orderNumber != null && !orderNumber.isBlank()) {
            return orderNumber;
        }
        if (orderNumberSnake != null && !orderNumberSnake.isBlank()) {
            return orderNumberSnake;
        }
        if (orderId != null && !orderId.isBlank()) {
            return orderId;
        }
        return null;
    }
}


