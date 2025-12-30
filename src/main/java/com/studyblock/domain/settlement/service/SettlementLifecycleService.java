package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.entity.SettlementLedger;
import com.studyblock.domain.settlement.repository.SettlementHoldRepository;
import com.studyblock.domain.settlement.repository.SettlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 생명주기 관리 서비스
 * 환불 처리, 정산 가능 상태 변경, 정산 실행을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementLifecycleService {

    private final SettlementLedgerRepository settlementLedgerRepository;
    private final SettlementHoldRepository settlementHoldRepository;

    private static final int REFUND_HOLD_DAYS = 7; // 환불 보류 기간 7일

    // ========================================
    // 환불 처리
    // ========================================

    /**
     * 환불 시 정산 레코드를 정산 불가로 변경
     * RefundService에서 환불 완료 시 호출됨
     * 
     * @param orderId 환불된 주문 ID
     * @throws IllegalStateException 이미 정산 완료된 주문인 경우
     */
    @Transactional
    public void markSettlementIneligible(Long orderId) {
        List<SettlementLedger> ledgers = settlementLedgerRepository.findAllByOrderItem_Id(orderId);
        
        if (ledgers.isEmpty()) {
            log.warn("정산 레코드를 찾을 수 없습니다 - orderId: {}", orderId);
            return;
        }
        
        // 정산 완료 여부 확인
        validateNotSettled(ledgers, orderId);
        
        // 정산 불가 처리
        markLedgersIneligible(ledgers, orderId);
        
        // 보류 취소 처리
        cancelSettlementHolds(ledgers);
    }

    /**
     * 정산 완료 여부 검증
     */
    private void validateNotSettled(List<SettlementLedger> ledgers, Long orderId) {
        for (SettlementLedger ledger : ledgers) {
            if (ledger.isSettled()) {
                throw new IllegalStateException(
                        "이미 정산 완료된 주문은 환불할 수 없습니다. orderId: " + orderId + 
                        ", ledgerId: " + ledger.getId());
            }
        }
    }

    /**
     * 정산 레코드들을 정산 불가로 변경
     */
    private void markLedgersIneligible(List<SettlementLedger> ledgers, Long orderId) {
        for (SettlementLedger ledger : ledgers) {
            ledger.markIneligible();
            settlementLedgerRepository.save(ledger);
            log.info("정산 레코드 정산 불가 처리 - orderId: {}, ledgerId: {}", orderId, ledger.getId());
        }
    }

    /**
     * 정산 보류 취소 처리
     */
    private void cancelSettlementHolds(List<SettlementLedger> ledgers) {
        for (SettlementLedger ledger : ledgers) {
            settlementHoldRepository.findByOrderItem_Id(ledger.getOrderItem().getId())
                .ifPresent(hold -> {
                    if ("HELD".equals(hold.getStatus())) {
                        hold.cancel();
                        settlementHoldRepository.save(hold);
                        log.info("정산 보류 취소 - orderItemId: {}, holdId: {}", 
                                ledger.getOrderItem().getId(), hold.getId());
                    }
                });
        }
    }

    // ========================================
    // 정산 가능 상태 변경
    // ========================================

    /**
     * 환불 기간(7일) 지난 항목을 정산 가능으로 변경
     * 스케줄러 또는 수동으로 실행
     * 
     * @return 정산 가능으로 변경된 개수
     */
    @Transactional
    public int markEligibleForSettlement() {
        LocalDateTime refundDeadline = LocalDateTime.now().minusDays(REFUND_HOLD_DAYS);

        // 정산 가능 대상 조회
        List<SettlementLedger> eligibleLedgers = findEligibleLedgers(refundDeadline);

        // 정산 가능 상태로 변경
        int processedCount = processEligibleLedgers(eligibleLedgers);

        log.info("정산 가능 상태로 변경 완료 - 총 {}건", processedCount);
        return processedCount;
    }

    /**
     * 정산 가능 대상 조회 (환불 기간 경과, 미정산 항목)
     */
    private List<SettlementLedger> findEligibleLedgers(LocalDateTime refundDeadline) {
        return settlementLedgerRepository.findAll().stream()
            .filter(ledger -> !ledger.getEligibleFlag()
                        && ledger.getCreatedAt().isBefore(refundDeadline)
                        && ledger.getSettledAt() == null)
            .toList();
    }

    /**
     * 정산 가능 상태로 변경 및 보류 해제
     */
    private int processEligibleLedgers(List<SettlementLedger> ledgers) {
        for (SettlementLedger ledger : ledgers) {
            // 정산 가능 상태로 변경
            ledger.markEligible();
            settlementLedgerRepository.save(ledger);

            log.info("정산 가능 상태로 변경 - ledgerId: {}, instructorId: {}, netAmount: {}", 
                    ledger.getId(), ledger.getInstructor().getId(), ledger.getNetAmount());
            
            // 정산 보류 해제
            releaseSettlementHold(ledger);
        }
        
        return ledgers.size();
    }

    /**
     * 정산 보류 해제
     */
    private void releaseSettlementHold(SettlementLedger ledger) {
        settlementHoldRepository.findByOrderItem_Id(ledger.getOrderItem().getId())
            .ifPresent(hold -> {
                if ("HELD".equals(hold.getStatus())) {
                    hold.release();
                    settlementHoldRepository.save(hold);
                    log.info("정산 보류 해제 - orderItemId: {}, holdId: {}", 
                            ledger.getOrderItem().getId(), hold.getId());
                }
            });
    }

    // ========================================
    // 정산 실행
    // ========================================

    /**
     * 특정 강사의 정산 실행
     * 관리자가 수동으로 실행
     * 
     * @param instructorId 강사 ID
     * @return 정산 완료 건수
     */
    @Transactional
    public int settleForInstructor(Long instructorId) {
        List<SettlementLedger> eligibleLedgers = 
                settlementLedgerRepository.findEligibleByInstructorId(instructorId);

        if (eligibleLedgers.isEmpty()) {
            log.warn("정산 가능한 항목이 없습니다 - instructorId: {}", instructorId);
            return 0;
        }

        int totalNetAmount = settleLedgers(eligibleLedgers);

        log.info("강사 정산 완료 - instructorId: {}, 건수: {}, 총액: {}",
                instructorId, eligibleLedgers.size(), totalNetAmount);

        return eligibleLedgers.size();
    }

    /**
     * 전체 정산 실행
     * 모든 정산 가능한 항목을 한 번에 정산
     * 
     * @return 정산 완료 건수
     */
    @Transactional
    public int settleAll() {
        List<SettlementLedger> eligibleLedgers = 
                settlementLedgerRepository.findEligibleForSettlement();

        if (eligibleLedgers.isEmpty()) {
            log.warn("정산 가능한 항목이 없습니다");
            return 0;
        }

        int totalNetAmount = settleLedgers(eligibleLedgers);

        log.info("전체 정산 완료 - 건수: {}, 총액: {}", eligibleLedgers.size(), totalNetAmount);

        return eligibleLedgers.size();
    }

    /**
     * 정산 레코드 목록을 정산 완료 처리
     * 
     * @return 총 정산 금액
     */
    private int settleLedgers(List<SettlementLedger> ledgers) {
        int totalNetAmount = 0;
        
        for (SettlementLedger ledger : ledgers) {
            ledger.settle();
            settlementLedgerRepository.save(ledger);
            totalNetAmount += ledger.getNetAmount();
        }
        
        return totalNetAmount;
    }
}

