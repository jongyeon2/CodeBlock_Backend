package com.studyblock.domain.admin.controller;

import com.studyblock.domain.wallet.dto.CookieAdminStatsResponse;
import com.studyblock.domain.wallet.dto.CookieAdminUsageResponse;
import com.studyblock.domain.wallet.service.CookieAdminService;
import com.studyblock.global.dto.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자용 쿠키 관리 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cookies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 쿠키 관리", description = "관리자용 쿠키 통계 및 사용 내역 조회 API")
public class CookieAdminController {

    private final CookieAdminService cookieAdminService;

    /**
     * 전체 사용자 쿠키 잔액 통계 조회
     * GET /api/admin/cookies/stats
     */
    @Operation(summary = "쿠키 잔액 통계 조회", description = "전체 사용자의 쿠키 잔액 통계와 월별 충전/사용 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "조회 실패")
    })
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<CookieAdminStatsResponse>> getCookieStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            log.info("쿠키 통계 조회 요청 - year: {}, month: {}", year, month);
            
            CookieAdminStatsResponse stats = cookieAdminService.getCookieStats(year, month);
            
            log.info("쿠키 통계 조회 완료 - totalBalance: {}, monthlyCharged: {}, monthlyUsed: {}", 
                    stats.getTotalBalance(), stats.getMonthlyCharged(), stats.getMonthlyUsed());
            
            return ResponseEntity.ok(CommonResponse.success(
                    "쿠키 통계를 조회했습니다",
                    stats
            ));
            
        } catch (Exception e) {
            log.error("쿠키 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠키 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 월별 쿠키 충전/사용 통계 조회
     * GET /api/admin/cookies/monthly-stats
     */
    @Operation(summary = "월별 쿠키 통계 조회", description = "특정 월의 쿠키 충전/사용 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "조회 실패")
    })
    @GetMapping("/monthly-stats")
    public ResponseEntity<CommonResponse<CookieAdminStatsResponse>> getMonthlyStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        // getCookieStats와 동일한 로직 사용
        return getCookieStats(year, month);
    }

    /**
     * 쿠키 사용/충전 내역 조회 (필터링 포함)
     * GET /api/admin/cookies/usage-history
     */
    @Operation(summary = "쿠키 사용/충전 내역 조회", description = "쿠키 사용/충전 내역을 조회합니다. 사용자 ID, type(DEBIT/CHARGE), 날짜 범위, 페이징으로 필터링 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "조회 실패")
    })
    @GetMapping("/usage-history")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getCookieUsageHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("쿠키 사용 내역 조회 요청 - userId: {}, type: {}, startDate: {}, endDate: {}, page: {}, size: {}",
                    userId, type, startDate, endDate, page, size);

            // 페이징 설정
            Pageable pageable = PageRequest.of(page, size);

            // 쿠키 사용 내역 조회 (type 파라미터 사용)
            List<CookieAdminUsageResponse> usageHistory = cookieAdminService.getCookieUsageHistory(
                    userId, type, startDate, endDate, pageable);

            // 전체 개수 조회
            Long totalCount = cookieAdminService.countCookieUsageHistory(
                    userId, type, startDate, endDate);
            
            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("data", usageHistory);
            responseData.put("count", usageHistory.size());
            responseData.put("totalCount", totalCount);
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("totalPages", (int) Math.ceil((double) totalCount / size));
            
            log.info("쿠키 사용 내역 조회 완료 - count: {}, totalCount: {}", usageHistory.size(), totalCount);
            
            return ResponseEntity.ok(CommonResponse.success(
                    "쿠키 사용 내역을 조회했습니다",
                    responseData
            ));
            
        } catch (Exception e) {
            log.error("쿠키 사용 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠키 사용 내역 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

