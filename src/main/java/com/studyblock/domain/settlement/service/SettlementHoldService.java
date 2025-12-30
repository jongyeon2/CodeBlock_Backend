package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.dto.SettlementHoldResponse;
import com.studyblock.domain.settlement.entity.SettlementHold;
import com.studyblock.domain.settlement.entity.SettlementLedger;
import com.studyblock.domain.settlement.repository.SettlementHoldRepository;
import com.studyblock.domain.settlement.repository.SettlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementHoldService {

    private final SettlementHoldRepository settlementHoldRepository;
    private final SettlementLedgerRepository settlementLedgerRepository;

    @Transactional(readOnly = true)
    public List<SettlementHoldResponse> listHolds(String status, LocalDateTime from, LocalDateTime to, Long instructorId, String orderNumber) {
        // order와 user fetch join으로 조회하여 LazyInitializationException 방지
        List<SettlementHold> holds;
        if (status != null && !status.isBlank()) {
            holds = settlementHoldRepository.findByStatusWithRelations(status);
        } else {
            holds = settlementHoldRepository.findAllWithRelations();
        }

        // Filter in-memory for simplicity and maintainability (could be optimized later with queries)
        List<SettlementHold> filtered = holds.stream()
                .filter(h -> status == null || status.isBlank() || Objects.equals(h.getStatus(), status))
                .filter(h -> from == null || !h.getCreatedAt().isBefore(from))
                .filter(h -> to == null || !h.getCreatedAt().isAfter(to))
                .filter(h -> {
                    if (orderNumber == null || orderNumber.isBlank()) return true;
                    // order는 이미 fetch join으로 로드됨
                    return h.getOrderItem() != null
                    && h.getOrderItem().getOrder() != null
                    && orderNumber.equals(h.getOrderItem().getOrder().getOrderNumber());
                    })
                .filter(h -> {
                    if (instructorId == null) return true;
                    // order는 이미 fetch join으로 로드됨
                    if (h.getOrderItem() == null) return false;

                    List<SettlementLedger> allLedgers = settlementLedgerRepository.findAllByOrderItem_Id(h.getOrderItem().getId());
                    if (allLedgers.isEmpty()) return false;
                    // 첫 번째 정산 보류 상태 레코드의 강사 정보 사용
                    SettlementLedger firstLedger = allLedgers.get(0);
                    if (firstLedger.getInstructor() != null) {
                        firstLedger.getInstructor().getName(); // 강제 로딩
                        return Objects.equals(firstLedger.getInstructor().getId(), instructorId);
                    }
                    return false;
                })
                .collect(Collectors.toList());

        // Build response with instructor info and amount
        return filtered.stream()
                .map(h -> {
                    // order와 user는 이미 fetch join으로 로드됨
                    SettlementHoldResponse base = SettlementHoldResponse.from(h);
                    if (h.getOrderItem() == null) {
                        return base;
                    }
                    // 한 주문에 여러 정산 레코드가 있을 수 있으므로 findAllByOrder_Id 사용
                    // 정산 보류 상태인 레코드만 필터링 (eligibleFlag = false, settledAt = null)
                    List<SettlementLedger> allLedgers = settlementLedgerRepository.findAllByOrderItem_Id(h.getOrderItem().getId());
                    if (allLedgers.isEmpty()) {
                        return base;
                    }
                    // 첫 번째 정산 보류 상태 레코드의 강사 정보 사용
                    SettlementLedger firstLedger = allLedgers.get(0);
                    if (firstLedger.getInstructor() != null) {
                        firstLedger.getInstructor().getName(); // 강제 로딩
                    }
                    int holdNetSum = allLedgers.stream()
                        .filter(ledger -> !ledger.getEligibleFlag() && ledger.getSettledAt() == null)
                        .mapToInt(SettlementLedger::getNetAmount)
                        .sum();
                        SettlementHoldResponse withAmount = base.withHoldNetAmount(holdNetSum);
                    if (firstLedger.getInstructor() != null) {
                        return withAmount.withInstructor(
                                firstLedger.getInstructor().getId(),
                                firstLedger.getInstructor().getName()
                        );
                    }
                    return withAmount;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SettlementHoldResponse getHold(Long holdId) {
        SettlementHold hold = settlementHoldRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("정산 보류를 찾을 수 없습니다"));
        if (hold.getOrderItem() != null) hold.getOrderItem().getOrder().getOrderNumber();
        if (hold.getUser() != null) hold.getUser().getName();
        SettlementHoldResponse base = SettlementHoldResponse.from(hold);
        if (hold.getOrderItem() == null) {
            return base;
        }
        List<SettlementLedger> allLedgers = settlementLedgerRepository.findAllByOrderItem_Id(hold.getOrderItem().getId());
        if (allLedgers.isEmpty()) {
            return base;
        }

        SettlementLedger firstLedger = allLedgers.get(0);
        if (firstLedger.getInstructor() != null) {
            firstLedger.getInstructor().getName(); // 강제 로딩
        }
        int holdNetSum = allLedgers.stream()
                .filter(ledger -> !ledger.getEligibleFlag() && ledger.getSettledAt() == null)
                .mapToInt(SettlementLedger::getNetAmount)
                .sum();
        SettlementHoldResponse withAmount = base.withHoldNetAmount(holdNetSum);
        if (firstLedger.getInstructor() != null) {
            return withAmount.withInstructor(
                    firstLedger.getInstructor().getId(),
                    firstLedger.getInstructor().getName()
            );
        }
        return withAmount;
    }

    @Transactional
    public SettlementHoldResponse releaseHold(Long holdId) {
        SettlementHold hold = settlementHoldRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("정산 보류를 찾을 수 없습니다"));
        if (!"HELD".equals(hold.getStatus())) {
            throw new IllegalStateException("보류 상태가 아닙니다");
        }
        hold.release();
        settlementHoldRepository.save(hold);

        // Mark ledger eligible when hold released
        settlementLedgerRepository.findAllByOrderItem_Id(hold.getOrderItem().getId())
                .stream()
                .filter(ledger -> !ledger.getEligibleFlag() && ledger.getSettledAt() == null)
                .forEach(ledger -> {
                    if (!ledger.isEligible()) {
                        ledger.markEligible();
                        settlementLedgerRepository.save(ledger);
                    }
                });

        return getHold(holdId);
    }

    @Transactional
    public SettlementHoldResponse cancelHold(Long holdId) {
        SettlementHold hold = settlementHoldRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("정산 보류를 찾을 수 없습니다"));
        if (!"HELD".equals(hold.getStatus())) {
            throw new IllegalStateException("보류 상태가 아닙니다");
        }
        hold.cancel();
        settlementHoldRepository.save(hold);
        // Do not change ledger eligible flag on cancel (business rule)
        return getHold(holdId);
    }
}
