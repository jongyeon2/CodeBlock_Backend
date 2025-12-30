package com.studyblock.domain.payment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMetadata extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "payment_source", length = 50)
    private String paymentSource;

    @Column(name = "order_name", length = 100)
    private String orderName;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "receipt_url", length = 255)
    private String receiptUrl;

    @Column(name = "toss_api_version", length = 20)
    private String tossApiVersion;

    @Builder
    public PaymentMetadata(Payment payment, String ipAddress, String userAgent,
                          String paymentSource, String orderName, String country,
                          String receiptUrl, String tossApiVersion) {
        this.payment = payment;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.paymentSource = paymentSource;
        this.orderName = orderName;
        this.country = country;
        this.receiptUrl = receiptUrl;
        this.tossApiVersion = tossApiVersion;
    }
}

