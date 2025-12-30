package com.studyblock.domain.settlement.dto;

import com.studyblock.domain.settlement.entity.SettlementTaxInvoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementTaxInvoiceResponse {
    private Long id;
    private Long paymentId;
    private Long instructorId;
    private String instructorName;
    private String invoiceNumber;
    private LocalDate issueDate;
    private Integer supplyAmount;
    private Integer taxAmount;
    private Integer totalAmount;
    private String invoiceFileUrl;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SettlementTaxInvoiceResponse from(SettlementTaxInvoice taxInvoice) {
        return SettlementTaxInvoiceResponse.builder()
                .id(taxInvoice.getId())
                .paymentId(taxInvoice.getSettlementPayment().getId())
                .instructorId(taxInvoice.getSettlementPayment().getSettlementLedger().getInstructor().getId())
                .instructorName(taxInvoice.getSettlementPayment().getSettlementLedger().getInstructor().getName())
                .invoiceNumber(taxInvoice.getInvoiceNumber())
                .issueDate(taxInvoice.getIssueDate())
                .supplyAmount(taxInvoice.getSupplyAmount())
                .taxAmount(taxInvoice.getTaxAmount())
                .totalAmount(taxInvoice.getTotalAmount())
                .invoiceFileUrl(taxInvoice.getInvoiceFileUrl())
                .periodStart(taxInvoice.getPeriodStart())
                .periodEnd(taxInvoice.getPeriodEnd())
                .createdAt(taxInvoice.getCreatedAt())
                .updatedAt(taxInvoice.getUpdatedAt())
                .build();
    }
}
