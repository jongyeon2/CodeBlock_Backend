package com.studyblock.domain.coupon.repository;

import com.studyblock.domain.coupon.entity.CouponUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CouponUsageLogRepository extends JpaRepository<CouponUsageLog, Long> {
    
    // 기본 조회 메서드
    List<CouponUsageLog> findByUserCoupon_Id(Long userCouponId);
    List<CouponUsageLog> findByOrder_Id(Long orderId);
    List<CouponUsageLog> findByOrderItem_Id(Long orderItemId);
    
    // 사용자별 조회
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.user.id = :userId")
    List<CouponUsageLog> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.user.id = :userId ORDER BY cul.usedAt DESC")
    List<CouponUsageLog> findByUserIdOrderByUsedAtDesc(@Param("userId") Long userId);
    
    // 쿠폰별 사용 로그
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId")
    List<CouponUsageLog> findByCouponId(@Param("couponId") Long couponId);
    
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId ORDER BY cul.usedAt DESC")
    List<CouponUsageLog> findByCouponIdOrderByUsedAtDesc(@Param("couponId") Long couponId);
    
    // 날짜 범위 조회
    List<CouponUsageLog> findByUsedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.user.id = :userId AND cul.usedAt BETWEEN :startDate AND :endDate")
    List<CouponUsageLog> findByUserIdAndUsedAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId AND cul.usedAt BETWEEN :startDate AND :endDate")
    List<CouponUsageLog> findByCouponIdAndUsedAtBetween(@Param("couponId") Long couponId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 할인 금액 기준 조회
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.discountAmount >= :minDiscount")
    List<CouponUsageLog> findByMinimumDiscountAmount(@Param("minDiscount") Integer minDiscount);
    
    // 통계 쿼리
    @Query("SELECT COUNT(cul) FROM CouponUsageLog cul WHERE cul.userCoupon.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(cul) FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId")
    Long countByCouponId(@Param("couponId") Long couponId);
    
    @Query("SELECT SUM(cul.discountAmount) FROM CouponUsageLog cul WHERE cul.userCoupon.user.id = :userId")
    Long sumDiscountAmountByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(cul.discountAmount) FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId")
    Long sumDiscountAmountByCouponId(@Param("couponId") Long couponId);
    
    @Query("SELECT SUM(cul.discountAmount) FROM CouponUsageLog cul WHERE cul.usedAt BETWEEN :startDate AND :endDate")
    Long sumDiscountAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(cul.discountAmount) FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId")
    Double avgDiscountAmountByCouponId(@Param("couponId") Long couponId);
    
    // 쿠폰 타입별 통계
    @Query("SELECT COUNT(cul) FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.type = :type")
    Long countByCouponType(@Param("type") String type);
    
    @Query("SELECT SUM(cul.discountAmount) FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.type = :type")
    Long sumDiscountAmountByCouponType(@Param("type") String type);
    
    // 최근 사용 로그
    List<CouponUsageLog> findTop10ByOrderByUsedAtDesc();
    
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.user.id = :userId ORDER BY cul.usedAt DESC LIMIT 10")
    List<CouponUsageLog> findTop10ByUserIdOrderByUsedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT cul FROM CouponUsageLog cul WHERE cul.userCoupon.coupon.id = :couponId ORDER BY cul.usedAt DESC LIMIT 10")
    List<CouponUsageLog> findTop10ByCouponIdOrderByUsedAtDesc(@Param("couponId") Long couponId);
    
    // 주문별 쿠폰 사용 집계
    @Query("SELECT SUM(cul.discountAmount) FROM CouponUsageLog cul WHERE cul.order.id = :orderId")
    Long sumDiscountAmountByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT COUNT(cul) FROM CouponUsageLog cul WHERE cul.order.id = :orderId")
    Long countByOrderId(@Param("orderId") Long orderId);
}

