package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

// 기본 조회 메서드
List<OrderItem> findByOrder_Id(Long orderId);
List<OrderItem> findByCourse_Id(Long courseId);
Optional<OrderItem> findByOrder_IdAndCourse_Id(Long orderId, Long courseId);

// 쿠폰 관련 조회
List<OrderItem> findByCoupon_Id(Long couponId);
List<OrderItem> findByOrder_IdAndCoupon_IdIsNotNull(Long orderId);

// 할인 관련 조회
@Query("SELECT oi FROM OrderItem oi WHERE oi.discountAmount > 0")
List<OrderItem> findAllWithDiscount();

@Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.discountAmount > 0")
List<OrderItem> findByOrderIdWithDiscount(@Param("orderId") Long orderId);

// 통계 쿼리
@Query("SELECT SUM(oi.finalAmount) FROM OrderItem oi WHERE oi.order.id = :orderId")
Long sumFinalAmountByOrderId(@Param("orderId") Long orderId);

@Query("SELECT SUM(oi.discountAmount) FROM OrderItem oi WHERE oi.order.id = :orderId")
Long sumDiscountAmountByOrderId(@Param("orderId") Long orderId);

@Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.course.id = :courseId")
Long countByCourseId(@Param("courseId") Long courseId);

// 중복 구매 확인: 사용자가 이미 이 강의를 구매했는가?
@Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
        "FROM OrderItem oi " +
        "WHERE oi.order.user.id = :userId " +
        "AND oi.course.id = :courseId " +
        "AND oi.order.status = 'PAID'")
boolean existsByUserIdAndCourseIdAndPaid(@Param("userId") Long userId,
                                        @Param("courseId") Long courseId);
}

