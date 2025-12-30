package com.studyblock.domain.payment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "webhook_payload", columnDefinition = "JSON", nullable = false)
    private String webhookPayload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public PaymentWebhookEvent(Payment payment, String eventType, String webhookPayload, LocalDateTime receivedAt) {
        this.payment = payment;
        this.eventType = eventType;
        this.webhookPayload = webhookPayload;
        this.receivedAt = receivedAt != null ? receivedAt : LocalDateTime.now();
    }
}


