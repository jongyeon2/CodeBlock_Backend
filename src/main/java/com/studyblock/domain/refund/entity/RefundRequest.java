package com.studyblock.domain.refund.entity;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refund_route", nullable = false, length = 10)
    private String refundRoute;

    @Column(name = "reason", columnDefinition = "LONGTEXT")
    private String reason;

    @Column(name = "result", nullable = false, length = 20)
    private String result = "PENDING";

    @Column(name = "refund_amount_cash")
    private Integer refundAmountCash;

    @Column(name = "refund_amount_cookie")
    private Integer refundAmountCookie;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processor_admin_id")
    private Long processorAdminId;

    @Builder
    public RefundRequest(Order order, User user, String refundRoute, String reason,
                        Integer refundAmountCash, Integer refundAmountCookie) {
        this.order = order;
        this.user = user;
        this.refundRoute = refundRoute;
        this.reason = reason;
        this.refundAmountCash = refundAmountCash;
        this.refundAmountCookie = refundAmountCookie;
        this.result = "PENDING";
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(Long processorAdminId) {
        this.result = "APPROVED";
        this.processorAdminId = processorAdminId;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(Long processorAdminId, String reason) {
        this.result = "REJECTED";
        this.processorAdminId = processorAdminId;
        this.reason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return "PENDING".equals(this.result);
    }

    public boolean isApproved() {
        return "APPROVED".equals(this.result);
    }

    public boolean isRejected() {
        return "REJECTED".equals(this.result);
    }

    public boolean isCashRefund() {
        return "CASH".equals(this.refundRoute);
    }

    public boolean isCookieRefund() {
        return "COOKIE".equals(this.refundRoute);
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

    public boolean hasCashRefund() {
        return this.refundAmountCash != null && this.refundAmountCash > 0;
    }

    public boolean hasCookieRefund() {
        return this.refundAmountCookie != null && this.refundAmountCookie > 0;
    }

    public boolean isProcessed() {
        return this.processedAt != null;
    }

    public long getProcessingTimeHours() {
        if (this.processedAt == null) {
            return 0;
        }
        return java.time.Duration.between(this.requestedAt, this.processedAt).toHours();
    }

    public boolean isHighValueRefund() {
        return getTotalRefundAmount() >= 100000; // 10만원 이상
    }

    public boolean isLowValueRefund() {
        return getTotalRefundAmount() < 10000; // 1만원 미만
    }

    public String getRefundTier() {
        if (isHighValueRefund()) {
            return "HIGH";
        } else if (isLowValueRefund()) {
            return "LOW";
        } else {
            return "MEDIUM";
        }
    }
}


