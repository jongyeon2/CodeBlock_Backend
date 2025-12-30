package com.studyblock.domain.settlement.entity;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_items_id", nullable = true)
    private OrderItem orderItem;

    @Column(name = "net_amount", nullable = false)
    private Integer netAmount;

    @Column(name = "fee_amount", nullable = false)
    private Integer feeAmount;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "eligible_flag", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean eligibleFlag = false;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public SettlementLedger(User instructor,
                            Order order,
                            OrderItem orderItem,
                            Integer netAmount,
                            Integer feeAmount,
                            Double rate,
                            Boolean eligibleFlag,
                            LocalDateTime settledAt) {
        this.instructor = instructor;
        this.order = order;
        this.orderItem = orderItem;
        this.netAmount = netAmount;
        this.feeAmount = feeAmount;
        this.rate = rate;
        if (eligibleFlag != null) this.eligibleFlag = eligibleFlag;
        this.settledAt = settledAt;
    }

    // 비즈니스 메서드

    /**
     * 정산 가능 상태로 변경 (환불 기간 지남)
     */
    public void markEligible() {
        this.eligibleFlag = true;
    }

    /**
     * 정산 불가 상태로 변경 (환불 발생 시)
     */
    public void markIneligible() {
        this.eligibleFlag = false;
    }

    /**
     * 정산 완료 처리
     */
    public void settle() {
        if (!this.eligibleFlag) {
            throw new IllegalStateException("정산 가능 상태가 아닙니다");
        }
        if (this.settledAt != null) {
            throw new IllegalStateException("이미 정산 완료된 항목입니다");
        }
        this.settledAt = LocalDateTime.now();
    }

    /**
     * 정산 완료 여부
     */
    public boolean isSettled() {
        return this.settledAt != null;
    }

    /**
     * 정산 가능 여부
     */
    public boolean isEligible() {
        return this.eligibleFlag && this.settledAt == null;
    }

    /**
     * 총 금액 (수수료 포함)
     */
    public Integer getTotalAmount() {
        return this.netAmount + this.feeAmount;
    }
}


