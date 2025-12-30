package com.studyblock.domain.settlement.service;

import com.studyblock.domain.settlement.dto.CourseStatisticsResponse;
import com.studyblock.domain.settlement.dto.InstructorSummary;
import com.studyblock.domain.settlement.dto.SettlementDashboardResponse;
import com.studyblock.domain.settlement.dto.SettlementSummaryResponse;
import com.studyblock.domain.settlement.repository.SettlementLedgerRepository;
import com.studyblock.domain.settlement.repository.SettlementPaymentRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 정산 통계 및 대시보드 서비스
 * 정산 요약, 대시보드, 강의별 통계 조회를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementStatisticsService {

    private final SettlementLedgerRepository settlementLedgerRepository;
    private final SettlementPaymentRepository settlementPaymentRepository;
    private final UserRepository userRepository;
    private final SettlementQueryService settlementQueryService;

    // ========================================
    // 강사별 요약 정보
    // ========================================

    /**
     * 강사별 정산 요약 정보 조회
     * 
     * @param instructorId 강사 ID
     * @return 정산 요약 정보
     * @throws IllegalArgumentException 강사를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public SettlementSummaryResponse getInstructorSummary(Long instructorId) {
        // 강사 정보 조회
        User instructor = getUserOrThrow(instructorId, "강사를 찾을 수 없습니다");

        // 금액 정보
        Integer pendingAmount = settlementQueryService.getPendingAmountForInstructor(instructorId);
        Integer eligibleAmount = settlementQueryService.getEligibleAmountForInstructor(instructorId);
        Integer settledAmount = settlementQueryService.getSettledAmountForInstructor(instructorId);
        Integer totalAmount = pendingAmount + eligibleAmount + settledAmount;

        // 건수 정보
        Long pendingCount = settlementLedgerRepository.countPendingByInstructorId(instructorId);
        Long eligibleCount = settlementLedgerRepository.countEligibleByInstructorId(instructorId);
        Long settledCount = settlementLedgerRepository.countSettledByInstructorId(instructorId);

        return SettlementSummaryResponse.builder()
                .instructorId(instructorId)
                .instructorName(instructor.getName())
                .pendingAmount(pendingAmount)
                .eligibleAmount(eligibleAmount)
                .settledAmount(settledAmount)
                .totalAmount(totalAmount)
                .pendingCount(convertToLong(pendingCount))
                .eligibleCount(eligibleCount)
                .settledCount(settledCount)
                .build();
    }

    /**
     * 모든 강사의 정산 요약 정보 조회
     * 
     * @return 강사별 요약 정보 목록
     */
    @Transactional(readOnly = true)
    public List<InstructorSummary> getInstructorSummaries() {
        List<Long> instructorIds = settlementLedgerRepository.findDistinctInstructorIds();
        
        log.info("강사 목록 생성 - 강사 수: {}", instructorIds.size());

        return instructorIds.stream()
                .map(this::buildInstructorSummary)
                .filter(summary -> summary != null)
                .collect(Collectors.toList());
    }

    /**
     * 강사별 요약 정보 생성
     */
    private InstructorSummary buildInstructorSummary(Long instructorId) {
        // 강사 정보 조회
        User instructor = userRepository.findById(instructorId).orElse(null);
        
        if (instructor == null) {
            log.warn("강사 정보를 찾을 수 없습니다 - instructorId: {}", instructorId);
            return null;
        }

        // 금액 정보
        Integer pendingAmount = settlementQueryService.getPendingAmountForInstructor(instructorId);
        Integer eligibleAmount = settlementQueryService.getEligibleAmountForInstructor(instructorId);
        Integer settledAmount = settlementQueryService.getSettledAmountForInstructor(instructorId);
        // 지급 완료 금액 (SettlementPayment status = COMPLETED)
        Integer paidAmount = convertToInteger(settlementPaymentRepository.sumCompletedAmountByInstructorId(instructorId));

        // 건수 정보
        Long pendingCount = convertToLong(settlementLedgerRepository.countPendingByInstructorId(instructorId));
        Long eligibleCount = convertToLong(settlementLedgerRepository.countEligibleByInstructorId(instructorId));
        Long settledCount = convertToLong(settlementLedgerRepository.countSettledByInstructorId(instructorId));

        return InstructorSummary.builder()
                .instructorId(instructorId)
                .instructorName(instructor.getName())
                .pendingAmount(pendingAmount)
                .eligibleAmount(eligibleAmount)
                .settledAmount(settledAmount)
                .paidAmount(paidAmount)
                .pendingCount(pendingCount)
                .eligibleCount(eligibleCount)
                .settledCount(settledCount)
                .build();
    }

    // ========================================
    // 대시보드
    // ========================================

    /**
     * 전체 정산 대시보드 조회 (관리자용)
     * 
     * @return 정산 대시보드 정보
     */
    @Transactional(readOnly = true)
    public SettlementDashboardResponse getDashboard() {
        // 전체 금액 정보
        Integer totalPendingAmount = convertToInteger(settlementLedgerRepository.sumTotalPendingAmount());
        Integer totalEligibleAmount = settlementQueryService.getTotalEligibleAmount();
        Integer totalSettledAmount = convertToInteger(settlementLedgerRepository.sumTotalSettledAmount());
        Integer totalPlatformFee = settlementQueryService.getTotalPlatformFee();

        // 전체 건수 정보
        Long totalPendingCount = convertToLong(settlementLedgerRepository.countPending());
        Long totalEligibleCount = settlementLedgerRepository.countEligible();
        Long totalSettledCount = settlementLedgerRepository.countSettled();

        // 강사별 요약 정보
        List<InstructorSummary> instructors = getInstructorSummaries();

        return SettlementDashboardResponse.builder()
                .totalPendingAmount(totalPendingAmount)
                .totalEligibleAmount(totalEligibleAmount)
                .totalSettledAmount(totalSettledAmount)
                .totalPlatformFee(totalPlatformFee)
                .totalPendingCount(totalPendingCount)
                .totalEligibleCount(totalEligibleCount)
                .totalSettledCount(totalSettledCount)
                .instructors(instructors)
                .build();
    }

    // ========================================
    // 강의별 통계
    // ========================================

    /**
     * 특정 강사의 강의별 통계 조회
     * 
     * @param instructorId 강사 ID
     * @return 강의별 통계 목록
     */
    @Transactional(readOnly = true)
    public List<CourseStatisticsResponse> getCourseStatisticsByInstructor(Long instructorId) {
        List<Object[]> statistics = settlementLedgerRepository.getCourseStatisticsByInstructor(instructorId);

        return statistics.stream()
                .map(stat -> buildCourseStatistics(stat, instructorId))
                .collect(Collectors.toList());
    }

    /**
     * 강의별 통계 응답 객체 생성
     */
    private CourseStatisticsResponse buildCourseStatistics(Object[] stat, Long instructorId) {
        // 기본 통계 정보 추출
        Long courseId = (Long) stat[0];
        String courseName = (String) stat[1];
        Long totalSales = (Long) stat[2];
        Integer totalAmount = convertNumberToInteger(stat[3]);
        Integer avgAmount = convertNumberToInteger(stat[4]);
        Integer minAmount = convertNumberToInteger(stat[5]);
        Integer maxAmount = convertNumberToInteger(stat[6]);

        // 상태별 건수 조회
        Long pendingCount = convertToLong(settlementLedgerRepository.countPendingByCourse(instructorId, courseId));
        Long eligibleCount = convertToLong(settlementLedgerRepository.countEligibleByCourse(instructorId, courseId));
        Long settledCount = convertToLong(settlementLedgerRepository.countSettledByCourse(instructorId, courseId));

        return CourseStatisticsResponse.builder()
                .courseId(courseId)
                .courseName(courseName)
                .totalSales(totalSales)
                .totalAmount(totalAmount)
                .avgAmount(avgAmount)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .pendingCount(pendingCount)
                .eligibleCount(eligibleCount)
                .settledCount(settledCount)
                .build();
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    /**
     * User 조회 (없으면 예외 발생)
     */
    private User getUserOrThrow(Long userId, String message) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    /**
     * Long을 Integer로 변환 (null 안전)
     */
    private Integer convertToInteger(Long value) {
        return value != null ? value.intValue() : 0;
    }

    /**
     * Long 값 변환 (null 안전)
     */
    private Long convertToLong(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Number를 Integer로 변환 (null 안전)
     */
    private Integer convertNumberToInteger(Object value) {
        return value != null ? ((Number) value).intValue() : 0;
    }
}

