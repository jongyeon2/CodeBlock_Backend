package com.studyblock.domain.report.service;

import com.studyblock.domain.report.dto.ReportCreateRequest;
import com.studyblock.domain.report.dto.ReportResponse;
import com.studyblock.domain.report.dto.ReportStatisticsResponse;
import com.studyblock.domain.report.entity.Report;
import com.studyblock.domain.report.enums.ReportStatus;
import com.studyblock.domain.report.enums.ReportTargetType;
import com.studyblock.domain.report.repository.ReportRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.entity.Comment;
import com.studyblock.domain.community.repository.PostRepository;
import com.studyblock.domain.community.repository.CommentRepository;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.activitylog.enums.ActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CourseRepository courseRepository;
    private final ActivityLogService activityLogService;

    //신고 생성
    @Transactional
    public ReportResponse createReport(Long userId, ReportCreateRequest request) {
        log.info("신고 생성 요청 - userId: {}, targetType: {}, contentId: {}", 
                userId, request.getTargetType(), request.getContentId());

        // 신고한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 신고당한 사용자 조회
        User reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new IllegalArgumentException("신고당한 사용자를 찾을 수 없습니다. ID: " + request.getReportedUserId()));

        // 중복 신고 확인 (같은 사용자가 같은 컨텐츠를 이미 신고했는지)
        reportRepository.findByUserAndTargetTypeAndContentId(
                userId,
                request.getTargetType(),
                request.getContentId()
        ).ifPresent(existingReport -> {
            throw new IllegalStateException("이미 신고한 컨텐츠입니다.");
        });

        // 신고 생성
        Report report = Report.builder()
                .user(user)
                .reportedUser(reportedUser)
                .targetType(request.getTargetType())
                .reportReason(request.getReportReason())
                .contentId(request.getContentId())
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("신고 생성 완료 - reportId: {}", savedReport.getId());

        // 연관 관계 포함하여 다시 조회
        Report reportWithRelations = reportRepository.findByIdWithRelations(savedReport.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 신고 정보를 조회할 수 없습니다"));
        
        // 활동 로그 저장
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("reportId", reportWithRelations.getId());
            metadata.put("targetType", request.getTargetType().toString());
            metadata.put("contentId", request.getContentId());
            metadata.put("reportReason", request.getReportReason().toString());
            metadata.put("reportedUserId", request.getReportedUserId());
            metadata.put("reportedUserNickname", reportedUser.getNickname());
            
            String contentTitle = getContentTitle(request.getTargetType(), request.getContentId());
            String description = String.format("%s 신고 (사유: %s, 대상: %s)", 
                    request.getTargetType().toString(), 
                    request.getReportReason().toString(),
                    contentTitle != null ? contentTitle : "ID: " + request.getContentId());
            
            activityLogService.createLog(
                    userId,
                    ActionType.REPORT,
                    "REPORT",
                    reportWithRelations.getId(),
                    description,
                    null,
                    metadata
            );
            log.info("신고 활동 로그 저장 완료 - userId: {}, reportId: {}", userId, reportWithRelations.getId());
        } catch (Exception e) {
            log.error("신고 활동 로그 저장 실패 - userId: {}, reportId: {}", userId, reportWithRelations.getId(), e);
            // 로그 저장 실패는 신고 생성에 영향을 주지 않음
        }
        
        return toResponseWithContentTitle(reportWithRelations);
    }

    //마이페이지 신고한 내역 조회 GET /api/reports/my
    public Page<ReportResponse> getMyReports(Long userId, ReportStatus status, Pageable pageable) {
        log.info("내가 신고한 내역 조회 - userId: {}, status: {}", userId, status);
        Page<Report> reports;
        if (status != null) {
            // 상태별 필터링
            reports = reportRepository.findByUser_IdAndStatusOrderByReportedAtDesc(userId, status, pageable);
        } else {
            // 전체 조회
            reports = reportRepository.findByUser_IdOrderByReportedAtDesc(userId, pageable);
        }
        // 페이징과 JOIN FETCH를 함께 사용할 수 없으므로, 트랜잭션 내에서 연관관계를 강제 로드
        reports.getContent().forEach(report -> {
            report.getUser().getId();
            report.getUser().getName();
            report.getUser().getNickname();
            report.getReportedUser().getId();
            report.getReportedUser().getName();
            report.getReportedUser().getNickname();
        });
        return reports.map(this::toResponseWithContentTitle);
    }

    //신고 상세 조회 (본인이 신고한 것만) GET /api/reports/{reportId}
    public ReportResponse getReport(Long reportId, Long userId) {
        log.info("신고 상세 조회 - reportId: {}, userId: {}", reportId, userId);
        Report report = reportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다. ID: " + reportId));
        // 본인이 신고한 것만 조회 가능
        if (!report.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인이 신고한 신고만 조회할 수 있습니다.");
        }
        return toResponseWithContentTitle(report);
    }

    //관리자용 전체 신고 목록 조회 GET /api/admin/reports
    public Page<ReportResponse> getAllReports(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        log.info("전체 신고 목록 조회 - status: {}, targetType: {}", status, targetType);
        Page<Report> reports;
        if (status != null && targetType != null) {
            reports = reportRepository.findByTargetTypeAndStatusOrderByReportedAtDesc(targetType, status, pageable);
        } else if (status != null) {
            reports = reportRepository.findByStatusOrderByReportedAtDesc(status, pageable);
        } else if (targetType != null) {
            reports = reportRepository.findByTargetTypeOrderByReportedAtDesc(targetType, pageable);
        } else {
            // 전체 조회는 페이징과 JOIN FETCH를 함께 사용할 수 없으므로, 일반 조회 후 연관관계 강제 로드
            reports = reportRepository.findAll(pageable);
        }

        // 연관관계 강제 로드
        reports.getContent().forEach(report -> {
            report.getUser().getId();
            report.getUser().getName();
            report.getUser().getNickname();
            report.getReportedUser().getId();
            report.getReportedUser().getName();
            report.getReportedUser().getNickname();
        });

        return reports.map(this::toResponseWithContentTitle);
    }

    //관리자용 신고 상세 조회 GET /api/admin/reports/{reportId}
    public ReportResponse getReportForAdmin(Long reportId) {
        log.info("관리자용 신고 상세 조회 - reportId: {}", reportId);
        Report report = reportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다. ID: " + reportId));
        return toResponseWithContentTitle(report);
    }

    //관리자용 신고 검토 시작 PUT /api/admin/reports/{reportId}/review
    @Transactional
    public ReportResponse startReview(Long reportId) {
        log.info("신고 검토 시작 - reportId: {}", reportId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다. ID: " + reportId));
        if (!report.isPending()) {
            throw new IllegalStateException("대기 중인 신고만 검토를 시작할 수 있습니다.");
        }
        report.startReview();
        Report savedReport = reportRepository.save(report);
        Report reportWithRelations = reportRepository.findByIdWithRelations(savedReport.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 신고 정보를 조회할 수 없습니다"));
        return toResponseWithContentTitle(reportWithRelations);
    }

    //관리자용 신고 처리 완료 PUT /api/admin/reports/{reportId}/resolve
    @Transactional
    public ReportResponse resolveReport(Long reportId) {
        log.info("신고 처리 완료 - reportId: {}", reportId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다. ID: " + reportId));
        if (!report.isReviewing()) {
            throw new IllegalStateException("검토 중인 신고만 처리 완료할 수 있습니다.");
        }
        report.resolve();
        Report savedReport = reportRepository.save(report);
        
        // 자동 차단 로직: RESOLVED 신고가 3회 이상이면 자동 차단
        checkAndAutoBlock(savedReport);
        
        Report reportWithRelations = reportRepository.findByIdWithRelations(savedReport.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 신고 정보를 조회할 수 없습니다"));
        return toResponseWithContentTitle(reportWithRelations);
    }

    // 자동 차단 체크 메서드
    private void checkAndAutoBlock(Report report) {
        // RESOLVED 상태인 신고만 카운트
        Long resolvedCount = reportRepository.countByTargetTypeAndContentIdAndStatus(
                report.getTargetType(),
                report.getContentId(),
                ReportStatus.RESOLVED
        );
        
        if (resolvedCount >= 3) {
            log.info("자동 차단 실행 - targetType: {}, contentId: {}, resolvedCount: {}", 
                    report.getTargetType(), report.getContentId(), resolvedCount);
            
            // 3회 이상이면 자동 차단
            switch (report.getTargetType()) {
                case USER:
                    User user = userRepository.findById(report.getContentId()).orElse(null);
                    if (user != null && user.getStatusEnum() != com.studyblock.domain.user.enums.UserStatus.SUSPENDED) {
                        user.updateStatus(com.studyblock.domain.user.enums.UserStatus.SUSPENDED);
                        userRepository.save(user);
                        log.info("사용자 자동 차단 완료 - userId: {}", report.getContentId());
                    }
                    break;
                case COURSE:
                    Course course = courseRepository.findById(report.getContentId()).orElse(null);
                    if (course != null && course.getIsPublished()) {
                        course.unpublish();
                        courseRepository.save(course);
                        log.info("강의 자동 차단 완료 - courseId: {}", report.getContentId());
                    }
                    break;
                case POST:
                    Post post = postRepository.findById(report.getContentId()).orElse(null);
                    if (post != null && post.getStatus() != com.studyblock.domain.community.enums.ContentStatus.DELETED) {
                        post.delete();
                        postRepository.save(post);
                        log.info("게시글 자동 차단 완료 - postId: {}", report.getContentId());
                    }
                    break;
                case COMMENT:
                    Comment comment = commentRepository.findById(report.getContentId()).orElse(null);
                    if (comment != null && comment.getStatus() != com.studyblock.domain.community.enums.ContentStatus.DELETED) {
                        comment.delete();
                        commentRepository.save(comment);
                        log.info("댓글 자동 차단 완료 - commentId: {}", report.getContentId());
                    }
                    break;
                case VIDEO:
                    // VIDEO는 현재 구현되지 않음
                    break;
            }
        }
    }

    //관리자용 신고 거절 PUT /api/admin/reports/{reportId}/reject
    @Transactional
    public ReportResponse rejectReport(Long reportId) {
        log.info("신고 거절 - reportId: {}", reportId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다. ID: " + reportId));
        if (!report.isReviewing()) {
            throw new IllegalStateException("검토 중인 신고만 거절할 수 있습니다.");
        }
        report.reject();
        Report savedReport = reportRepository.save(report);
        Report reportWithRelations = reportRepository.findByIdWithRelations(savedReport.getId())
                .orElseThrow(() -> new IllegalStateException("저장된 신고 정보를 조회할 수 없습니다"));
        return toResponseWithContentTitle(reportWithRelations);
    }

    //관리자용 신고 통계 조회 GET /api/admin/reports/statistics
    public ReportStatisticsResponse getStatistics() {
        log.info("신고 통계 조회");

        // 전체 건수
        Long totalCount = reportRepository.count();

        // 상태별 건수
        Long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        Long reviewingCount = reportRepository.countByStatus(ReportStatus.REVIEWING);
        Long resolvedCount = reportRepository.countByStatus(ReportStatus.RESOLVED);
        Long rejectedCount = reportRepository.countByStatus(ReportStatus.REJECTED);

        // 타입별 건수
        Long postCount = reportRepository.countByTargetType(ReportTargetType.POST);
        Long commentCount = reportRepository.countByTargetType(ReportTargetType.COMMENT);
        Long userCount = reportRepository.countByTargetType(ReportTargetType.USER);
        Long videoCount = reportRepository.countByTargetType(ReportTargetType.VIDEO);
        Long courseCount = reportRepository.countByTargetType(ReportTargetType.COURSE);

        // null 체크 및 기본값 설정
        totalCount = totalCount != null ? totalCount : 0L;
        pendingCount = pendingCount != null ? pendingCount : 0L;
        reviewingCount = reviewingCount != null ? reviewingCount : 0L;
        resolvedCount = resolvedCount != null ? resolvedCount : 0L;
        rejectedCount = rejectedCount != null ? rejectedCount : 0L;
        postCount = postCount != null ? postCount : 0L;
        commentCount = commentCount != null ? commentCount : 0L;
        userCount = userCount != null ? userCount : 0L;
        videoCount = videoCount != null ? videoCount : 0L;
        courseCount = courseCount != null ? courseCount : 0L;

        return ReportStatisticsResponse.builder()
                .totalCount(totalCount)
                .pendingCount(pendingCount)
                .reviewingCount(reviewingCount)
                .resolvedCount(resolvedCount)
                .rejectedCount(rejectedCount)
                .postCount(postCount)
                .commentCount(commentCount)
                .userCount(userCount)
                .videoCount(videoCount)
                .courseCount(courseCount)
                .build();
    }

    // 신고 대상 컨텐츠의 제목/내용을 조회하여 ReportResponse 생성
    private ReportResponse toResponseWithContentTitle(Report report) {
        String contentTitle = getContentTitle(report.getTargetType(), report.getContentId());
        
        ReportResponse response = ReportResponse.from(report);
        return ReportResponse.builder()
                .id(response.getId())
                .userId(response.getUserId())
                .userName(response.getUserName())
                .userNickname(response.getUserNickname())
                .reportedUserId(response.getReportedUserId())
                .reportedUserName(response.getReportedUserName())
                .reportedUserNickname(response.getReportedUserNickname())
                .targetType(response.getTargetType())
                .contentId(response.getContentId())
                .contentTitle(contentTitle)
                .reportReason(response.getReportReason())
                .status(response.getStatus())
                .reportedAt(response.getReportedAt())
                .reportedActedAt(response.getReportedActedAt())
                .build();
    }

    // 신고 대상 타입과 ID로 제목/내용 조회
    private String getContentTitle(ReportTargetType targetType, Long contentId) {
        try {
            switch (targetType) {
                case POST:
                    Post post = postRepository.findById(contentId).orElse(null);
                    return post != null ? post.getTitle() : null;
                case COMMENT:
                    Comment comment = commentRepository.findById(contentId).orElse(null);
                    return comment != null ? comment.getContent() : null;
                case COURSE:
                    Course course = courseRepository.findById(contentId).orElse(null);
                    return course != null ? course.getTitle() : null;
                case USER:
                    User user = userRepository.findById(contentId).orElse(null);
                    return user != null ? user.getNickname() : null;
                case VIDEO:
                    // VIDEO는 현재 구현되지 않음
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("신고 대상 컨텐츠 제목 조회 실패 - targetType: {}, contentId: {}", targetType, contentId, e);
            return null;
        }
    }
}
