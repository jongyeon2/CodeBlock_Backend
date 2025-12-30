package com.studyblock.domain.payment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.payment.enums.OrderStatus;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "cookie_spent")
    private Integer cookieSpent;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "order_number", nullable = false, length = 64)
    private String orderNumber;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "refundable_until")
    private LocalDateTime refundableUntil;

    @Column(name = "first_viewed_at")
    private LocalDateTime firstViewedAt;

    @Column(name = "idempotency_key", length = 36)
    private String idempotencyKey;

    @Column(name = "is_mixed_forbidden_violation", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isMixedForbiddenViolation = false;

    @Column(name = "policy_snapshot", columnDefinition = "JSON")
    private String policySnapshot;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "fail_reason", columnDefinition = "LONGTEXT")
    private String failReason;

    // 토스페이먼츠 연동 필드
    @Column(name = "toss_order_id", length = 64)
    private String tossOrderId;

    @Column(name = "order_name", length = 100)
    private String orderName;

    @Column(name = "customer_key", length = 100)
    private String customerKey;

    // 쿠폰 관련
    @Column(name = "total_discount_amount", nullable = false)
    private Integer totalDiscountAmount = 0;

    @Column(name = "coupon_count", nullable = false)
    private Integer couponCount = 0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(User user, PaymentType paymentType, Long totalAmount, Integer cookieSpent,
                 String orderNumber, String orderType, String idempotencyKey, String tossOrderId,
                 String orderName, String customerKey) {
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.currency = "KRW";
        this.paymentType = paymentType;
        this.totalAmount = totalAmount;
        this.cookieSpent = cookieSpent;
        this.orderNumber = orderNumber;
        this.orderType = orderType;
        this.idempotencyKey = idempotencyKey;
        this.tossOrderId = tossOrderId;
        this.orderName = orderName;
        this.customerKey = customerKey;
        this.isMixedForbiddenViolation = false;
        this.totalDiscountAmount = 0;
        this.couponCount = 0;
    }

    // Business methods
    public void markAsPaid() {
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void refund() {
        this.status = OrderStatus.REFUNDED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    public boolean canBeRefunded() {
        return this.status == OrderStatus.PAID;
    }

    public void setRefundableUntil(LocalDateTime refundableUntil) {
        this.refundableUntil = refundableUntil;
    }

    public void markFirstViewed() {
        if (this.firstViewedAt == null) {
            this.firstViewedAt = LocalDateTime.now();
        }
    }

    public void setPolicySnapshot(String policySnapshot) {
        this.policySnapshot = policySnapshot;
    }

    public void markAsCancelled(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.failReason = reason;
    }

    public void markAsFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.failReason = reason;
    }

    public void setTossOrderId(String tossOrderId) {
        this.tossOrderId = tossOrderId;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public void addDiscount(Integer discountAmount) {
        this.totalDiscountAmount += discountAmount;
        this.couponCount += 1;
    }

    public boolean isRefundable() {
        return this.refundableUntil != null && 
               LocalDateTime.now().isBefore(this.refundableUntil) &&
               this.status == OrderStatus.PAID;
    }

    public boolean hasMixedForbiddenViolation() {
        return this.isMixedForbiddenViolation;
    }

    public void markMixedForbiddenViolation() {
        this.isMixedForbiddenViolation = true;
    }

    // 할인 정보 업데이트 (여러 쿠폰 한번에 처리)
    public void updateDiscountInfo(Integer totalDiscountAmount, Integer couponCount) {
        this.totalDiscountAmount = totalDiscountAmount;
        this.couponCount = couponCount;
    }
}
