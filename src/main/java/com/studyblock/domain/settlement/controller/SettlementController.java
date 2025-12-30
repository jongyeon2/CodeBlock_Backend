package com.studyblock.domain.settlement.controller;

import com.studyblock.domain.settlement.dto.CourseStatisticsResponse;
import com.studyblock.domain.settlement.dto.SettlementDashboardResponse;
import com.studyblock.domain.settlement.dto.SettlementLedgerResponse;
import com.studyblock.domain.settlement.dto.SettlementSummaryResponse;
import com.studyblock.domain.settlement.service.SettlementService;
import com.studyblock.global.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

// 정산 레코드 관리 API
@Slf4j
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // ========================================
    // 정산 레코드 조회
    // ========================================

    // 강사별 정산 내역 조회 (페이징 + 필터링 지원)
    @GetMapping("/ledgers/instructor/{instructorId}")
    @PreAuthorize("hasRole('ADMIN') or #instructorId == authentication.principal.id")
    public ResponseEntity<CommonResponse<Page<SettlementLedgerResponse>>> getInstructorSettlements(
            @PathVariable Long instructorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status) { // "PENDING", "ELIGIBLE", "SETTLED"
        try {
            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // 필터링 파라미터가 있으면 필터링된 조회, 없으면 기본 조회
            Page<SettlementLedgerResponse> responses;
            if (startDate != null || endDate != null || status != null) {
                responses = settlementService.getInstructorSettlementsWithFilters(
                    instructorId, startDate, endDate, status, pageable
                );
            } else {
                responses = settlementService.getInstructorSettlements(instructorId, pageable);
            }

            return ResponseEntity.ok(CommonResponse.success(
                    "정산 내역을 조회했습니다",
                    responses
            ));
        } catch (Exception e) {
            log.error("정산 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 내역 조회 중 오류가 발생했습니다"));
        }
    }

    // 강사별 정산 요약 정보 조회
    @GetMapping("/summary/instructor/{instructorId}")
    @PreAuthorize("hasRole('ADMIN') or #instructorId == authentication.principal.id")
    public ResponseEntity<CommonResponse<SettlementSummaryResponse>> getInstructorSummary(
            @PathVariable Long instructorId) {
        try {
            SettlementSummaryResponse summary = settlementService.getInstructorSummary(instructorId);
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 요약 정보를 조회했습니다",
                    summary
            ));
        } catch (Exception e) {
            log.error("정산 요약 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 요약 조회 중 오류가 발생했습니다"));
        }
    }

    // 강사별 강의별 통계 조회
    @GetMapping("/statistics/instructor/{instructorId}/by-course")
    @PreAuthorize("hasRole('ADMIN') or #instructorId == authentication.principal.id")
    public ResponseEntity<CommonResponse<java.util.List<CourseStatisticsResponse>>> getCourseStatistics(
            @PathVariable Long instructorId) {
        try {
            java.util.List<CourseStatisticsResponse> statistics = settlementService.getCourseStatisticsByInstructor(instructorId);
            return ResponseEntity.ok(CommonResponse.success(
                    "강의별 통계를 조회했습니다",
                    statistics
            ));
        } catch (Exception e) {
            log.error("강의별 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("강의별 통계 조회 중 오류가 발생했습니다"));
        }
    }

    // 전체 정산 대시보드 조회 (관리자 전용)
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementDashboardResponse>> getDashboard() {
        try {
            SettlementDashboardResponse dashboard = settlementService.getDashboard();
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 대시보드를 조회했습니다",
                    dashboard
            ));
        } catch (Exception e) {
            log.error("정산 대시보드 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 대시보드 조회 중 오류가 발생했습니다"));
        }
    }

    // ========================================
    // 정산 실행
    // ========================================

    // 강사별 정산 실행 (관리자 전용)
    @PostMapping("/settle/instructor/{instructorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<Integer>> settleForInstructor(
            @PathVariable Long instructorId) {
        try {
            int count = settlementService.settleForInstructor(instructorId);
            return ResponseEntity.ok(CommonResponse.success(
                    "정산이 완료되었습니다",
                    count
            ));
        } catch (Exception e) {
            log.error("정산 실행 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 실행 중 오류가 발생했습니다"));
        }
    }

    // 전체 정산 실행 (관리자 전용)
    @PostMapping("/settle/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<Integer>> settleAll() {
        try {
            int count = settlementService.settleAll();
            return ResponseEntity.ok(CommonResponse.success(
                    "전체 정산이 완료되었습니다",
                    count
            ));
        } catch (Exception e) {
            log.error("전체 정산 실행 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("전체 정산 실행 중 오류가 발생했습니다"));
        }
    }

    // 정산 가능 상태로 변경 (스케줄러용, 관리자 전용)
    @PostMapping("/mark-eligible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<Integer>> markEligibleForSettlement() {
        try {
            int count = settlementService.markEligibleForSettlement();
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 가능 상태로 변경되었습니다",
                    count
            ));
        } catch (Exception e) {
            log.error("정산 가능 상태 변경 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 가능 상태 변경 중 오류가 발생했습니다"));
        }
    }
}

