package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

// 기본 쿼리 메서드
Optional<Payment> findByPaymentKey(String paymentKey);
Optional<Payment> findByMerchantUid(String merchantUid);
Optional<Payment> findByIdempotencyKey(String idempotencyKey);
List<Payment> findByOrder_Id(Long orderId);
List<Payment> findByStatus(PaymentStatus status);

// 토스페이먼츠 연동 쿼리
Optional<Payment> findByOrder_IdAndStatus(Long orderId, PaymentStatus status);

// Payment 조회 시 Order와 User를 함께 fetch (환불 처리용)
@Query("SELECT p FROM Payment p " +
       "LEFT JOIN FETCH p.order o " +
       "LEFT JOIN FETCH o.user " +
       "WHERE p.id = :paymentId")
Optional<Payment> findByIdWithOrderAndUser(@Param("paymentId") Long paymentId);
// V27 정규화로 제거된 필드(type, country) 관련 메서드 삭제

// 날짜 범위 쿼리
List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
List<Payment> findByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime startDate, LocalDateTime endDate);

// 웹훅 관련 쿼리 제거 (webhook_received_at 컬럼 제거됨)

// 결제 통계
@Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt >= :startDate")
Long countByStatusAndCreatedAtAfter(@Param("status") PaymentStatus status, @Param("startDate") LocalDateTime startDate);

// 결제 금액 통계
@Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'CAPTURED' AND p.createdAt BETWEEN :startDate AND :endDate")
Long sumAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

// 최근 결제 조회
List<Payment> findTop10ByOrder_User_IdOrderByCreatedAtDesc(Long userId);
}