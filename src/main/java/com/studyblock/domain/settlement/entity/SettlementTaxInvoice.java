package com.studyblock.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_tax_invoice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementTaxInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_payment_id", nullable = false)
    private SettlementPayment settlementPayment;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "supply_amount", nullable = false)
    private Integer supplyAmount;

    @Column(name = "tax_amount", nullable = false)
    private Integer taxAmount;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "invoice_file_url", length = 500)
    private String invoiceFileUrl;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public SettlementTaxInvoice(SettlementPayment settlementPayment,
                                String invoiceNumber,
                                LocalDate issueDate,
                                Integer supplyAmount,
                                Integer taxAmount,
                                Integer totalAmount,
                                String invoiceFileUrl,
                                LocalDate periodStart,
                                LocalDate periodEnd) {
        this.settlementPayment = settlementPayment;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.supplyAmount = supplyAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.invoiceFileUrl = invoiceFileUrl;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // 비즈니스 메서드

    /**
     * 세금계산서 파일 URL 업데이트 (로컬 파일시스템)
     * @param localFilePath 로컬 파일 경로 (예: /uploads/tax-invoices/2024/01/invoice-123.pdf)
     */
    public void updateInvoiceFileUrl(String localFilePath) {
        this.invoiceFileUrl = localFilePath;
        this.updatedAt = LocalDateTime.now();
    }

    // /**
    //  * 세금계산서 파일 URL 업데이트 (S3 스토리지)
    //  * @param s3Url S3 URL (예: https://bucket.s3.region.amazonaws.com/tax-invoices/invoice-123.pdf)
    //  */
    // public void updateInvoiceFileUrlFromS3(String s3Url) {
    //     this.invoiceFileUrl = s3Url;
    //     this.updatedAt = LocalDateTime.now();
    // }

    /**
     * 세금계산서 금액 검증
     * @return 총액 = 공급가액 + 세액 여부
     */
    public boolean isAmountValid() {
        return this.totalAmount.equals(this.supplyAmount + this.taxAmount);
    }

    /**
     * 세금계산서 기간 검증
     * @return 시작일 <= 종료일 여부
     */
    public boolean isPeriodValid() {
        return !this.periodStart.isAfter(this.periodEnd);
    }

    /**
     * 세금계산서 번호 생성 헬퍼
     * @param year 연도
     * @param month 월
     * @param sequence 일련번호
     * @return 세금계산서 번호 (형식: INV-YYYY-MM-NNNNN)
     */
    public static String generateInvoiceNumber(int year, int month, int sequence) {
        return String.format("INV-%04d-%02d-%05d", year, month, sequence);
    }
}
