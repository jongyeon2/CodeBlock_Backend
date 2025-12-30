package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.dto.CourseStatisticsResponse;
import com.studyblock.domain.settlement.dto.InstructorSummary;
import com.studyblock.domain.settlement.dto.SettlementDashboardResponse;
import com.studyblock.domain.settlement.dto.SettlementLedgerResponse;
import com.studyblock.domain.settlement.dto.SettlementSummaryResponse;
import com.studyblock.domain.settlement.entity.SettlementLedger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 서비스 Facade
 * 기존 코드와의 호환성을 유지하면서 실제 작업은 전문화된 서비스로 위임
 * 
 * 역할별 서비스:
 * - SettlementLedgerCreationService: 정산 레코드 생성
 * - SettlementLifecycleService: 환불 처리, 정산 가능 상태 변경, 정산 실행
 * - SettlementQueryService: 금액 조회, 정산 내역 조회
 * - SettlementStatisticsService: 통계, 대시보드, 요약 정보
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    // 전문화된 서비스들
    private final SettlementLedgerCreationService ledgerCreationService;
    private final SettlementLifecycleService lifecycleService;
    private final SettlementQueryService queryService;
    private final SettlementStatisticsService statisticsService;
    // ========================================
    // 1. 정산 레코드 생성 (위임)
    // ========================================

    /**
     * 결제 완료 시 정산 레코드 생성
     */
    @Transactional
    public void createSettlementLedgers(Long orderId) {
        ledgerCreationService.createSettlementLedgers(orderId);
    }

    // ========================================
    // 2. 환불/정산 생명주기 관리 (위임)
    // ========================================

    /**
     * 환불 시 정산 레코드를 정산 불가로 변경
     */
    @Transactional
    public void markSettlementIneligible(Long orderId) {
        lifecycleService.markSettlementIneligible(orderId);
    }

    /**
     * 환불 기간(7일) 지난 항목을 정산 가능으로 변경
     */
    @Transactional
    public int markEligibleForSettlement() {
        return lifecycleService.markEligibleForSettlement();
    }

    /**
     * 특정 강사의 정산 실행
     */
    @Transactional
    public int settleForInstructor(Long instructorId) {
        return lifecycleService.settleForInstructor(instructorId);
    }

    /**
     * 전체 정산 실행
     */
    @Transactional
    public int settleAll() {
        return lifecycleService.settleAll();
    }

    // ========================================
    // 3. 금액 및 내역 조회 (위임)
    // ========================================

    @Transactional(readOnly = true)
    public Integer getEligibleAmountForInstructor(Long instructorId) {
        return queryService.getEligibleAmountForInstructor(instructorId);
    }

    @Transactional(readOnly = true)
    public Integer getSettledAmountForInstructor(Long instructorId) {
        return queryService.getSettledAmountForInstructor(instructorId);
    }

    @Transactional(readOnly = true)
    public Integer getPendingAmountForInstructor(Long instructorId) {
        return queryService.getPendingAmountForInstructor(instructorId);
    }

    @Transactional(readOnly = true)
    public Integer getTotalEligibleAmount() {
        return queryService.getTotalEligibleAmount();
    }

    @Transactional(readOnly = true)
    public Integer getTotalPlatformFee() {
        return queryService.getTotalPlatformFee();
    }

    @Transactional(readOnly = true)
    public List<SettlementLedger> getInstructorSettlements(Long instructorId) {
        return queryService.getInstructorSettlements(instructorId);
    }

    @Transactional(readOnly = true)
    public Page<SettlementLedgerResponse> getInstructorSettlements(Long instructorId, Pageable pageable) {
        return queryService.getInstructorSettlements(instructorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SettlementLedgerResponse> getInstructorSettlementsWithFilters(
            Long instructorId, LocalDateTime startDate, LocalDateTime endDate, String status, Pageable pageable) {
        return queryService.getInstructorSettlementsWithFilters(instructorId, startDate, endDate, status, pageable);
    }

    // ========================================
    // 4. 통계 및 대시보드 (위임)
    // ========================================

    @Transactional(readOnly = true)
    public SettlementSummaryResponse getInstructorSummary(Long instructorId) {
        return statisticsService.getInstructorSummary(instructorId);
    }

    @Transactional(readOnly = true)
    public SettlementDashboardResponse getDashboard() {
        return statisticsService.getDashboard();
    }

    @Transactional(readOnly = true)
    public List<InstructorSummary> getInstructorSummaries() {
        return statisticsService.getInstructorSummaries();
    }

    @Transactional(readOnly = true)
    public List<CourseStatisticsResponse> getCourseStatisticsByInstructor(Long instructorId) {
        return statisticsService.getCourseStatisticsByInstructor(instructorId);
    }
}

