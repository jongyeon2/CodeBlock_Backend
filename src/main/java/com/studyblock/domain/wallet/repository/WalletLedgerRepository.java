package com.studyblock.domain.wallet.repository;

import com.studyblock.domain.wallet.entity.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {
    
    // 기본 조회 메서드
    List<WalletLedger> findByUser_Id(Long userId);
    List<WalletLedger> findByType(String type);
    // 느슨한 참조 기반 조회
    List<WalletLedger> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    // 날짜 범위 조회
    List<WalletLedger> findByUser_IdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    List<WalletLedger> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 타입별 조회
    List<WalletLedger> findByUser_IdAndType(Long userId, String type);
    List<WalletLedger> findByTypeAndCreatedAtBetween(String type, LocalDateTime startDate, LocalDateTime endDate);
    
    // 통계 쿼리
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.type = 'CHARGE'")
    Long sumChargedCookiesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.type = 'DEBIT'")
    Long sumDebitedCookiesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.type = 'REFUND'")
    Long sumRefundedCookiesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.type = 'EXPIRE'")
    Long sumExpiredCookiesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.type = 'CHARGE' AND wl.createdAt BETWEEN :startDate AND :endDate")
    Long sumChargedCookiesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.type = 'DEBIT' AND wl.createdAt BETWEEN :startDate AND :endDate")
    Long sumDebitedCookiesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 최근 거래 조회
    List<WalletLedger> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
    List<WalletLedger> findTop10ByTypeOrderByCreatedAtDesc(String type);
    
    // 거래 수 통계
    @Query("SELECT COUNT(wl) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.type = :type")
    Long countByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
    
    // 사용자별 월별 통계
    @Query("SELECT SUM(wl.cookieAmount) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.type = :type AND YEAR(wl.createdAt) = :year AND MONTH(wl.createdAt) = :month")
    Long sumCookieAmountByUserIdAndTypeAndMonth(@Param("userId") Long userId, @Param("type") String type, @Param("year") int year, @Param("month") int month);
}

