package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

// 기본 쿼리 메서드
Optional<Order> findByOrderNumber(String orderNumber);

// OrderNumber로 조회 시 User를 함께 fetch (환불 처리용)
@Query("SELECT o FROM Order o " +
       "LEFT JOIN FETCH o.user " +
       "WHERE o.orderNumber = :orderNumber")
Optional<Order> findByOrderNumberWithUser(@Param("orderNumber") String orderNumber);
List<Order> findByUser_Id(Long userId);
List<Order> findByStatus(OrderStatus status);
List<Order> findByUser_IdAndStatus(Long userId, OrderStatus status);

// 날짜 범위 쿼리
List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
List<Order> findByUser_IdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

// 토스페이먼츠 연동 쿼리
Optional<Order> findByTossOrderId(String tossOrderId);
Optional<Order> findByIdempotencyKey(String idempotencyKey);

// 환불 가능 주문 조회
@Query("SELECT o FROM Order o WHERE o.refundableUntil IS NOT NULL AND o.refundableUntil > :now AND o.status = 'PAID'")
List<Order> findRefundableOrders(@Param("now") LocalDateTime now);

// 사용자별 주문 통계
@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status = :status")
Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

// 최근 주문 조회
List<Order> findTop10ByUser_IdOrderByCreatedAtDesc(Long userId);

// 사용자 주문 조회 (course, section 정보 포함)
@Query("SELECT DISTINCT o FROM Order o " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.course c " +
       "LEFT JOIN FETCH oi.section s " +
       "LEFT JOIN FETCH s.course " +
       "WHERE o.user.id = :userId " +
       "ORDER BY o.createdAt DESC")
List<Order> findByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
