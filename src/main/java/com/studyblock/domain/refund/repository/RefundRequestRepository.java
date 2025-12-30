package com.studyblock.domain.refund.repository;

import com.studyblock.domain.refund.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    
    // 기본 조회 메서드
    List<RefundRequest> findByOrder_Id(Long orderId);
    List<RefundRequest> findByUser_Id(Long userId);
    List<RefundRequest> findByResult(String result);
    List<RefundRequest> findByUser_IdAndResult(Long userId, String result);
    
    // 환불 경로별 조회
    List<RefundRequest> findByRefundRoute(String refundRoute);
    List<RefundRequest> findByRefundRouteAndResult(String refundRoute, String result);
    
    // 날짜 범위 조회
    List<RefundRequest> findByRequestedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<RefundRequest> findByProcessedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 처리 대기 중인 요청
    List<RefundRequest> findByResultAndProcessedAtIsNull(String result);
    List<RefundRequest> findByResultOrderByRequestedAtAsc(String result);
    
    // 관리자별 조회
    List<RefundRequest> findByProcessorAdminId(Long processorAdminId);
    List<RefundRequest> findByProcessorAdminIdAndResult(Long processorAdminId, String result);
    
    // 통계 쿼리
    @Query("SELECT COUNT(rr) FROM RefundRequest rr WHERE rr.result = :result AND rr.requestedAt >= :startDate")
    Long countByResultAndRequestedAtAfter(@Param("result") String result, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT SUM(rr.refundAmountCash) FROM RefundRequest rr WHERE rr.result = 'APPROVED' AND rr.requestedAt BETWEEN :startDate AND :endDate")
    Long sumCashRefundByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(rr.refundAmountCookie) FROM RefundRequest rr WHERE rr.result = 'APPROVED' AND rr.requestedAt BETWEEN :startDate AND :endDate")
    Long sumCookieRefundByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 최근 요청 조회
    List<RefundRequest> findTop10ByUser_IdOrderByRequestedAtDesc(Long userId);
    List<RefundRequest> findTop10ByResultOrderByRequestedAtDesc(String result);
    
    // 처리 시간 분석용
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, rr.requestedAt, rr.processedAt)) FROM RefundRequest rr WHERE rr.processedAt IS NOT NULL")
    Double avgProcessingTimeHours();
}

