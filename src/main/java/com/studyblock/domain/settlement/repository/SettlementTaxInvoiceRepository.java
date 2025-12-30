package com.studyblock.domain.settlement.repository;

import com.studyblock.domain.settlement.entity.SettlementTaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementTaxInvoiceRepository extends JpaRepository<SettlementTaxInvoice, Long> {

// 기본 조회
Optional<SettlementTaxInvoice> findBySettlementPayment_Id(Long settlementPaymentId);
Optional<SettlementTaxInvoice> findByInvoiceNumber(String invoiceNumber);

// 강사별 세금계산서 조회 (연관 관계 포함)
@Query("SELECT DISTINCT sti FROM SettlementTaxInvoice sti " +
       "JOIN FETCH sti.settlementPayment sp " +
       "JOIN FETCH sp.settlementLedger sl " +
       "JOIN FETCH sl.instructor " +
       "WHERE sl.instructor.id = :instructorId " +
       "ORDER BY sti.issueDate DESC")
List<SettlementTaxInvoice> findByInstructorId(@Param("instructorId") Long instructorId);

// 발행일 기준 조회
@Query("SELECT sti FROM SettlementTaxInvoice sti " +
       "WHERE sti.issueDate BETWEEN :startDate AND :endDate " +
       "ORDER BY sti.issueDate DESC")
List<SettlementTaxInvoice> findByIssueDateBetween(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

// 정산 기간 기준 조회
@Query("SELECT sti FROM SettlementTaxInvoice sti " +
       "WHERE sti.periodStart >= :periodStart AND sti.periodEnd <= :periodEnd " +
       "ORDER BY sti.issueDate DESC")
List<SettlementTaxInvoice> findByPeriod(@Param("periodStart") LocalDate periodStart,
                                          @Param("periodEnd") LocalDate periodEnd);

// 특정 강사의 발행일 기준 조회
@Query("SELECT sti FROM SettlementTaxInvoice sti " +
       "WHERE sti.settlementPayment.settlementLedger.instructor.id = :instructorId " +
       "AND sti.issueDate BETWEEN :startDate AND :endDate " +
       "ORDER BY sti.issueDate DESC")
List<SettlementTaxInvoice> findByInstructorIdAndIssueDateBetween(@Param("instructorId") Long instructorId,
                                                                      @Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate);

// 통계 쿼리

// 특정 강사의 총 세금계산서 발행 금액
@Query("SELECT SUM(sti.totalAmount) FROM SettlementTaxInvoice sti " +
       "WHERE sti.settlementPayment.settlementLedger.instructor.id = :instructorId")
Long sumTotalAmountByInstructorId(@Param("instructorId") Long instructorId);

// 전체 세금계산서 발행 금액
@Query("SELECT SUM(sti.totalAmount) FROM SettlementTaxInvoice sti")
Long sumTotalAmount();

// 특정 기간 세금계산서 발행 금액
@Query("SELECT SUM(sti.totalAmount) FROM SettlementTaxInvoice sti " +
       "WHERE sti.issueDate BETWEEN :startDate AND :endDate")
Long sumTotalAmountByDateRange(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

// 특정 연도의 세금계산서 개수 (일련번호 생성용)
@Query("SELECT COUNT(sti) FROM SettlementTaxInvoice sti " +
       "WHERE YEAR(sti.issueDate) = :year AND MONTH(sti.issueDate) = :month")
Long countByYearAndMonth(@Param("year") int year, @Param("month") int month);

// 파일 URL이 없는 세금계산서 조회 (파일 업로드 대기)
@Query("SELECT sti FROM SettlementTaxInvoice sti " +
       "WHERE sti.invoiceFileUrl IS NULL " +
       "ORDER BY sti.issueDate DESC")
List<SettlementTaxInvoice> findWithoutFile();

// ID로 조회 (연관 관계 포함) - DTO 변환을 위해 필요한 연관 관계를 함께 조회
@Query("SELECT sti FROM SettlementTaxInvoice sti " +
       "JOIN FETCH sti.settlementPayment sp " +
       "JOIN FETCH sp.settlementLedger sl " +
       "JOIN FETCH sl.instructor " +
       "WHERE sti.id = :id")
Optional<SettlementTaxInvoice> findByIdWithRelations(@Param("id") Long id);
}
