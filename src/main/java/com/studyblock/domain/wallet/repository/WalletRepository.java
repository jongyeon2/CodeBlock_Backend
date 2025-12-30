package com.studyblock.domain.wallet.repository;

import com.studyblock.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    // 기본 조회 메서드
    Optional<Wallet> findByUser_Id(Long userId);
    List<Wallet> findByIsActive(Boolean isActive);
    
    // 활성화 상태 조회
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.isActive = true")
    Optional<Wallet> findActiveWalletByUserId(@Param("userId") Long userId);
    
    // 비활성화된 지갑 조회
    @Query("SELECT w FROM Wallet w WHERE w.isActive = false")
    List<Wallet> findInactiveWallets();
    
    // 존재 여부 확인
    boolean existsByUser_Id(Long userId);
    
    // 통계 쿼리
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.isActive = true")
    Long countActiveWallets();
    
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.isActive = false")
    Long countInactiveWallets();
}

