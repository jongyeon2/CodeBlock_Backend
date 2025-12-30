package com.studyblock.domain.coupon.repository;

import com.studyblock.domain.coupon.entity.CouponEvent;
import com.studyblock.domain.coupon.enums.CouponTargetUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {
    
    // 기본 조회 메서드
    Optional<CouponEvent> findByName(String name);
    List<CouponEvent> findByCoupon_Id(Long couponId);
    List<CouponEvent> findByIsActive(Boolean isActive);
    List<CouponEvent> findByTargetUsers(CouponTargetUsers targetUsers);
    
    // 활성 이벤트 조회
    List<CouponEvent> findByCoupon_IdAndIsActive(Long couponId, Boolean isActive);
    List<CouponEvent> findByTargetUsersAndIsActive(CouponTargetUsers targetUsers, Boolean isActive);
    
    // 생성자별 조회
    List<CouponEvent> findByCreatedBy_Id(Long creatorId);
    List<CouponEvent> findByCreatedBy_IdAndIsActive(Long creatorId, Boolean isActive);
    
    // 날짜 범위 조회
    List<CouponEvent> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<CouponEvent> findByIsActiveAndCreatedAtBetween(Boolean isActive, LocalDateTime startDate, LocalDateTime endDate);
    
    // 발급 수 기준 조회
    @Query("SELECT ce FROM CouponEvent ce WHERE ce.issuedCount >= :minIssued")
    List<CouponEvent> findByMinimumIssuedCount(@Param("minIssued") Integer minIssued);
    
    @Query("SELECT ce FROM CouponEvent ce WHERE ce.isActive = true ORDER BY ce.issuedCount DESC")
    List<CouponEvent> findActiveEventsOrderByIssuedCountDesc();
    
    // 통계 쿼리
    @Query("SELECT SUM(ce.issuedCount) FROM CouponEvent ce WHERE ce.coupon.id = :couponId")
    Long sumIssuedCountByCouponId(@Param("couponId") Long couponId);
    
    @Query("SELECT SUM(ce.issuedCount) FROM CouponEvent ce WHERE ce.isActive = true")
    Long sumIssuedCountForActiveEvents();
    
    @Query("SELECT COUNT(ce) FROM CouponEvent ce WHERE ce.isActive = true")
    Long countActiveEvents();
    
    @Query("SELECT AVG(ce.issuedCount) FROM CouponEvent ce WHERE ce.isActive = true")
    Double avgIssuedCountForActiveEvents();
    
    // 최근 이벤트 조회
    List<CouponEvent> findTop10ByOrderByCreatedAtDesc();
    List<CouponEvent> findTop10ByIsActiveOrderByCreatedAtDesc(Boolean isActive);
    List<CouponEvent> findTop10ByTargetUsersOrderByIssuedCountDesc(CouponTargetUsers targetUsers);
    
    // 쿠폰별 활성 이벤트 수
    @Query("SELECT COUNT(ce) FROM CouponEvent ce WHERE ce.coupon.id = :couponId AND ce.isActive = true")
    Long countActiveEventsByCouponId(@Param("couponId") Long couponId);
}

