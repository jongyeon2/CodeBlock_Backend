package com.studyblock.domain.coupon.repository;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.enums.CouponType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    // 기본 조회 메서드
    Optional<Coupon> findByName(String name);
    List<Coupon> findByType(CouponType type);
    
    // 활성화된 쿠폰 조회 (createdBy fetch join 포함)
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.createdBy WHERE c.isActive = :isActive")
    List<Coupon> findByIsActive(@Param("isActive") Boolean isActive);
    
    List<Coupon> findByTypeAndIsActive(CouponType type, Boolean isActive);

    // 생성자별 조회
    List<Coupon> findByCreatedBy_Id(Long creatorId);
    List<Coupon> findByCreatedBy_IdAndIsActive(Long creatorId, Boolean isActive);

    // 유효한 쿠폰 조회 (createdBy fetch join 포함)
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.createdBy WHERE c.validFrom <= :now AND c.validUntil >= :now AND c.isActive = true")
    List<Coupon> findValidCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.type = :type AND c.validFrom <= :now AND c.validUntil >= :now AND c.isActive = true")
    List<Coupon> findValidCouponsByType(@Param("type") CouponType type, @Param("now") LocalDateTime now);

    // 사용 가능한 쿠폰 조회 (한도 미달, createdBy fetch join 포함)
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.createdBy WHERE c.validFrom <= :now AND c.validUntil >= :now AND c.isActive = true AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.type = :type AND c.validFrom <= :now AND c.validUntil >= :now AND c.isActive = true AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findAvailableCouponsByType(@Param("type") CouponType type, @Param("now") LocalDateTime now);

    // 만료된 쿠폰 조회
    @Query("SELECT c FROM Coupon c WHERE c.validUntil < :now")
    List<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now);

    // 한도 초과 쿠폰 조회
    @Query("SELECT c FROM Coupon c WHERE c.usageLimit IS NOT NULL AND c.usedCount >= c.usageLimit")
    List<Coupon> findFullyUsedCoupons();

    // 할인 금액 기준 조회
    @Query("SELECT c FROM Coupon c WHERE c.discountValue >= :minDiscount AND c.isActive = true")
    List<Coupon> findByMinimumDiscount(@Param("minDiscount") Integer minDiscount);

    // 적용 가능한 쿠폰 조회 (금액 기준)
    @Query("SELECT c FROM Coupon c WHERE (c.minimumAmount IS NULL OR c.minimumAmount <= :orderAmount) AND c.validFrom <= :now AND c.validUntil >= :now AND c.isActive = true")
    List<Coupon> findApplicableCoupons(@Param("orderAmount") Integer orderAmount, @Param("now") LocalDateTime now);
    
    // 날짜 범위 조회
    List<Coupon> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Coupon> findByValidFromBetweenAndIsActive(LocalDateTime startDate, LocalDateTime endDate, Boolean isActive);
    
    // 통계 쿼리
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.isActive = true")
    Long countActiveCoupons();
    
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.type = :type AND c.isActive = true")
    Long countActiveCouponsByType(@Param("type") CouponType type);
    
    @Query("SELECT SUM(c.usedCount) FROM Coupon c")
    Long sumTotalUsedCount();
    
    @Query("SELECT SUM(c.usedCount) FROM Coupon c WHERE c.type = :type")
    Long sumUsedCountByType(@Param("type") CouponType type);
    
    @Query("SELECT AVG(c.usedCount) FROM Coupon c WHERE c.usageLimit IS NOT NULL")
    Double avgUsedCount();
    
    // 최근 쿠폰 조회
    List<Coupon> findTop10ByOrderByCreatedAtDesc();
    List<Coupon> findTop10ByTypeOrderByCreatedAtDesc(CouponType type);

    // createdBy fetch join으로 조회 (LazyInitializationException 방지)
    // LEFT JOIN FETCH를 사용하여 createdBy가 NULL인 경우도 포함
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.createdBy")
    List<Coupon> findAllWithCreator();
}
