package com.studyblock.domain.report.controller;

import com.studyblock.domain.report.dto.ReportCreateRequest;
import com.studyblock.domain.report.dto.ReportResponse;
import com.studyblock.domain.report.enums.ReportStatus;
import com.studyblock.domain.report.service.ReportService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "신고", description = "신고 관련 API")
public class ReportController {

    private final ReportService reportService;
    private final AuthenticationUtils authenticationUtils;

    //신고 생성 POST /api/reports
    @PostMapping
    @Operation(summary = "신고 생성",description = "게시글, 댓글, 유저, 동영상, 강의에 대한 신고를 생성합니다.")
    @ApiResponse(responseCode = "201", description = "신고 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 신고 등)")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportResponse>> createReport(
            @Valid @RequestBody ReportCreateRequest request,
            Authentication authentication) {

        try {
            // 사용자 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            ReportResponse response = reportService.createReport(userId, request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(CommonResponse.success("신고가 접수되었습니다", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("신고 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 처리 중 오류가 발생했습니다"));
        }
    }

    //내가 신고한 내역 조회 GET /api/reports/my
    @GetMapping("/my")
    @Operation(summary = "내가 신고한 내역 조회", description = "본인이 신고한 내역을 조회합니다. 페이징을 지원합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Page<ReportResponse>>> getMyReports(
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 필드", example = "reportedAt")
            @RequestParam(defaultValue = "reportedAt") String sortBy,
            @Parameter(description = "정렬 방향 (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "신고 상태 필터 (선택)", example = "PENDING")
            @RequestParam(required = false) ReportStatus status) {

        try {
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ReportResponse> reports = reportService.getMyReports(userId, status, pageable);
            
            return ResponseEntity.ok(CommonResponse.success("신고 내역 조회 성공", reports));
        } catch (Exception e) {
            log.error("신고 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 내역 조회 중 오류가 발생했습니다"));
        }
    }

    //신고 상세 조회 GET /api/reports/{reportId}
    @GetMapping("/{reportId}")
    @Operation(summary = "신고 상세 조회", description = "본인이 신고한 신고의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (본인이 신고한 것만 조회 가능)")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ReportResponse>> getReport(
            @Parameter(description = "신고 ID", required = true, example = "1")
            @PathVariable Long reportId,
            Authentication authentication) {

        try {
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            ReportResponse response = reportService.getReport(reportId, userId);
            return ResponseEntity.ok(CommonResponse.success("신고 상세 조회 성공", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("신고 상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("신고 상세 조회 중 오류가 발생했습니다"));
        }
    }
}
