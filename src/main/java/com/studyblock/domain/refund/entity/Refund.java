package com.studyblock.domain.refund.entity;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.refund.enums.RefundStatus;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payments_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "refund_key")
    private String refundKey;

    @Column(name = "idempotency_key", length = 36)
    private String idempotencyKey;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "refund_route", nullable = false, length = 10)
    private String refundRoute = "CASH";

    @Column(name = "refund_amount_cash")
    private Integer refundAmountCash;

    @Column(name = "refund_amount_cookie")
    private Integer refundAmountCookie;

    @Column(name = "processor_admin_id")
    private Long processorAdminId;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    // V16: 환불 추가 정보
    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    @Column(name = "refund_bank", length = 50)
    private String refundBank;

    @Column(name = "refund_account_number", length = 50)
    private String refundAccountNumber;

    @Column(name = "refund_holder_name", length = 100)
    private String refundHolderName;

    // 토스페이먼츠 환불 응답 원본 JSON (디버깅/감사용)
    @Column(name = "toss_refund_response", columnDefinition = "JSON")
    private String tossRefundResponse;

    @Builder
    public Refund(Order order, User user, Payment payment, Integer amount,
                  String refundKey, String idempotencyKey, String reason, String refundRoute,
                  Integer refundAmountCash, Integer refundAmountCookie,
                  String refundReason, String refundMethod, String refundBank,
                  String refundAccountNumber, String refundHolderName, String tossRefundResponse) {
        this.order = order;
        this.user = user;
        this.payment = payment;
        this.amount = amount;
        this.refundKey = refundKey;
        this.idempotencyKey = idempotencyKey;
        this.reason = reason;
        this.refundRoute = refundRoute != null ? refundRoute : "CASH";
        this.refundAmountCash = refundAmountCash;
        this.refundAmountCookie = refundAmountCookie;
        this.refundReason = refundReason;
        this.refundMethod = refundMethod;
        this.refundBank = refundBank;
        this.refundAccountNumber = refundAccountNumber;
        this.refundHolderName = refundHolderName;
        this.tossRefundResponse = tossRefundResponse;
        this.status = RefundStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // 토스 환불 응답 JSON 저장 (원본 데이터 보존)
    public void setTossRefundResponse(String tossRefundResponse) {
        this.tossRefundResponse = tossRefundResponse;
    }

    // refundKey 설정 (토스 transactionKey용)
    public void setRefundKey(String refundKey) {
        this.refundKey = refundKey;
    }

    // processedAt 설정 (토스 canceledAt용)
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    // Business methods
    public void approve(Long processorAdminId) {
        this.status = RefundStatus.APPROVED;
        this.processorAdminId = processorAdminId;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(Long processorAdminId, String reason) {
        this.status = RefundStatus.REJECTED;
        this.processorAdminId = processorAdminId;
        this.reason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void process(Long processorAdminId) {
        this.status = RefundStatus.PROCESSED;
        this.processorAdminId = processorAdminId;
        this.processedAt = LocalDateTime.now();
    }

    public boolean isCashRefund() {
        return "CASH".equals(this.refundRoute);
    }

    public boolean isCookieRefund() {
        return "COOKIE".equals(this.refundRoute);
    }

    public boolean isPending() {
        return this.status == RefundStatus.PENDING;
    }

    public boolean isApproved() {
        return this.status == RefundStatus.APPROVED;
    }

    public boolean isProcessed() {
        return this.status == RefundStatus.PROCESSED;
    }

    public boolean isRejected() {
        return this.status == RefundStatus.REJECTED;
    }

    public Integer getTotalRefundAmount() {
        int total = 0;
        if (this.refundAmountCash != null) {
            total += this.refundAmountCash;
        }
        if (this.refundAmountCookie != null) {
            total += this.refundAmountCookie;
        }
        return total;
    }

    // V16 추가 비즈니스 메서드들
    public void setRefundBankInfo(String refundBank, String refundAccountNumber, String refundHolderName) {
        this.refundBank = refundBank;
        this.refundAccountNumber = refundAccountNumber;
        this.refundHolderName = refundHolderName;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public boolean hasRefundBankInfo() {
        return this.refundBank != null && this.refundAccountNumber != null && this.refundHolderName != null;
    }

    public boolean isBankRefund() {
        return "BANK".equals(this.refundMethod);
    }

    public boolean isCardRefund() {
        return "CARD".equals(this.refundMethod);
    }

    public boolean isVirtualAccountRefund() {
        return "VIRTUAL_ACCOUNT".equals(this.refundMethod);
    }

    public boolean requiresBankInfo() {
        return isBankRefund() || isVirtualAccountRefund();
    }

    public void validateRefundInfo() {
        if (requiresBankInfo() && !hasRefundBankInfo()) {
            throw new IllegalStateException("환불 계좌 정보가 필요합니다.");
        }
    }
}
