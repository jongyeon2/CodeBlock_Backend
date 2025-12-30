package com.studyblock.domain.wallet.repository;

import com.studyblock.domain.wallet.entity.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {
    
    // 기본 조회 메서드
    List<WalletBalance> findByWallet_Id(Long walletId);
    Optional<WalletBalance> findByWallet_IdAndCurrencyCode(Long walletId, String currencyCode);
    List<WalletBalance> findByCurrencyCode(String currencyCode);
    
    // V16: 동결 금액 관련 조회
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.frozenAmount > 0")
    List<WalletBalance> findAllWithFrozenAmount();
    
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.wallet.id = :walletId AND wb.frozenAmount > 0")
    List<WalletBalance> findByWalletIdWithFrozenAmount(@Param("walletId") Long walletId);
    
    // V16: 한도 관련 조회
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.dailyLimit IS NOT NULL")
    List<WalletBalance> findAllWithDailyLimit();
    
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.monthlyLimit IS NOT NULL")
    List<WalletBalance> findAllWithMonthlyLimit();
    
    // V16: 업데이트 주체별 조회
    List<WalletBalance> findByLastUpdatedBy(String lastUpdatedBy);
    
    // 잔액 조회
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.amount >= :minAmount")
    List<WalletBalance> findByAmountGreaterThanEqual(@Param("minAmount") Long minAmount);
    
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.wallet.id = :walletId AND (wb.amount - wb.frozenAmount) >= :requiredAmount")
    Optional<WalletBalance> findByWalletIdWithSufficientBalance(@Param("walletId") Long walletId, @Param("requiredAmount") Long requiredAmount);
    
    // 통계 쿼리
    @Query("SELECT SUM(wb.amount) FROM WalletBalance wb WHERE wb.currencyCode = :currencyCode")
    Long sumAmountByCurrencyCode(@Param("currencyCode") String currencyCode);
    
    @Query("SELECT SUM(wb.frozenAmount) FROM WalletBalance wb WHERE wb.currencyCode = :currencyCode")
    Long sumFrozenAmountByCurrencyCode(@Param("currencyCode") String currencyCode);
    
    @Query("SELECT COUNT(wb) FROM WalletBalance wb WHERE wb.amount > 0")
    Long countNonZeroBalances();
}

