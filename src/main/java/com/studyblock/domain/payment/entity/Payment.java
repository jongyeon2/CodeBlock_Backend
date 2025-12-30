package com.studyblock.domain.payment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.payment.enums.PaymentMethod;
import com.studyblock.domain.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(nullable = false, length = 20)
    private String provider = "toss";

    @Column(nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "KRW";

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.INIT;

    @Column(name = "payment_key", nullable = false)
    private String paymentKey;

    @Column(name = "merchant_uid", nullable = false)
    private String merchantUid;

    @Column(name = "idempotency_key", nullable = false, columnDefinition = "CHAR(36)")
    private String idempotencyKey;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "failure_reason")
    private String failureReason;

    // V27 이후: 메타/웹훅/보안/현금영수증 필드 제거

    @Column(name = "toss_response", columnDefinition = "JSON")
    private String tossResponse;

    // (removed fields per V27)

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAllocation> allocations = new ArrayList<>();

    @Builder
    public Payment(Order order, PaymentMethod method, String provider, Long amount,
                   String paymentKey, String merchantUid, String idempotencyKey) {
        this.order = order;
        this.method = method;
        this.provider = provider != null ? provider : "toss";
        this.currency = "KRW";
        this.amount = amount;
        this.status = PaymentStatus.INIT;
        this.paymentKey = paymentKey;
        this.merchantUid = merchantUid;
        this.idempotencyKey = idempotencyKey;
        // removed fields are no longer initialized
    }

    // Business methods
    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
    }

    public void capture() {
        this.status = PaymentStatus.CAPTURED;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.CAPTURED;
    }

    public void addAllocation(PaymentAllocation allocation) {
        this.allocations.add(allocation);
    }

//    public void setWebhookReceivedAt(LocalDateTime webhookReceivedAt) {
//        this.webhookReceivedAt = webhookReceivedAt;
//    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void setTossResponse(String tossResponse) {
        this.tossResponse = tossResponse;
    }


    public Long getRefundableAmount() {
        return this.amount;
    }

    /**
     * 카드 결제 여부 확인
     */
    public boolean isCardPayment() {
        return this.method == PaymentMethod.CARD;
    }

    /**
     * 가상계좌 결제 여부 확인
     */
    public boolean isVirtualAccountPayment() {
        return this.method == PaymentMethod.VIRTUAL_ACCOUNT;
    }

    /**
     * 계좌이체 결제 여부 확인
     */
    public boolean isTransferPayment() {
        return this.method == PaymentMethod.TRANSFER;
    }

    /**
     * 간편결제 여부 확인
     */
    public boolean isEasyPayPayment() {
        return this.method == PaymentMethod.EASY_PAY;
    }

    /**
     * 휴대폰 결제 여부 확인
     */
    public boolean isMobilePhonePayment() {
        return this.method == PaymentMethod.MOBILE;
    }

    // retry/cash receipt helpers removed per V27
}
