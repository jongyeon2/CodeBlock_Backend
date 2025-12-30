package com.studyblock.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethodDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 20)
    private MethodType methodType;

    @Column(name = "method_data", columnDefinition = "JSON", nullable = false)
    private String methodData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MethodType {
        CARD,
        VIRTUAL_ACCOUNT,
        TRANSFER,
        MOBILE,
        EASYPAY,
        GIFT_CERTIFICATE
    }

    @Builder
    public PaymentMethodDetails(Payment payment, MethodType methodType, String methodData) {
        this.payment = payment;
        this.methodType = methodType;
        this.methodData = methodData;
        this.createdAt = LocalDateTime.now();
    }
}

