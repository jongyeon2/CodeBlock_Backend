package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.dto.SettlementLedgerResponse;
import com.studyblock.domain.settlement.entity.SettlementLedger;
import com.studyblock.domain.settlement.repository.SettlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 정산 조회 전담 서비스
 * 정산 금액 조회, 정산 내역 조회를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementQueryService {

    private final SettlementLedgerRepository settlementLedgerRepository;

    // ========================================
    // 금액 조회 (강사별)
    // ========================================

    /**
     * 특정 강사의 정산 가능 금액 조회
     * 
     * @param instructorId 강사 ID
     * @return 정산 가능 금액
     */
    @Transactional(readOnly = true)
    public Integer getEligibleAmountForInstructor(Long instructorId) {
        Long amount = settlementLedgerRepository.sumEligibleAmountByInstructorId(instructorId);
        return convertToInteger(amount);
    }

    /**
     * 특정 강사의 정산 완료 금액 조회
     * 
     * @param instructorId 강사 ID
     * @return 정산 완료 금액
     */
    @Transactional(readOnly = true)
    public Integer getSettledAmountForInstructor(Long instructorId) {
        Long amount = settlementLedgerRepository.sumSettledAmountByInstructorId(instructorId);
        return convertToInteger(amount);
    }

    /**
     * 특정 강사의 정산 대기 금액 조회 (환불 기간)
     * 
     * @param instructorId 강사 ID
     * @return 정산 대기 금액
     */
    @Transactional(readOnly = true)
    public Integer getPendingAmountForInstructor(Long instructorId) {
        Long amount = settlementLedgerRepository.sumPendingAmountByInstructorId(instructorId);
        return convertToInteger(amount);
    }

    // ========================================
    // 전체 금액 조회 (관리자용)
    // ========================================

    /**
     * 전체 정산 가능 금액 조회
     * 
     * @return 전체 정산 가능 금액
     */
    @Transactional(readOnly = true)
    public Integer getTotalEligibleAmount() {
        Long amount = settlementLedgerRepository.sumTotalEligibleAmount();
        return convertToInteger(amount);
    }

    /**
     * 전체 플랫폼 수수료 총액 조회
     * 
     * @return 플랫폼 수수료 총액
     */
    @Transactional(readOnly = true)
    public Integer getTotalPlatformFee() {
        Long amount = settlementLedgerRepository.sumTotalPlatformFee();
        return convertToInteger(amount);
    }

    // ========================================
    // 정산 내역 조회
    // ========================================

    /**
     * 특정 강사의 정산 내역 조회
     * 
     * @param instructorId 강사 ID
     * @return 정산 내역 목록
     */
    @Transactional(readOnly = true)
    public List<SettlementLedger> getInstructorSettlements(Long instructorId) {
        return settlementLedgerRepository.findByInstructor_Id(instructorId);
    }

    /**
     * 특정 강사의 정산 내역 조회 (페이징)
     * DTO 변환까지 완료된 상태로 반환
     * 
     * @param instructorId 강사 ID
     * @param pageable 페이징 정보
     * @return 정산 내역 페이지
     */
    @Transactional(readOnly = true)
    public Page<SettlementLedgerResponse> getInstructorSettlements(
            Long instructorId, Pageable pageable) {
        // 페이징된 결과 조회
        Page<SettlementLedger> ledgersPage = 
                settlementLedgerRepository.findByInstructor_IdOrderByCreatedAtDesc(instructorId, pageable);

        // LAZY 로딩 강제 실행
        eagerLoadLedgerRelations(ledgersPage.getContent());

        // DTO 변환 및 Page 객체 생성
        return convertToResponsePage(ledgersPage, pageable);
    }

    /**
     * 특정 강사의 정산 내역 조회 (필터링 + 페이징)
     * DTO 변환까지 완료된 상태로 반환
     * 
     * @param instructorId 강사 ID
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param status 상태 필터 (PENDING, ELIGIBLE, SETTLED)
     * @param pageable 페이징 정보
     * @return 정산 내역 페이지
     */
    @Transactional(readOnly = true)
    public Page<SettlementLedgerResponse> getInstructorSettlementsWithFilters(
            Long instructorId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String status,
            Pageable pageable) {

        // 필터링된 결과 조회
        Page<SettlementLedger> ledgersPage = settlementLedgerRepository.findByInstructorIdWithFilters(
            instructorId, startDate, endDate, status, pageable
        );

        // LAZY 로딩 강제 실행 (Course/Section 포함)
        eagerLoadLedgerRelationsWithCourse(ledgersPage.getContent());

        // DTO 변환 및 Page 객체 생성
        return convertToResponsePage(ledgersPage, pageable);
    }

    // ========================================
    // LAZY 로딩 헬퍼 메서드
    // ========================================

    /**
     * SettlementLedger 목록의 연관 관계 강제 로딩 (기본)
     */
    private void eagerLoadLedgerRelations(List<SettlementLedger> ledgers) {
        ledgers.forEach(ledger -> {
            // Instructor 정보
            ledger.getInstructor().getName();
            
            // Order 정보
            ledger.getOrder().getOrderNumber();
            
            // 주문자 정보
            if (ledger.getOrder().getUser() != null) {
                ledger.getOrder().getUser().getName();
                ledger.getOrder().getUser().getEmail();
            }
        });
    }

    /**
     * SettlementLedger 목록의 연관 관계 강제 로딩 (Course/Section 포함)
     */
    private void eagerLoadLedgerRelationsWithCourse(List<SettlementLedger> ledgers) {
        ledgers.forEach(ledger -> {
            // 기본 정보 로딩
            eagerLoadLedgerRelations(List.of(ledger));
            
            // OrderItem의 Course/Section 정보
            if (ledger.getOrderItem() != null) {
                if (ledger.getOrderItem().getCourse() != null) {
                    ledger.getOrderItem().getCourse().getTitle();
                }
                if (ledger.getOrderItem().getSection() != null) {
                    ledger.getOrderItem().getSection().getTitle();
                    if (ledger.getOrderItem().getSection().getCourse() != null) {
                        ledger.getOrderItem().getSection().getCourse().getTitle();
                    }
                }
            }
        });
    }

    /**
     * SettlementLedger Page를 DTO Response Page로 변환
     */
    private Page<SettlementLedgerResponse> convertToResponsePage(
            Page<SettlementLedger> ledgersPage, Pageable pageable) {
        List<SettlementLedgerResponse> responses = ledgersPage.getContent().stream()
                .map(SettlementLedgerResponse::from)
                .collect(Collectors.toList());

        return new PageImpl<>(
                responses,
                pageable,
                ledgersPage.getTotalElements()
        );
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    /**
     * Long을 Integer로 변환 (null 안전)
     */
    private Integer convertToInteger(Long value) {
        return value != null ? value.intValue() : 0;
    }
}

