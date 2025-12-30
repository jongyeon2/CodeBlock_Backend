package com.studyblock.domain.payment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
//import com.studyblock.domain.payment.enums.CashReceiptType;
//import com.studyblock.domain.payment.enums.CashReceiptType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cash_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashReceipt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "issued", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean issued = false;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "type", length = 32)
//    private CashReceiptType type; // INCOME_DEDUCTION / PROOF_OF_EXPENSE

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Builder
    public CashReceipt(Payment payment, Boolean issued, //CashReceiptType type,
                    String receiptNumber, LocalDateTime issuedAt) {
        this.payment = payment;
        this.issued = issued != null ? issued : false;
        //this.type = type;
        //this.type = type;
        this.receiptNumber = receiptNumber;
        this.issuedAt = issuedAt;
    }
}


