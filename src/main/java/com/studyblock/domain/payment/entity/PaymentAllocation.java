package com.studyblock.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_allocations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payments_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_items_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public PaymentAllocation(Payment payment, OrderItem orderItem, Integer amount) {
        this.payment = payment;
        this.orderItem = orderItem;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isForOrderItem(Long orderItemId) {
        return this.orderItem.getId().equals(orderItemId);
    }
}
