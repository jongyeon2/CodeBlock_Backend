package com.studyblock.domain.refund.entity;

import com.studyblock.domain.payment.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_allocations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refunds_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_items_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public RefundAllocation(Refund refund, OrderItem orderItem, Integer amount) {
        this.refund = refund;
        this.orderItem = orderItem;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isForOrderItem(Long orderItemId) {
        return this.orderItem.getId().equals(orderItemId);
    }
}
