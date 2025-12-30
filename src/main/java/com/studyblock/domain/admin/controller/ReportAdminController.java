package com.studyblock.domain.admin.controller;

import com.studyblock.domain.report.dto.ReportResponse;
import com.studyblock.domain.report.dto.ReportStatisticsResponse;
import com.studyblock.domain.report.enums.ReportStatus;
import com.studyblock.domain.report.enums.ReportTargetType;
import com.studyblock.domain.report.service.ReportService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "관리자 - 신고 관리", description = "관리자용 신고 관리 API")
public class ReportAdminController {

    private final ReportService reportService;

    //전체 신고 목록 조회 GET /api/admin/reports
    @GetMapping
    @Operation(summary = "전체 신고 목록 조회", description = "전체 신고 목록을 조회합니다. 상태, 타입별 필터링이 가능합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Page<ReportResponse>>> getAllReports(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 필드", example = "reportedAt")
            @RequestParam(defaultValue = "reportedAt") String sortBy,
            @Parameter(description = "정렬 방향 (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "신고 상태 필터 (선택)", example = "PENDING")
            @RequestParam(required = false) ReportStatus status,
            @Parameter(description = "신고 타입 필터 (선택)", example = "POST")
            @RequestParam(required = false) ReportTargetType targetType) {

        try {
            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ReportResponse> reports = reportService.getAllReports(status, targetType, pageable);
            return ResponseEntity.ok(CommonResponse.success("신고 목록 조회 성공", reports));

        } catch (Exception e) {
            log.error("신고 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 목록 조회 중 오류가 발생했습니다"));
        }
    }

    //신고 상세 조회 GET /api/admin/reports/{reportId}
    @GetMapping("/{reportId}")
    @Operation(summary = "신고 상세 조회",description = "신고의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportResponse>> getReport(
            @Parameter(description = "신고 ID", required = true, example = "1")
            @PathVariable Long reportId) {
        try {
            ReportResponse response = reportService.getReportForAdmin(reportId);
            return ResponseEntity.ok(CommonResponse.success("신고 상세 조회 성공", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("신고 상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 상세 조회 중 오류가 발생했습니다"));
        }
    }

    //신고 검토 시작 PUT /api/admin/reports/{reportId}/review
    @PutMapping("/{reportId}/review")
    @Operation(summary = "신고 검토 시작", description = "신고 상태를 PENDING에서 REVIEWING으로 변경합니다.")
    @ApiResponse(responseCode = "200", description = "검토 시작 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportResponse>> startReview(
            @Parameter(description = "신고 ID", required = true, example = "1")
            @PathVariable Long reportId) {

        try {
            ReportResponse response = reportService.startReview(reportId);
            return ResponseEntity.ok(CommonResponse.success("신고 검토가 시작되었습니다", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("신고 검토 시작 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 검토 시작 중 오류가 발생했습니다"));
        }
    }

    //신고 처리 완료 PUT /api/admin/reports/{reportId}/resolve
    @PutMapping("/{reportId}/resolve")
    @Operation(summary = "신고 처리 완료", description = "신고 상태를 REVIEWING에서 RESOLVED로 변경합니다.")
    @ApiResponse(responseCode = "200", description = "처리 완료 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportResponse>> resolveReport(
            @Parameter(description = "신고 ID", required = true, example = "1")
            @PathVariable Long reportId) {

        try {
            ReportResponse response = reportService.resolveReport(reportId);
            return ResponseEntity.ok(CommonResponse.success("신고 처리가 완료되었습니다", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("신고 처리 완료 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 처리 완료 중 오류가 발생했습니다"));
        }
    }

    //신고 거절 PUT /api/admin/reports/{reportId}/reject
    @PutMapping("/{reportId}/reject")
    @Operation(summary = "신고 거절", description = "신고 상태를 REVIEWING에서 REJECTED로 변경합니다.")
    @ApiResponse(responseCode = "200", description = "거절 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportResponse>> rejectReport(
            @Parameter(description = "신고 ID", required = true, example = "1")
            @PathVariable Long reportId) {

        try {
            ReportResponse response = reportService.rejectReport(reportId);
            return ResponseEntity.ok(CommonResponse.success("신고가 거절되었습니다", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("신고 거절 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 거절 중 오류가 발생했습니다"));
        }
    }

    //신고 통계 조회 GET /api/admin/reports/statistics
    @GetMapping("/statistics")
    @Operation(summary = "신고 통계 조회", description = "신고 통계 정보를 조회합니다. 상태별, 타입별 건수를 제공합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportStatisticsResponse>> getStatistics() {
        try {
            ReportStatisticsResponse response = reportService.getStatistics();
            return ResponseEntity.ok(CommonResponse.success("신고 통계 조회 성공", response));
        } catch (Exception e) {
            log.error("신고 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 통계 조회 중 오류가 발생했습니다"));
        }
    }
}

