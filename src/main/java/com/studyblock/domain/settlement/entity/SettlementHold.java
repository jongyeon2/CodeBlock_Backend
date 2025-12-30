package com.studyblock.domain.settlement.entity;

import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_hold")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_items_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "hold_until", nullable = false)
    private LocalDateTime holdUntil;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // HELD, RELEASED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder
    public SettlementHold(OrderItem orderItem, User user, LocalDateTime holdUntil, String status) {
        this.orderItem = orderItem;
        this.user = user;
        this.holdUntil = holdUntil;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public void release() {
        this.status = "RELEASED";
        this.releasedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.cancelledAt = LocalDateTime.now();
    }
}


