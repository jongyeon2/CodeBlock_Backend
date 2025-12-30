package com.studyblock.domain.settlement.repository;

import com.studyblock.domain.settlement.entity.SettlementLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementLedgerRepository extends JpaRepository<SettlementLedger, Long> {

    // 기본 조회
    Optional<SettlementLedger> findByOrderItem_Id(Long orderItemId);
    List<SettlementLedger> findAllByOrderItem_Id(Long orderItemId);
    List<SettlementLedger> findByInstructor_Id(Long instructorId);

    // 강사별 정산 내역 조회 (instructor, order fetch join 포함)
    @Query("SELECT DISTINCT sl FROM SettlementLedger sl LEFT JOIN FETCH sl.instructor LEFT JOIN FETCH sl.order WHERE sl.instructor.id = :instructorId")
    List<SettlementLedger> findByInstructorIdWithRelations(@Param("instructorId") Long instructorId);

    // 강사별 정산 내역 조회 (페이징, instructor, order fetch join 포함)
    @Query("SELECT DISTINCT sl FROM SettlementLedger sl LEFT JOIN FETCH sl.instructor LEFT JOIN FETCH sl.order WHERE sl.instructor.id = :instructorId")
    List<SettlementLedger> findByInstructorIdWithRelationsPaged(@Param("instructorId") Long instructorId, Pageable pageable);

    // 기본 페이징 메서드 (fetch join 미사용)
    Page<SettlementLedger> findByInstructor_IdOrderByCreatedAtDesc(Long instructorId, Pageable pageable);

    // 정산 가능한 항목 조회 (eligibleFlag = true, settledAt = null)
    @Query("SELECT sl FROM SettlementLedger sl WHERE sl.eligibleFlag = true AND sl.settledAt IS NULL")
    List<SettlementLedger> findEligibleForSettlement();

    // 특정 강사의 정산 가능한 항목 조회
    @Query("SELECT sl FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.eligibleFlag = true AND sl.settledAt IS NULL")
    List<SettlementLedger> findEligibleByInstructorId(@Param("instructorId") Long instructorId);

    // 정산 완료된 항목 조회
    @Query("SELECT sl FROM SettlementLedger sl WHERE sl.settledAt IS NOT NULL")
    List<SettlementLedger> findSettled();

    // 특정 강사의 정산 완료된 항목 조회
    @Query("SELECT sl FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.settledAt IS NOT NULL")
    List<SettlementLedger> findSettledByInstructorId(@Param("instructorId") Long instructorId);

    // 기간별 정산 내역 조회
    @Query("SELECT sl FROM SettlementLedger sl WHERE sl.createdAt BETWEEN :startDate AND :endDate")
    List<SettlementLedger> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);

    // 특정 강사의 기간별 정산 내역
    @Query("SELECT sl FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.createdAt BETWEEN :startDate AND :endDate")
    List<SettlementLedger> findByInstructorIdAndDateRange(@Param("instructorId") Long instructorId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    // 필터링이 포함된 강사별 정산 내역 조회 (페이징)
    // status: "PENDING", "ELIGIBLE", "SETTLED", null(전체)
    @Query("SELECT sl FROM SettlementLedger sl " +
           "WHERE sl.instructor.id = :instructorId " +
           "AND (:startDate IS NULL OR sl.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR sl.createdAt <= :endDate) " +
           "AND (:status IS NULL OR " +
           "     (:status = 'PENDING' AND sl.eligibleFlag = false AND sl.settledAt IS NULL) OR " +
           "     (:status = 'ELIGIBLE' AND sl.eligibleFlag = true AND sl.settledAt IS NULL) OR " +
           "     (:status = 'SETTLED' AND sl.settledAt IS NOT NULL))")
    Page<SettlementLedger> findByInstructorIdWithFilters(@Param("instructorId") Long instructorId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate,
                                                          @Param("status") String status,
                                                          Pageable pageable);

    // 모든 강사 ID 조회 (정산 레코드가 있는 강사만)
    @Query("SELECT DISTINCT sl.instructor.id FROM SettlementLedger sl")
    List<Long> findDistinctInstructorIds();


    // 통계 쿼리

    // 특정 강사의 정산 가능 총액
    @Query("SELECT SUM(sl.netAmount) FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.eligibleFlag = true AND sl.settledAt IS NULL")
    Long sumEligibleAmountByInstructorId(@Param("instructorId") Long instructorId);

    // 특정 강사의 정산 완료 총액
    @Query("SELECT SUM(sl.netAmount) FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.settledAt IS NOT NULL")
    Long sumSettledAmountByInstructorId(@Param("instructorId") Long instructorId);

    // 특정 강사의 정산 대기 총액 (환불 기간)
    @Query("SELECT SUM(sl.netAmount) FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.eligibleFlag = false AND sl.settledAt IS NULL")
    Long sumPendingAmountByInstructorId(@Param("instructorId") Long instructorId);

    // 전체 강사의 정산 가능 총액
    @Query("SELECT SUM(sl.netAmount) FROM SettlementLedger sl WHERE sl.eligibleFlag = true AND sl.settledAt IS NULL")
    Long sumTotalEligibleAmount();

    // 전체 플랫폼 수수료 총액
    @Query("SELECT SUM(sl.feeAmount) FROM SettlementLedger sl WHERE sl.settledAt IS NOT NULL")
    Long sumTotalPlatformFee();

    // 전체 정산 완료 총액
    @Query("SELECT SUM(sl.netAmount) FROM SettlementLedger sl WHERE sl.settledAt IS NOT NULL")
    Long sumTotalSettledAmount();

    // 전체 정산 대기 총액 (환불 기간)
    @Query("SELECT SUM(sl.netAmount) FROM SettlementLedger sl WHERE sl.eligibleFlag = false AND sl.settledAt IS NULL")
    Long sumTotalPendingAmount();

    // 특정 기간의 플랫폼 수수료
    @Query("SELECT SUM(sl.feeAmount) FROM SettlementLedger sl WHERE sl.settledAt BETWEEN :startDate AND :endDate")
    Long sumPlatformFeeByDateRange(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);

    // 강사별 정산 통계
    @Query("SELECT sl.instructor.id, COUNT(sl), SUM(sl.netAmount), SUM(sl.feeAmount) " +
        "FROM SettlementLedger sl " +
        "WHERE sl.settledAt BETWEEN :startDate AND :endDate " +
        "GROUP BY sl.instructor.id")
    List<Object[]> getInstructorSettlementStats(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // 정산 대기 중인 항목 개수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl WHERE sl.eligibleFlag = false AND sl.settledAt IS NULL")
    Long countPending();

    // 특정 강사의 정산 대기 중인 항목 개수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.eligibleFlag = false AND sl.settledAt IS NULL")
    Long countPendingByInstructorId(@Param("instructorId") Long instructorId);

    // 특정 강사의 정산 가능 항목 개수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.eligibleFlag = true AND sl.settledAt IS NULL")
    Long countEligibleByInstructorId(@Param("instructorId") Long instructorId);

    // 특정 강사의 정산 완료 항목 개수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl WHERE sl.instructor.id = :instructorId AND sl.settledAt IS NOT NULL")
    Long countSettledByInstructorId(@Param("instructorId") Long instructorId);

    // 전체 정산 가능 항목 개수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl WHERE sl.eligibleFlag = true AND sl.settledAt IS NULL")
    Long countEligible();

    // 전체 정산 완료 항목 개수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl WHERE sl.settledAt IS NOT NULL")
    Long countSettled();

    // ========================================
    // 강의별 통계 쿼리
    // ========================================

    // 특정 강사의 강의별 정산 통계
    // 결과: courseId, courseName, count, sum, avg, min, max
    @Query("SELECT oi.course.id, oi.course.title, COUNT(sl), SUM(sl.netAmount), AVG(sl.netAmount), MIN(sl.netAmount), MAX(sl.netAmount) " +
           "FROM SettlementLedger sl " +
           "JOIN sl.orderItem oi " +
           "WHERE sl.instructor.id = :instructorId " +
           "AND oi.course IS NOT NULL " +
           "GROUP BY oi.course.id, oi.course.title " +
           "ORDER BY SUM(sl.netAmount) DESC")
    List<Object[]> getCourseStatisticsByInstructor(@Param("instructorId") Long instructorId);

    // 특정 강사의 특정 강의별 상태별 건수
    @Query("SELECT COUNT(sl) FROM SettlementLedger sl " +
           "JOIN sl.orderItem oi " +
           "WHERE sl.instructor.id = :instructorId " +
           "AND oi.course.id = :courseId " +
           "AND sl.eligibleFlag = false AND sl.settledAt IS NULL")
    Long countPendingByCourse(@Param("instructorId") Long instructorId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(sl) FROM SettlementLedger sl " +
           "JOIN sl.orderItem oi " +
           "WHERE sl.instructor.id = :instructorId " +
           "AND oi.course.id = :courseId " +
           "AND sl.eligibleFlag = true AND sl.settledAt IS NULL")
    Long countEligibleByCourse(@Param("instructorId") Long instructorId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(sl) FROM SettlementLedger sl " +
           "JOIN sl.orderItem oi " +
           "WHERE sl.instructor.id = :instructorId " +
           "AND oi.course.id = :courseId " +
           "AND sl.settledAt IS NOT NULL")
    Long countSettledByCourse(@Param("instructorId") Long instructorId, @Param("courseId") Long courseId);
}

