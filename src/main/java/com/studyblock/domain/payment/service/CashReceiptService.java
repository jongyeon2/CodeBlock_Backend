package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.entity.CashReceipt;
import com.studyblock.domain.payment.entity.Payment;
//import com.studyblock.domain.payment.enums.CashReceiptType;
//import com.studyblock.domain.payment.enums.CashReceiptType;
import com.studyblock.domain.payment.repository.CashReceiptRepository;
import com.studyblock.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashReceiptService {

    private final CashReceiptRepository cashReceiptRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public CashReceipt issue(Long paymentId, //CashReceiptType type,
                             String receiptNumber, LocalDateTime issuedAt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: id=" + paymentId));

        CashReceipt receipt = cashReceiptRepository.findByPayment_Id(paymentId)
                .orElse(CashReceipt.builder().payment(payment).build());

        // 발급 정보 설정
        return cashReceiptRepository.save(
                CashReceipt.builder()
                        .payment(payment)
                        .issued(true)
                        //.type(type)
                        //.type(type)
                        .receiptNumber(receiptNumber)
                        .issuedAt(issuedAt != null ? issuedAt : LocalDateTime.now())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public CashReceipt getByPaymentId(Long paymentId) {
        return cashReceiptRepository.findByPayment_Id(paymentId)
                .orElse(null);
    }
}


