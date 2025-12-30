package com.studyblock.domain.settlement.repository;

import com.studyblock.domain.settlement.entity.SettlementHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementHoldRepository extends JpaRepository<SettlementHold, Long> {
    Optional<SettlementHold> findByOrderItem_Id(Long orderItemId);
    List<SettlementHold> findAllByOrderItem_Order_Id(Long orderId);

    @Query("select sh from SettlementHold sh join sh.orderItem oi where oi.order.id = :orderId")
    List<SettlementHold> findAllByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT sh FROM SettlementHold sh WHERE sh.status = 'HELD' AND sh.holdUntil <= :now")
    List<SettlementHold> findDueHolds(LocalDateTime now);

    // order와 user fetch join으로 조회 (LazyInitializationException 방지)
    @Query("SELECT DISTINCT sh FROM SettlementHold sh LEFT JOIN FETCH sh.orderItem oi LEFT JOIN FETCH oi.order LEFT JOIN FETCH sh.user")
    List<SettlementHold> findAllWithRelations();

    // 상태별 조회 (order와 user fetch join 포함)
    @Query("SELECT DISTINCT sh FROM SettlementHold sh LEFT JOIN FETCH sh.orderItem oi LEFT JOIN FETCH oi.order LEFT JOIN FETCH sh.user")
    List<SettlementHold> findByStatusWithRelations(@Param("status") String status);
}


