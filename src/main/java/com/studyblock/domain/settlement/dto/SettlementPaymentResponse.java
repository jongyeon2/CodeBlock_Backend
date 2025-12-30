package com.studyblock.domain.settlement.dto;

import com.studyblock.domain.settlement.entity.SettlementPayment;
import com.studyblock.domain.settlement.enums.PaymentMethod;
import com.studyblock.domain.settlement.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementPaymentResponse {
    private Long id;
    private Long ledgerId;
    private Long instructorId;
    private String instructorName;
    private Integer netAmount;
    private LocalDateTime paymentDate;
    private PaymentMethod paymentMethod;
    private String bankAccountInfo;
    private PaymentStatus status;
    private String confirmationNumber;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SettlementPaymentResponse from(SettlementPayment payment) {
        return SettlementPaymentResponse.builder()
                .id(payment.getId())
                .ledgerId(payment.getSettlementLedger().getId())
                .instructorId(payment.getSettlementLedger().getInstructor().getId())
                .instructorName(payment.getSettlementLedger().getInstructor().getName())
                .netAmount(payment.getSettlementLedger().getNetAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .bankAccountInfo(payment.getBankAccountInfo())
                .status(payment.getStatus())
                .confirmationNumber(payment.getConfirmationNumber())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
