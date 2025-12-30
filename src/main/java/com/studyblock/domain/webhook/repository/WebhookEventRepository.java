package com.studyblock.domain.webhook.repository;

import com.studyblock.domain.webhook.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    
    // 기본 조회 메서드
    List<WebhookEvent> findByPayment_Id(Long paymentId);
    List<WebhookEvent> findByEventType(String eventType);
    
    // 처리 상태별 조회
    @Query("SELECT we FROM WebhookEvent we WHERE we.processedAt IS NULL")
    List<WebhookEvent> findUnprocessedEvents();
    
    @Query("SELECT we FROM WebhookEvent we WHERE we.processedAt IS NOT NULL")
    List<WebhookEvent> findProcessedEvents();
    
    @Query("SELECT we FROM WebhookEvent we WHERE we.processedAt IS NULL ORDER BY we.createdAt ASC")
    List<WebhookEvent> findUnprocessedEventsOrderByCreatedAtAsc();
    
    // 이벤트 타입별 조회
    List<WebhookEvent> findByEventTypeAndProcessedAtIsNull(String eventType);
    List<WebhookEvent> findByEventTypeAndProcessedAtIsNotNull(String eventType);
    
    // 날짜 범위 조회
    List<WebhookEvent> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<WebhookEvent> findByProcessedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 특정 결제의 웹훅 이벤트
    List<WebhookEvent> findByPayment_IdOrderByCreatedAtDesc(Long paymentId);
    
    // 오래된 미처리 이벤트 조회
    @Query("SELECT we FROM WebhookEvent we WHERE we.processedAt IS NULL AND we.createdAt < :thresholdDate")
    List<WebhookEvent> findOldUnprocessedEvents(@Param("thresholdDate") LocalDateTime thresholdDate);
    
    // 통계 쿼리
    @Query("SELECT COUNT(we) FROM WebhookEvent we WHERE we.eventType = :eventType AND we.createdAt >= :startDate")
    Long countByEventTypeAndCreatedAtAfter(@Param("eventType") String eventType, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(we) FROM WebhookEvent we WHERE we.processedAt IS NULL")
    Long countUnprocessedEvents();
    
    @Query("SELECT COUNT(we) FROM WebhookEvent we WHERE we.processedAt IS NOT NULL AND we.processedAt BETWEEN :startDate AND :endDate")
    Long countProcessedEventsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, we.createdAt, we.processedAt)) FROM WebhookEvent we WHERE we.processedAt IS NOT NULL")
    Double avgProcessingTimeSeconds();
    
    // 최근 이벤트 조회
    List<WebhookEvent> findTop20ByOrderByCreatedAtDesc();
    List<WebhookEvent> findTop10ByEventTypeOrderByCreatedAtDesc(String eventType);
    
    // 결제 성공/실패 이벤트
    @Query("SELECT we FROM WebhookEvent we WHERE we.eventType = 'PAYMENT_SUCCESS'")
    List<WebhookEvent> findPaymentSuccessEvents();
    
    @Query("SELECT we FROM WebhookEvent we WHERE we.eventType = 'PAYMENT_FAILURE'")
    List<WebhookEvent> findPaymentFailureEvents();
    
    @Query("SELECT we FROM WebhookEvent we WHERE we.eventType = 'REFUND_SUCCESS'")
    List<WebhookEvent> findRefundSuccessEvents();
}

