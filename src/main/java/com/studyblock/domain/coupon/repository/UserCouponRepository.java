package com.studyblock.domain.coupon.repository;

import com.studyblock.domain.coupon.entity.UserCoupon;
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
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // UserCoupon + Coupon fetch join (LazyInitializationException 방지)
    @Query("SELECT uc FROM UserCoupon uc "+ "JOIN FETCH uc.coupon "+"JOIN FETCH uc.user " +"WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithCoupon(@Param("id") Long id);

    // 기본 조회 메서드
    Optional<UserCoupon> findByCouponCode(String couponCode);
    List<UserCoupon> findByUser_Id(Long userId);
    Page<UserCoupon> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // UserCoupon + Coupon + User fetch join (LazyInitializationException 방지)
    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon JOIN FETCH uc.user WHERE uc.user.id = :userId")
    List<UserCoupon> findByUser_IdWithCoupon(@Param("userId") Long userId);
    
    // UserCoupon + Coupon + User 조인 (페이징) - JOIN FETCH는 페이징과 함께 사용 불가
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon JOIN uc.user WHERE uc.user.id = :userId ORDER BY uc.createdAt DESC")
    Page<UserCoupon> findByUser_IdWithCoupon(@Param("userId") Long userId, Pageable pageable);
    
    List<UserCoupon> findByCoupon_Id(Long couponId);

    // 쿠폰 ID로 발급 내역 조회 (User fetch join - LazyInitializationException 방지)
    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.user JOIN FETCH uc.coupon WHERE uc.coupon.id = :couponId")
    List<UserCoupon> findByCoupon_IdWithUser(@Param("couponId") Long couponId);

    List<UserCoupon> findByUser_IdAndCoupon_Id(Long userId, Long couponId);

    // 쿠폰 코드로 사용자 쿠폰 조회
    Optional<UserCoupon> findByUser_IdAndCouponCode(Long userId, String couponCode);

    // 사용 여부별 조회
    List<UserCoupon> findByUser_IdAndIsUsed(Long userId, Boolean isUsed);
    List<UserCoupon> findByCoupon_IdAndIsUsed(Long couponId, Boolean isUsed);

    // 사용 가능한 쿠폰 조회 (AVAILABLE 상태만)
    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon JOIN FETCH uc.user WHERE uc.user.id = :userId AND uc.status = 'AVAILABLE' AND uc.expiresAt > :now")
    List<UserCoupon> findAvailableCouponsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.coupon.id = :couponId AND uc.isUsed = false AND uc.expiresAt > :now")
    Optional<UserCoupon> findAvailableCouponByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId, @Param("now") LocalDateTime now);

    // 만료된 쿠폰 조회
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.isUsed = false AND uc.expiresAt < :now")
    List<UserCoupon> findExpiredUnusedCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.isUsed = false AND uc.expiresAt < :now")
    List<UserCoupon> findExpiredUnusedCouponsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // 날짜 범위 조회
    List<UserCoupon> findByUser_IdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    List<UserCoupon> findByUser_IdAndUsedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    List<UserCoupon> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 사용된 쿠폰 조회
    List<UserCoupon> findByUser_IdAndIsUsedAndUsedAtBetween(Long userId, Boolean isUsed, LocalDateTime startDate, LocalDateTime endDate);

    // 곧 만료될 쿠폰 조회 (7일 이내)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.isUsed = false AND uc.expiresAt BETWEEN :now AND :endDate")
    List<UserCoupon> findExpiringSoonCoupons(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("endDate") LocalDateTime endDate);

    // 통계 쿼리
    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.isUsed = false AND uc.expiresAt > :now")
    Long countAvailableCouponsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.isUsed = true")
    Long countUsedCouponsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.id = :couponId")
    Long countIssuedCouponsByCouponId(@Param("couponId") Long couponId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.id = :couponId AND uc.isUsed = true")
    Long countUsedCouponsByCouponId(@Param("couponId") Long couponId);

    // 쿠폰 타입별 통계
    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.coupon.type = :type AND uc.isUsed = false AND uc.expiresAt > :now")
    Long countAvailableCouponsByUserIdAndType(@Param("userId") Long userId, @Param("type") String type, @Param("now") LocalDateTime now);

    // 최근 쿠폰 조회
    List<UserCoupon> findTop10ByUser_IdOrderByCreatedAtDesc(Long userId);
    List<UserCoupon> findTop10ByUser_IdAndIsUsedOrderByUsedAtDesc(Long userId, Boolean isUsed);

    // 중복 발급 체크
    boolean existsByUser_IdAndCoupon_Id(Long userId, Long couponId);

    // 쿠폰 ID로 이미 발급된 사용자 ID 목록만 조회 (엔티티 전체를 로드하지 않음)
    @Query("SELECT uc.user.id FROM UserCoupon uc WHERE uc.coupon.id = :couponId")
    List<Long> findUserIdsByCouponId(@Param("couponId") Long couponId);
}

