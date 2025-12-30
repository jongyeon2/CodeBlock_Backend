package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.DailyLimitAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLimitAggregateRepository extends JpaRepository<DailyLimitAggregate, Long> {

// 기본 조회 메서드
Optional<DailyLimitAggregate> findByUser_IdAndDate(Long userId, LocalDate date);
List<DailyLimitAggregate> findByUser_Id(Long userId);
List<DailyLimitAggregate> findByDate(LocalDate date);

// 날짜 범위 조회
List<DailyLimitAggregate> findByUser_IdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
List<DailyLimitAggregate> findByDateBetween(LocalDate startDate, LocalDate endDate);

// 한도 초과 조회
@Query("SELECT d FROM DailyLimitAggregate d WHERE d.cashSum >= :limit")
List<DailyLimitAggregate> findByCashSumExceeding(@Param("limit") Integer limit);

@Query("SELECT d FROM DailyLimitAggregate d WHERE d.cookieSum >= :limit")
List<DailyLimitAggregate> findByCookieSumExceeding(@Param("limit") Integer limit);

@Query("SELECT d FROM DailyLimitAggregate d WHERE d.user.id = :userId AND d.cashSum >= :limit AND d.date = :date")
Optional<DailyLimitAggregate> findByUserIdAndCashSumExceeding(@Param("userId") Long userId, @Param("limit") Integer limit, @Param("date") LocalDate date);

// 통계 쿼리
@Query("SELECT SUM(d.cashSum) FROM DailyLimitAggregate d WHERE d.user.id = :userId AND d.date BETWEEN :startDate AND :endDate")
Long sumCashByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

@Query("SELECT SUM(d.cookieSum) FROM DailyLimitAggregate d WHERE d.user.id = :userId AND d.date BETWEEN :startDate AND :endDate")
Long sumCookieByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

@Query("SELECT AVG(d.cashSum) FROM DailyLimitAggregate d WHERE d.user.id = :userId")
Double avgCashSumByUserId(@Param("userId") Long userId);

// 최근 데이터 조회
List<DailyLimitAggregate> findTop30ByUser_IdOrderByDateDesc(Long userId);
}

