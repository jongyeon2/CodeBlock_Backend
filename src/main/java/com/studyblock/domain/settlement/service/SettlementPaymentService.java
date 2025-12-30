package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.entity.SettlementPayment;
import com.studyblock.domain.settlement.enums.PaymentMethod;
import com.studyblock.domain.settlement.enums.PaymentStatus;
import com.studyblock.domain.settlement.repository.SettlementLedgerRepository;
import com.studyblock.domain.settlement.repository.SettlementPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 정산 지급 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementPaymentService {

    private final SettlementLedgerRepository settlementLedgerRepository;
    private final SettlementPaymentRepository settlementPaymentRepository;

    // 정산 지급 실행
    @Transactional
    public SettlementPayment executePayment(Long ledgerId,
                                            PaymentMethod paymentMethod,
                                            String bankAccountInfo,
                                            String notes,
                                            String confirmationNumber) {
        var ledger = settlementLedgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("정산 레코드를 찾을 수 없습니다"));

        if (!ledger.isSettled()) {
            throw new IllegalStateException("정산 완료되지 않은 항목입니다");
        }

        // 이미 지급된 항목인지 확인
        List<SettlementPayment> existingPayments = settlementPaymentRepository.findBySettlementLedger_Id(ledgerId);
        boolean alreadyCompleted = existingPayments.stream()
            .anyMatch(SettlementPayment::isCompleted);

        if (alreadyCompleted) {
            throw new IllegalStateException("이미 지급 완료된 항목입니다");
        }

        // 지급 레코드 생성 (초기 상태는 PENDING)
        SettlementPayment payment = SettlementPayment.builder()
            .settlementLedger(ledger)
            .paymentDate(LocalDateTime.now())
            .paymentMethod(paymentMethod)
            .bankAccountInfo(bankAccountInfo)
            .status(PaymentStatus.PENDING)
            .notes(notes)
            .build();

        settlementPaymentRepository.save(payment);

        // 지급 실행 시 자동으로 완료 처리 (확인번호는 선택사항)
        // 확인번호가 없으면 자동 생성됨
        payment.complete(confirmationNumber);
        settlementPaymentRepository.save(payment);

        log.info("정산 지급 생성 및 자동 완료 - ledgerId: {}, instructorId: {}, amount: {}, method: {}, confirmationNumber: {}",
                ledgerId, ledger.getInstructor().getId(), ledger.getNetAmount(), paymentMethod, payment.getConfirmationNumber());

        // DTO 변환을 위해 필요한 연관 관계를 포함하여 다시 조회
        return settlementPaymentRepository.findByIdWithRelations(payment.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 지급 정보를 조회할 수 없습니다"));
    }

    // 지급 완료 처리
    @Transactional
    public SettlementPayment completePayment(Long paymentId, String confirmationNumber) {
        SettlementPayment payment = settlementPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("지급 레코드를 찾을 수 없습니다"));

        payment.complete(confirmationNumber);
        settlementPaymentRepository.save(payment);

        log.info("정산 지급 완료 - paymentId: {}, confirmationNumber: {}", paymentId, confirmationNumber);

        // DTO 변환을 위해 필요한 연관 관계를 포함하여 다시 조회
        return settlementPaymentRepository.findByIdWithRelations(payment.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 지급 정보를 조회할 수 없습니다"));
    }

    // 지급 실패 처리
    @Transactional
    public SettlementPayment failPayment(Long paymentId, String reason) {
        SettlementPayment payment = settlementPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("지급 레코드를 찾을 수 없습니다"));

        payment.fail(reason);
        settlementPaymentRepository.save(payment);

        log.warn("정산 지급 실패 - paymentId: {}, reason: {}", paymentId, reason);

        // DTO 변환을 위해 필요한 연관 관계를 포함하여 다시 조회
        return settlementPaymentRepository.findByIdWithRelations(payment.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 지급 정보를 조회할 수 없습니다"));
    }

    // 강사별 지급 내역 조회
    @Transactional(readOnly = true)
    public List<SettlementPayment> getPaymentHistory(Long instructorId) {
        return settlementPaymentRepository.findByInstructorId(instructorId);
    }

    // 강사별 지급 내역 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<SettlementPayment> getPaymentHistory(Long instructorId, Pageable pageable) {
        Page<SettlementPayment> payments = settlementPaymentRepository.findByInstructorId(instructorId, pageable);
        
        // 페이징과 JOIN FETCH를 함께 사용할 수 없으므로, 트랜잭션 내에서 연관관계를 강제 로드
        payments.getContent().forEach(payment -> {
            // SettlementLedger와 Instructor 정보를 강제로 로드
            payment.getSettlementLedger().getId();
            payment.getSettlementLedger().getInstructor().getId();
            payment.getSettlementLedger().getInstructor().getName();
            payment.getSettlementLedger().getNetAmount();
        });
        
        return payments;
    }

    // 지급 대기 건수 조회
    @Transactional(readOnly = true)
    public Long getPendingPaymentCount() {
        return settlementPaymentRepository.countPending();
    }
}

