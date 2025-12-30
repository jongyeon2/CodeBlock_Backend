package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.CookieBatch;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.wallet.enums.CookieType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CookieBatchRepository extends JpaRepository<CookieBatch, Long> {
       // 사용자별 활성 쿠키 배치 조회 (FIFO 순서)
       @Query("SELECT cb FROM CookieBatch cb WHERE cb.user = :user AND cb.isActive = true AND cb.qtyRemain > 0 " +
              "AND (cb.expiresAt IS NULL OR cb.expiresAt > :now) ORDER BY cb.createdAt ASC")
       List<CookieBatch> findAvailableBatchesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

       // 사용자별 쿠키 배치 조회
       List<CookieBatch> findByUserAndIsActiveTrueOrderByCreatedAtAsc(User user);
       List<CookieBatch> findByUserOrderByCreatedAtDesc(User user);

       // 만료된 쿠키 배치 조회
       @Query("SELECT cb FROM CookieBatch cb WHERE cb.expiresAt IS NOT NULL AND cb.expiresAt <= :now AND cb.isActive = true")
       List<CookieBatch> findExpiredBatches(@Param("now") LocalDateTime now);

       // 쿠키 타입별 배치 조회
       List<CookieBatch> findByUserAndCookieTypeAndIsActiveTrue(User user, CookieType cookieType);

       // 사용자별 총 사용 가능한 쿠키 수량
       @Query("SELECT COALESCE(SUM(cb.qtyRemain), 0) FROM CookieBatch cb WHERE cb.user = :user AND cb.isActive = true " +
              "AND (cb.expiresAt IS NULL OR cb.expiresAt > :now)")
       Integer getTotalAvailableCookies(@Param("user") User user, @Param("now") LocalDateTime now);

       // 보너스 쿠키(FREE) 조회 - 유효기간이 짧은 것부터 정렬 (FIFO: 만료일이 가까운 것부터)
       // MySQL 호환: NULLS LAST 대신 IS NULL을 먼저 정렬
       @Query("SELECT cb FROM CookieBatch cb WHERE cb.user = :user AND cb.isActive = true AND cb.qtyRemain > 0 " +
              "AND cb.cookieType = 'FREE' AND (cb.expiresAt IS NULL OR cb.expiresAt > :now) " +
              "ORDER BY CASE WHEN cb.expiresAt IS NULL THEN 1 ELSE 0 END ASC, cb.expiresAt ASC, cb.createdAt ASC")
       List<CookieBatch> findAvailableFreeBatchesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

       // 유료 쿠키(PAID) 조회 - 생성일 순서대로 (FIFO)
       @Query("SELECT cb FROM CookieBatch cb WHERE cb.user = :user AND cb.isActive = true AND cb.qtyRemain > 0 " +
              "AND cb.cookieType = 'PAID' AND (cb.expiresAt IS NULL OR cb.expiresAt > :now) " +
              "ORDER BY cb.createdAt ASC")
       List<CookieBatch> findAvailablePaidBatchesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

       // 월별 충전 합계 계산 (source가 PURCHASE이고 지정된 기간에 생성된 것들의 qty_total 합계)
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE cb.source = :source " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalBySourceAndDateRange(@Param("source") com.studyblock.domain.payment.enums.CookieSource source,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

       // 월별 충전 합계 계산 (source가 PURCHASE 또는 ADMIN이고 지정된 기간에 생성된 것들의 qty_total 합계)
       // 무료 쿠키(FREE)와 유료 쿠키(PAID) 모두 포함
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE (cb.source = 'PURCHASE' OR cb.source = 'ADMIN') " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalByPurchaseAndAdminAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

       // 월별 유료 쿠키 충전 합계 계산 (cookieType이 PAID인 모든 쿠키의 qty_total 합계 - source 무관)
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE cb.cookieType = 'PAID' " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalPaidByPurchaseAndAdminAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

       // 월별 무료 쿠키 충전 합계 계산 (cookieType이 FREE인 모든 쿠키의 qty_total 합계 - source 무관)
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE cb.cookieType = 'FREE' " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalFreeByPurchaseAndAdminAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

       // ===== 쿠키 잔액 조회 (qty_remain 합계) =====
       // 총 쿠키 잔액 (전체 사용자의 활성 쿠키 배치의 qty_remain 합계)
       @Query("SELECT COALESCE(SUM(cb.qtyRemain), 0) FROM CookieBatch cb WHERE cb.isActive = true " +
              "AND (cb.expiresAt IS NULL OR cb.expiresAt > :now)")
       Long sumQtyRemainTotal(@Param("now") LocalDateTime now);

       // 유료 쿠키 잔액 (cookie_type이 PAID인 활성 쿠키 배치의 qty_remain 합계)
       @Query("SELECT COALESCE(SUM(cb.qtyRemain), 0) FROM CookieBatch cb WHERE cb.isActive = true " +
              "AND cb.cookieType = 'PAID' AND (cb.expiresAt IS NULL OR cb.expiresAt > :now)")
       Long sumQtyRemainPaid(@Param("now") LocalDateTime now);

       // 무료 쿠키 잔액 (cookie_type이 FREE인 활성 쿠키 배치의 qty_remain 합계)
       @Query("SELECT COALESCE(SUM(cb.qtyRemain), 0) FROM CookieBatch cb WHERE cb.isActive = true " +
              "AND cb.cookieType = 'FREE' AND (cb.expiresAt IS NULL OR cb.expiresAt > :now)")
       Long sumQtyRemainFree(@Param("now") LocalDateTime now);

       // 만료된 무료 쿠키 잔액 (cookie_type이 FREE이고 만료된 쿠키 배치의 qty_remain 합계)
       @Query("SELECT COALESCE(SUM(cb.qtyRemain), 0) FROM CookieBatch cb WHERE cb.isActive = true " +
              "AND cb.cookieType = 'FREE' AND cb.expiresAt IS NOT NULL AND cb.expiresAt <= :now")
       Long sumQtyRemainExpiredFree(@Param("now") LocalDateTime now);

       // ===== 년도별 충전량 조회 (qty_total 합계) =====
       // 년도별 전체 충전량 (source가 PURCHASE이고 지정된 기간에 생성된 것들의 qty_total 합계)
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE cb.source = 'PURCHASE' " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalByPurchaseAndDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

       // 년도별 유료 쿠키 충전량 (cookieType이 PAID인 모든 쿠키의 qty_total 합계 - source 무관)
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE cb.cookieType = 'PAID' " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalPaidByPurchaseAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

       // 년도별 무료 쿠키 충전량 (cookieType이 FREE인 모든 쿠키의 qty_total 합계 - source 무관)
       @Query("SELECT COALESCE(SUM(cb.qtyTotal), 0) FROM CookieBatch cb WHERE cb.cookieType = 'FREE' " +
              "AND cb.createdAt >= :startDate AND cb.createdAt <= :endDate")
       Long sumQtyTotalFreeByPurchaseAndBonusAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
}
