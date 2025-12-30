package com.studyblock.domain.settlement.dto;

import com.studyblock.domain.settlement.entity.SettlementHold;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementHoldResponse {
    private Long id;
    private Long orderItemId;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userName;
    private Long instructorId; // optional, resolved via ledger
    private String instructorName; // optional
    private LocalDateTime holdUntil;
    private String status; // HELD, RELEASED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
    private LocalDateTime cancelledAt;

    // 금액 표시용 (보류 중 정산 순수익 합계)
    private Integer holdNetAmount; // sum of ledger.netAmount for this order in HOLD state

    public static SettlementHoldResponse from(SettlementHold hold) {
        // Basic mapping (order/user lazy fields should be initialized in service)
        return SettlementHoldResponse.builder()
                .id(hold.getId())
                .orderItemId(hold.getOrderItem() != null ? hold.getOrderItem().getId() : null)
                .orderId(hold.getOrderItem() != null ? hold.getOrderItem().getOrder().getId() : null)
                .orderNumber(hold.getOrderItem() != null ? hold.getOrderItem().getOrder().getOrderNumber() : null)
                .userId(hold.getUser() != null ? hold.getUser().getId() : null)
                .userName(hold.getUser() != null ? hold.getUser().getName() : null)
                .holdUntil(hold.getHoldUntil())
                .status(hold.getStatus())
                .createdAt(hold.getCreatedAt())
                .releasedAt(hold.getReleasedAt())
                .cancelledAt(hold.getCancelledAt())
                .build();
    }

    public SettlementHoldResponse withInstructor(Long instructorId, String instructorName) {
        return SettlementHoldResponse.builder()
                .id(this.id)
                .orderItemId(this.orderItemId)
                .orderId(this.orderId)
                .orderNumber(this.orderNumber)
                .userId(this.userId)
                .userName(this.userName)
                .instructorId(instructorId)
                .instructorName(instructorName)
                .holdUntil(this.holdUntil)
                .status(this.status)
                .createdAt(this.createdAt)
                .releasedAt(this.releasedAt)
                .cancelledAt(this.cancelledAt)
                .holdNetAmount(this.holdNetAmount)
                .build();
    }

    public SettlementHoldResponse withHoldNetAmount(Integer holdNetAmount) {
        return SettlementHoldResponse.builder()
                .id(this.id)
                .orderItemId(this.orderItemId)
                .orderId(this.orderId)
                .orderNumber(this.orderNumber)
                .userId(this.userId)
                .userName(this.userName)
                .instructorId(this.instructorId)
                .instructorName(this.instructorName)
                .holdUntil(this.holdUntil)
                .status(this.status)
                .createdAt(this.createdAt)
                .releasedAt(this.releasedAt)
                .cancelledAt(this.cancelledAt)
                .holdNetAmount(holdNetAmount)
                .build();
    }
}
