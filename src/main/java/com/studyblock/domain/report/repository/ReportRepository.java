package com.studyblock.domain.report.repository;

import com.studyblock.domain.report.entity.Report;
import com.studyblock.domain.report.enums.ReportStatus;
import com.studyblock.domain.report.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // ID로 조회 (연관 관계 포함)
    @Query("SELECT r FROM Report r JOIN FETCH r.user JOIN FETCH r.reportedUser WHERE r.id = :id")
    Optional<Report> findByIdWithRelations(@Param("id") Long id);

    // 상태별 신고 조회
    List<Report> findByStatus(ReportStatus status);

    // 상태별 신고 조회 (페이징)
    Page<Report> findByStatusOrderByReportedAtDesc(ReportStatus status, Pageable pageable);

    // 타입별 신고 조회
    List<Report> findByTargetType(ReportTargetType targetType);

    // 타입별 신고 조회 (페이징)
    Page<Report> findByTargetTypeOrderByReportedAtDesc(ReportTargetType targetType, Pageable pageable);

    // 타입 + 상태별 신고 조회
    List<Report> findByTargetTypeAndStatus(ReportTargetType targetType, ReportStatus status);

    // 타입 + 상태별 신고 조회 (페이징)
    Page<Report> findByTargetTypeAndStatusOrderByReportedAtDesc(
            ReportTargetType targetType,
            ReportStatus status,
            Pageable pageable);

    // 신고한 사용자별 조회
    List<Report> findByUser_Id(Long userId);

    // 신고한 사용자별 조회 (페이징)
    Page<Report> findByUser_IdOrderByReportedAtDesc(Long userId, Pageable pageable);

    // 신고당한 사용자별 조회
    List<Report> findByReportedUser_Id(Long reportedUserId);

    // 신고당한 사용자별 조회 (페이징)
    Page<Report> findByReportedUser_IdOrderByReportedAtDesc(Long reportedUserId, Pageable pageable);

    // 신고한 사용자 + 상태별 조회
    List<Report> findByUser_IdAndStatus(Long userId, ReportStatus status);

    // 신고한 사용자 + 상태별 조회 (페이징)
    Page<Report> findByUser_IdAndStatusOrderByReportedAtDesc(Long userId, ReportStatus status, Pageable pageable);

    // 신고당한 사용자 + 상태별 조회
    List<Report> findByReportedUser_IdAndStatus(Long reportedUserId, ReportStatus status);

    // 특정 컨텐츠에 대한 신고 조회 (중복 신고 확인)
    @Query("SELECT r FROM Report r WHERE r.targetType = :targetType AND r.contentId = :contentId")
    List<Report> findByTargetTypeAndContentId(
            @Param("targetType") ReportTargetType targetType,
            @Param("contentId") Long contentId);

    // 특정 사용자가 특정 컨텐츠를 이미 신고했는지 확인
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.targetType = :targetType AND r.contentId = :contentId")
    Optional<Report> findByUserAndTargetTypeAndContentId(
            @Param("userId") Long userId,
            @Param("targetType") ReportTargetType targetType,
            @Param("contentId") Long contentId);

    // 신고 접수 기간별 조회
    List<Report> findByReportedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 신고 처리 기간별 조회
    List<Report> findByReportedActedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 통계 쿼리
    // 상태별 신고 건수
    Long countByStatus(ReportStatus status);

    // 타입별 신고 건수
    Long countByTargetType(ReportTargetType targetType);

    // 타입 + 상태별 신고 건수
    Long countByTargetTypeAndStatus(ReportTargetType targetType, ReportStatus status);

    // 특정 사용자가 신고한 건수
    Long countByUser_Id(Long userId);

    // 특정 사용자가 신고당한 건수
    Long countByReportedUser_Id(Long reportedUserId);

    // 특정 컨텐츠에 대한 신고 건수
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = :targetType AND r.contentId = :contentId")
    Long countByTargetTypeAndContentId(
            @Param("targetType") ReportTargetType targetType,
            @Param("contentId") Long contentId);

    // 특정 컨텐츠에 대한 RESOLVED 신고 건수
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = :targetType AND r.contentId = :contentId AND r.status = :status")
    Long countByTargetTypeAndContentIdAndStatus(
            @Param("targetType") ReportTargetType targetType,
            @Param("contentId") Long contentId,
            @Param("status") ReportStatus status);

    // 관리자용 조회
    // 전체 신고 조회 (페이징)
    @Query("SELECT DISTINCT r FROM Report r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.reportedUser ORDER BY r.reportedAt DESC")
    List<Report> findAllWithRelations();

    // 상태별 신고 조회
    @Query("SELECT DISTINCT r FROM Report r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.reportedUser WHERE r.status = :status ORDER BY r.reportedAt DESC")
    List<Report> findByStatusWithRelations(@Param("status") ReportStatus status);

    // 타입별 신고 조회
    @Query("SELECT DISTINCT r FROM Report r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.reportedUser WHERE r.targetType = :targetType ORDER BY r.reportedAt DESC")
    List<Report> findByTargetTypeWithRelations(@Param("targetType") ReportTargetType targetType);

    // 타입 + 상태별 신고 조회
    @Query("SELECT DISTINCT r FROM Report r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.reportedUser WHERE r.targetType = :targetType AND r.status = :status ORDER BY r.reportedAt DESC")
    List<Report> findByTargetTypeAndStatusWithRelations(
            @Param("targetType") ReportTargetType targetType,
            @Param("status") ReportStatus status);
}

