package com.studyblock.domain.refund.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.enums.RefundStatus;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    // 기본 조회 메서드
    Optional<Refund> findByRefundKey(String refundKey);
    Optional<Refund> findByIdempotencyKey(String idempotencyKey);
    List<Refund> findByUser_Id(Long userId);
    Page<Refund> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Refund> findByOrder_Id(Long orderId);
    List<Refund> findByPayment_Id(Long paymentId);
    List<Refund> findByStatus(RefundStatus status);
    Page<Refund> findByStatusOrderByCreatedAtDesc(RefundStatus status, Pageable pageable);

    // 복합 조회 메서드
    List<Refund> findByUser_IdAndStatus(Long userId, RefundStatus status);
    List<Refund> findByOrder_IdAndStatus(Long orderId, RefundStatus status);
    List<Refund> findByPayment_IdAndStatus(Long paymentId, RefundStatus status);

    // 날짜 범위 조회
    List<Refund> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Refund> findByRequestedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Refund> findByProcessedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 환불 경로별 조회
    List<Refund> findByRefundRoute(String refundRoute);
    List<Refund> findByRefundRouteAndStatus(String refundRoute, RefundStatus status);

    // V16: 환불 방법별 조회
    List<Refund> findByRefundMethod(String refundMethod);
    List<Refund> findByRefundMethodAndStatus(String refundMethod, RefundStatus status);

    // 관리자별 조회
    List<Refund> findByProcessorAdminId(Long processorAdminId);
    List<Refund> findByProcessorAdminIdAndStatus(Long processorAdminId, RefundStatus status);

    // 대기 중인 환불 조회
    List<Refund> findByStatusAndProcessedAtIsNull(RefundStatus status);
    List<Refund> findByStatusOrderByRequestedAtAsc(RefundStatus status);

    // 통계 쿼리
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.status = :status AND r.createdAt >= :startDate")
    Long countByStatusAndCreatedAtAfter(@Param("status") RefundStatus status, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(r.amount) FROM Refund r WHERE r.status = 'PROCESSED' AND r.createdAt BETWEEN :startDate AND :endDate")
    Long sumAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(r.refundAmountCash) FROM Refund r WHERE r.status = 'PROCESSED' AND r.createdAt BETWEEN :startDate AND :endDate")
    Long sumCashRefundByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(r.refundAmountCookie) FROM Refund r WHERE r.status = 'PROCESSED' AND r.createdAt BETWEEN :startDate AND :endDate")
    Long sumCookieRefundByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 최근 환불 조회
    List<Refund> findTop10ByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Refund> findTop10ByStatusOrderByRequestedAtDesc(RefundStatus status);

    // 환불 가능한 주문의 환불 조회
    @Query("SELECT r FROM Refund r WHERE r.order.id = :orderId AND r.status IN ('PENDING', 'APPROVED')")
    List<Refund> findActiveRefundsByOrderId(@Param("orderId") Long orderId);

    // 사용자별 환불 통계
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.user.id = :userId AND r.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") RefundStatus status);
    
    // V16: 은행 정보가 있는 환불 조회
    @Query("SELECT r FROM Refund r WHERE r.refundBank IS NOT NULL AND r.refundAccountNumber IS NOT NULL AND r.status = :status")
    List<Refund> findByStatusWithBankInfo(@Param("status") RefundStatus status);

    // Order fetch join으로 조회 (LazyInitializationException 방지)
    @Query("SELECT DISTINCT r FROM Refund r LEFT JOIN FETCH r.order")
    List<Refund> findAllWithOrder();

    // 상태별 환불 조회 (Order fetch join 포함)
    @Query("SELECT DISTINCT r FROM Refund r LEFT JOIN FETCH r.order WHERE r.status = :status")
    List<Refund> findByStatusWithOrder(@Param("status") RefundStatus status);
}
