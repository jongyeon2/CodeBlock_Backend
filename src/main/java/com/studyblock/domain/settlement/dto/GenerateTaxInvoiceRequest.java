package com.studyblock.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTaxInvoiceRequest {
    private Long paymentId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String invoiceFileUrl;
}
