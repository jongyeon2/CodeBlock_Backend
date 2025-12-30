package com.studyblock.domain.settlement.repository;

import com.studyblock.domain.settlement.entity.SettlementPayment;
import com.studyblock.domain.settlement.enums.PaymentStatus;
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
public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, Long> {

// 기본 조회
List<SettlementPayment> findBySettlementLedger_Id(Long settlementLedgerId);

// 강사별 지급 내역 조회 (연관 관계 포함)
@Query("SELECT DISTINCT sp FROM SettlementPayment sp " +
       "JOIN FETCH sp.settlementLedger sl " +
       "JOIN FETCH sl.instructor " +
       "WHERE sl.instructor.id = :instructorId " +
       "ORDER BY sp.paymentDate DESC")
List<SettlementPayment> findByInstructorId(@Param("instructorId") Long instructorId);

// 강사별 지급 내역 조회 (페이징)
// 페이징과 JOIN FETCH를 함께 사용할 수 없으므로, Service의 @Transactional(readOnly = true) 내에서 DTO 변환
@Query("SELECT sp FROM SettlementPayment sp " +
       "WHERE sp.settlementLedger.instructor.id = :instructorId " +
       "ORDER BY sp.paymentDate DESC")
Page<SettlementPayment> findByInstructorId(@Param("instructorId") Long instructorId, Pageable pageable);

// 상태별 지급 내역 조회
List<SettlementPayment> findByStatus(PaymentStatus status);

// 특정 강사의 상태별 지급 내역
@Query("SELECT sp FROM SettlementPayment sp " +
       "WHERE sp.settlementLedger.instructor.id = :instructorId " +
       "AND sp.status = :status " +
       "ORDER BY sp.paymentDate DESC")
List<SettlementPayment> findByInstructorIdAndStatus(@Param("instructorId") Long instructorId,
                                                        @Param("status") PaymentStatus status);

// 기간별 지급 내역 조회
@Query("SELECT sp FROM SettlementPayment sp " +
       "WHERE sp.paymentDate BETWEEN :startDate AND :endDate " +
       "ORDER BY sp.paymentDate DESC")
List<SettlementPayment> findByPaymentDateBetween(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

// 특정 강사의 기간별 지급 내역
@Query("SELECT sp FROM SettlementPayment sp " +
       "WHERE sp.settlementLedger.instructor.id = :instructorId " +
       "AND sp.paymentDate BETWEEN :startDate AND :endDate " +
       "ORDER BY sp.paymentDate DESC")
List<SettlementPayment> findByInstructorIdAndPaymentDateBetween(@Param("instructorId") Long instructorId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate);

// 통계 쿼리

// 특정 강사의 총 지급 금액 (완료 건만)
@Query("SELECT SUM(sp.settlementLedger.netAmount) FROM SettlementPayment sp " +
       "WHERE sp.settlementLedger.instructor.id = :instructorId " +
       "AND sp.status = 'COMPLETED'")
Long sumCompletedAmountByInstructorId(@Param("instructorId") Long instructorId);

// 전체 지급 완료 총액
@Query("SELECT SUM(sp.settlementLedger.netAmount) FROM SettlementPayment sp " +
       "WHERE sp.status = 'COMPLETED'")
Long sumTotalCompletedAmount();

// 특정 기간 지급 완료 총액
@Query("SELECT SUM(sp.settlementLedger.netAmount) FROM SettlementPayment sp " +
       "WHERE sp.status = 'COMPLETED' " +
       "AND sp.paymentDate BETWEEN :startDate AND :endDate")
Long sumCompletedAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

// 지급 대기 건수
@Query("SELECT COUNT(sp) FROM SettlementPayment sp WHERE sp.status = 'PENDING'")
Long countPending();

// 특정 강사의 지급 대기 건수
@Query("SELECT COUNT(sp) FROM SettlementPayment sp " +
       "WHERE sp.settlementLedger.instructor.id = :instructorId " +
       "AND sp.status = 'PENDING'")
Long countPendingByInstructorId(@Param("instructorId") Long instructorId);

// 확인 번호로 조회
Optional<SettlementPayment> findByConfirmationNumber(String confirmationNumber);

// ID로 조회 (연관 관계 포함) - DTO 변환을 위해 필요한 연관 관계를 함께 조회
@Query("SELECT sp FROM SettlementPayment sp " +
       "JOIN FETCH sp.settlementLedger sl " +
       "JOIN FETCH sl.instructor " +
       "WHERE sp.id = :id")
Optional<SettlementPayment> findByIdWithRelations(@Param("id") Long id);
}
