package com.studyblock.domain.activitylog.repository;

import com.studyblock.domain.activitylog.entity.ActivityLog;
import com.studyblock.domain.activitylog.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 최근 로그 조회 (User 엔티티를 함께 로드)
    @Query("SELECT al FROM ActivityLog al JOIN FETCH al.user ORDER BY al.createdAt DESC")
    List<ActivityLog> findAllWithUserOrderByCreatedAtDesc();

    // 특정 사용자의 로그 조회 (User 엔티티를 함께 로드)
    @Query("SELECT al FROM ActivityLog al JOIN FETCH al.user WHERE al.user.id = :userId ORDER BY al.createdAt DESC")
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // 특정 액션 타입의 로그 조회 (User 엔티티를 함께 로드)
    @Query("SELECT al FROM ActivityLog al JOIN FETCH al.user WHERE al.actionType = :actionType ORDER BY al.createdAt DESC")
    List<ActivityLog> findByActionTypeOrderByCreatedAtDesc(@Param("actionType") ActionType actionType);

    // 기간별 조회 (User 엔티티를 함께 로드)
    @Query("SELECT al FROM ActivityLog al JOIN FETCH al.user WHERE al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<ActivityLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // 특정 사용자와 특정 액션 타입 로그 조회 (User 엔티티를 함께 로드)
    @Query("SELECT al FROM ActivityLog al JOIN FETCH al.user WHERE al.user.id = :userId AND al.actionType = :actionType ORDER BY al.createdAt DESC")
    List<ActivityLog> findByUserIdAndActionTypeOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("actionType") ActionType actionType);
}
