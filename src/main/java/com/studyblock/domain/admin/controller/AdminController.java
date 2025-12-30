package com.studyblock.domain.admin.controller;

import com.studyblock.domain.admin.dto.CourseListResponse;
import com.studyblock.domain.admin.dto.CourseStatusUpdateRequest;
import com.studyblock.domain.admin.dto.DashboardStatisticsResponse;
import com.studyblock.domain.admin.dto.ReviewListResponse;
import com.studyblock.domain.admin.dto.UserStatusUpdateRequest;
import com.studyblock.domain.admin.service.CourseListService;
import com.studyblock.domain.admin.service.DashboardStatisticsService;
import com.studyblock.domain.admin.service.ExcelService;
import com.studyblock.domain.admin.service.ReviewListService;
import com.studyblock.domain.admin.service.UserListService;
import com.studyblock.domain.community.service.PostService;
import com.studyblock.domain.community.service.CommentService;
import com.studyblock.domain.community.dto.PostResponse;
import com.studyblock.domain.community.dto.CommentResponse;
import com.studyblock.domain.course.dto.SectionResponse;
import com.studyblock.domain.course.service.SectionService;
import com.studyblock.domain.refund.dto.RefundResponse;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.enums.RefundStatus;
import com.studyblock.domain.refund.service.RefundService;
import com.studyblock.domain.activitylog.dto.ActivityLogResponse;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "관리자 관리", description = "관리자 대시보드 API")
public class AdminController {

    private final ExcelService excelService;
    private final UserListService userListService;
    private final CourseListService courseListService;
    private final ReviewListService reviewListService;
    private final RefundService refundService;
    private final SectionService sectionService;
    private final DashboardStatisticsService dashboardStatisticsService;
    private final ActivityLogService activityLogService;
    private final PostService postService;
    private final CommentService commentService;

    // 대시보드 통계 조회
    @Operation(summary = "대시보드 통계 조회", description = "관리자 대시보드의 모든 통계 데이터를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "조회 실패")
    })
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<DashboardStatisticsResponse> getDashboardStatistics() {
        try {
            DashboardStatisticsResponse statistics = dashboardStatisticsService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("대시보드 통계 조회 실패", e);
            return ResponseEntity.status(500).build();
            
        }
    }

    // 엑셀 파일 다운로드
    @Operation(summary = "엑셀 파일 다운로드", description = "목록을 엑셀 파일로 다운로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다운로드 성공"),
            @ApiResponse(responseCode = "500", description = "다운로드 실패")
    })
    @GetMapping("/excel/userList/download")
    public void downloadUserExcel(HttpServletResponse response) {
        try {
            excelService.downloadUserExcel(response);
        } catch (Exception e) {
            log.error("엑셀 다운로드 실패", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // 사용자 목록 조회
    @Operation(summary = "사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/userList")
    public List<UserProfileResponse> userList() {
        return userListService.getUserList();
    }

    // 코스 리스트 조회
    @Operation(summary = "코스 목록 조회", description = "모든 코스 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/courseList")
    public List<CourseListResponse> courseList() {
        return courseListService.getCourseList();
    }

    // 리뷰 목록 조회
    @Operation(summary = "리뷰 목록 조회", description = "모든 리뷰 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/reviewList")
    public List<ReviewListResponse> reviewList() {
        return reviewListService.getReviewList();
    }

    // 환불 목록 조회 (관리자용)
    @Operation(summary = "환불 목록 조회", description = "전체 환불 목록을 조회합니다. 상태별 필터링이 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/refundList")
    public List<RefundResponse> refundList(@RequestParam(required = false) String status) {
        List<Refund> refunds;

        if (status != null && !status.isBlank()) {
            try {
                RefundStatus refundStatus = RefundStatus.valueOf(status.toUpperCase());
                refunds = refundService.getRefundsByStatus(refundStatus);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 환불 상태: {}", status);
                refunds = refundService.getAllRefunds();
            }
        } else {
            refunds = refundService.getAllRefunds();
        }

        return refunds.stream()
                .map(RefundResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }

    // 사용자 상태 변경 (차단/해제)
    @Operation(summary = "사용자 상태 변경", description = "사용자의 상태를 변경합니다 (차단/해제).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateRequest request) {
        try {
            userListService.updateUserStatus(userId, request.getStatus());
            return ResponseEntity.ok(Map.of("message", "사용자 상태가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 사용자 활동 로그 조회
    @Operation(summary = "사용자 활동 로그 조회", description = "특정 사용자의 최근 활동 로그를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/users/{userId}/activity-logs")
    public ResponseEntity<List<ActivityLogResponse>> getUserActivityLogs(@PathVariable Long userId) {
        try {
            List<ActivityLogResponse> logs = activityLogService.getLogsByUserId(userId, 20); // 최근 20개
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("사용자 활동 로그 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.status(500).build();
        }
    }

    // 코스 목록에서 섹션 조회
    @Operation(summary = "강의 섹션 목록 조회", description = "특정 강의의 섹션 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없습니다.")
    })
    @GetMapping("/courses/{courseId}/sections")
    public List<SectionResponse> getSections(@PathVariable Long courseId) {
        return sectionService.getSectionsByCourseId(courseId);
    }

    // 강의 공개/비공개 상태 변경
    @Operation(summary = "강의 공개/비공개 상태 변경", description = "강의의 공개/비공개 상태를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    })
    @PutMapping("/courses/{courseId}/publish")
    public ResponseEntity<?> updateCoursePublishStatus(
            @PathVariable Long courseId,
            @RequestBody CourseStatusUpdateRequest request) {
        try {
            courseListService.updateCoursePublishStatus(courseId, request.getIsPublished());
            return ResponseEntity.ok(Map.of("message", "강의 상태가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 게시글 상태 변경 (차단/해제)
    @Operation(summary = "게시글 상태 변경", description = "게시글의 상태를 변경합니다 (차단/해제).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PutMapping("/post/{postId}/status")
    public ResponseEntity<?> updatePostStatus(
            @PathVariable Long postId,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "상태 값이 필요합니다."));
            }
            postService.changePostStatus(postId, status);
            return ResponseEntity.ok(Map.of("message", "게시글 상태가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 활성화 상태인 전체 회원 수 카운트(자유게시판 활성 회원에 사용을 위함_관리자 제외)
    @GetMapping("/users/count")
    public ResponseEntity<Long> getActiveUserCount(){
        long count = userListService.getActiveUserCount();
        return ResponseEntity.ok(count);

    }

    // 차단된 사용자 조회
    @Operation(summary = "차단된 사용자 조회", description = "차단된 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/blocked")
    public List<UserProfileResponse> getBlockedUsers() {
        return userListService.getBlockedUsers();
    }

    // 차단 사용자 통계 조회
    @Operation(summary = "차단 사용자 통계 조회", description = "차단 사용자 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/blocked/statistics")
    public ResponseEntity<UserListService.BlockedUserStatistics> getBlockedUserStatistics() {
        UserListService.BlockedUserStatistics statistics = userListService.getBlockedUserStatistics();
        return ResponseEntity.ok(statistics);
    }

    // 차단된 강의 조회
    @Operation(summary = "차단된 강의 조회", description = "차단된 강의 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/courses/blocked")
    public List<CourseListResponse> getBlockedCourses() {
        return courseListService.getBlockedCourses();
    }

    // 차단된 게시글 조회
    @Operation(summary = "차단된 게시글 조회", description = "차단된 게시글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/posts/blocked")
    public List<PostResponse> getBlockedPosts() {
        return postService.getBlockedPosts();
    }

    // 차단된 댓글 조회
    @Operation(summary = "차단된 댓글 조회", description = "차단된 댓글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/comments/blocked")
    public List<CommentResponse> getBlockedComments() {
        return commentService.getBlockedComments();
    }
}
