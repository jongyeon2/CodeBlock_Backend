package com.studyblock.domain.webhook.entity;

import com.studyblock.domain.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "JSON")
    private String eventData;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public WebhookEvent(Payment payment, String eventType, String eventData) {
        this.payment = payment;
        this.eventType = eventType;
        this.eventData = eventData;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void markAsProcessed() {
        this.processedAt = LocalDateTime.now();
    }

    public boolean isProcessed() {
        return this.processedAt != null;
    }

    public boolean isPending() {
        return this.processedAt == null;
    }

    public boolean isPaymentApproved() {
        return "PAYMENT_APPROVED".equals(this.eventType);
    }

    public boolean isPaymentCancelled() {
        return "PAYMENT_CANCELLED".equals(this.eventType);
    }

    public boolean isPaymentFailed() {
        return "PAYMENT_FAILED".equals(this.eventType);
    }

    public boolean isRefundApproved() {
        return "REFUND_APPROVED".equals(this.eventType);
    }

    public boolean isRefundCancelled() {
        return "REFUND_CANCELLED".equals(this.eventType);
    }

    public boolean isRefundFailed() {
        return "REFUND_FAILED".equals(this.eventType);
    }

    public boolean isPaymentEvent() {
        return this.eventType != null && this.eventType.startsWith("PAYMENT_");
    }

    public boolean isRefundEvent() {
        return this.eventType != null && this.eventType.startsWith("REFUND_");
    }

    public boolean isApprovedEvent() {
        return this.eventType != null && this.eventType.endsWith("_APPROVED");
    }

    public boolean isCancelledEvent() {
        return this.eventType != null && this.eventType.endsWith("_CANCELLED");
    }

    public boolean isFailedEvent() {
        return this.eventType != null && this.eventType.endsWith("_FAILED");
    }

    public long getProcessingDelaySeconds() {
        if (this.processedAt == null) {
            return 0;
        }
        return java.time.Duration.between(this.createdAt, this.processedAt).getSeconds();
    }
}