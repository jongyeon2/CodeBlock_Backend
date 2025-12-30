package com.studyblock.domain.idempotency.repository;

import com.studyblock.domain.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    
    // 기본 조회 메서드
    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);
    List<IdempotencyKey> findByUser_Id(Long userId);
    
    // 존재 여부 확인
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    // 상태별 조회 (status 기반)
    List<IdempotencyKey> findByStatus(String status);
    List<IdempotencyKey> findByUser_IdAndStatus(Long userId, String status);
    
    // 날짜 범위 조회
    List<IdempotencyKey> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<IdempotencyKey> findByExpiresAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 만료된 키 조회
    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.expiresAt < :now AND ik.status <> 'USED'")
    List<IdempotencyKey> findExpiredUnusedKeys(@Param("now") LocalDateTime now);
    
    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.expiresAt < :now")
    List<IdempotencyKey> findExpiredKeys(@Param("now") LocalDateTime now);
    
    // 유효한 키 조회
    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.expiresAt > :now AND ik.status <> 'USED'")
    List<IdempotencyKey> findValidKeys(@Param("now") LocalDateTime now);
    
    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.idempotencyKey = :key AND ik.expiresAt > :now AND ik.status <> 'USED'")
    Optional<IdempotencyKey> findValidKeyByIdempotencyKey(@Param("key") String key, @Param("now") LocalDateTime now);
    
    // 통계 쿼리
    @Query("SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.status = 'USED'")
    Long countUsedKeys();
    
    @Query("SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.status <> 'USED' AND ik.expiresAt > :now")
    Long countValidUnusedKeys(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.user.id = :userId AND ik.createdAt >= :startDate")
    Long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    // 최근 키 조회
    List<IdempotencyKey> findTop10ByUser_IdOrderByCreatedAtDesc(Long userId);
    
    // 정리 대상 키 조회 (만료되고 사용되지 않은 오래된 키)
    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.expiresAt < :thresholdDate AND ik.status <> 'USED'")
    List<IdempotencyKey> findKeysToCleanup(@Param("thresholdDate") LocalDateTime thresholdDate);
}

