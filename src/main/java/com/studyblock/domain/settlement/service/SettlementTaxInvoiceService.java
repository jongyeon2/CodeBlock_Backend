package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.entity.SettlementTaxInvoice;
import com.studyblock.domain.settlement.repository.SettlementPaymentRepository;
import com.studyblock.domain.settlement.repository.SettlementTaxInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// 세금계산서 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementTaxInvoiceService {

    private final SettlementPaymentRepository settlementPaymentRepository;
    private final SettlementTaxInvoiceRepository settlementTaxInvoiceRepository;

    // 세금계산서 발행
    @Transactional
    public SettlementTaxInvoice generateTaxInvoice(Long paymentId,
                                                    LocalDate periodStart,
                                                    LocalDate periodEnd,
                                                    String invoiceFileUrl) {
        var payment = settlementPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("지급 레코드를 찾을 수 없습니다"));

        if (!payment.isCompleted()) {
            throw new IllegalStateException("지급 완료된 항목만 세금계산서를 발행할 수 있습니다");
        }

        // 이미 세금계산서가 발행되었는지 확인
        settlementTaxInvoiceRepository.findBySettlementPayment_Id(paymentId)
            .ifPresent(existing -> {
                throw new IllegalStateException("이미 세금계산서가 발행되었습니다");
            });

        // 세금계산서 번호 생성
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        Long count = settlementTaxInvoiceRepository.countByYearAndMonth(year, month);
        String invoiceNumber = SettlementTaxInvoice.generateInvoiceNumber(year, month, count.intValue() + 1);

        // 금액 계산 (부가세 10%)
        Integer netAmount = payment.getSettlementLedger().getNetAmount();
        Integer supplyAmount = Math.round(netAmount / 1.1f);
        Integer taxAmount = netAmount - supplyAmount;

        SettlementTaxInvoice taxInvoice = SettlementTaxInvoice.builder()
            .settlementPayment(payment)
            .invoiceNumber(invoiceNumber)
            .issueDate(today)
            .supplyAmount(supplyAmount)
            .taxAmount(taxAmount)
            .totalAmount(netAmount)
            .invoiceFileUrl(invoiceFileUrl)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .build();

        if (!taxInvoice.isAmountValid()) {
            throw new IllegalStateException("세금계산서 금액 계산 오류");
        }

        if (!taxInvoice.isPeriodValid()) {
            throw new IllegalArgumentException("정산 기간이 유효하지 않습니다");
        }

        settlementTaxInvoiceRepository.save(taxInvoice);

        log.info("세금계산서 발행 - paymentId: {}, invoiceNumber: {}, amount: {}",
                paymentId, invoiceNumber, netAmount);

        // DTO 변환을 위해 필요한 연관 관계를 포함하여 다시 조회
        return settlementTaxInvoiceRepository.findByIdWithRelations(taxInvoice.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 세금계산서 정보를 조회할 수 없습니다"));
    }

    // 세금계산서 파일 URL 업데이트
    @Transactional
    public SettlementTaxInvoice updateTaxInvoiceFile(Long taxInvoiceId, String localFilePath) {
        SettlementTaxInvoice taxInvoice = settlementTaxInvoiceRepository.findById(taxInvoiceId)
            .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다"));

        taxInvoice.updateInvoiceFileUrl(localFilePath);
        settlementTaxInvoiceRepository.save(taxInvoice);

        log.info("세금계산서 파일 업데이트 - taxInvoiceId: {}, filePath: {}", taxInvoiceId, localFilePath);

        // DTO 변환을 위해 필요한 연관 관계를 포함하여 다시 조회
        return settlementTaxInvoiceRepository.findByIdWithRelations(taxInvoice.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 세금계산서 정보를 조회할 수 없습니다"));
    }

    // 강사별 세금계산서 조회
    @Transactional(readOnly = true)
    public List<SettlementTaxInvoice> getTaxInvoices(Long instructorId) {
        return settlementTaxInvoiceRepository.findByInstructorId(instructorId);
    }

    // 세금계산서 번호로 조회
    @Transactional(readOnly = true)
    public SettlementTaxInvoice getTaxInvoiceByNumber(String invoiceNumber) {
        SettlementTaxInvoice taxInvoice = settlementTaxInvoiceRepository.findByInvoiceNumber(invoiceNumber)
            .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다"));
        
        // DTO 변환을 위해 필요한 연관 관계를 포함하여 다시 조회
        return settlementTaxInvoiceRepository.findByIdWithRelations(taxInvoice.getId())
                .orElseThrow(() -> new IllegalStateException("세금계산서 정보를 조회할 수 없습니다"));
    }
}

