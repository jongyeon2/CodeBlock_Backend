package com.studyblock.domain.settlement.controller;

import com.studyblock.domain.settlement.dto.SettlementHoldResponse;
import com.studyblock.domain.settlement.service.SettlementHoldService;
import com.studyblock.global.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/settlement/holds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SettlementHoldController {

    private final SettlementHoldService settlementHoldService;

    // 목록 조회 (필터: status, from, to, instructorId, orderNumber)
    @GetMapping
    public ResponseEntity<CommonResponse<List<SettlementHoldResponse>>> listHolds(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(required = false) String orderNumber
    ) {
        try {
            List<SettlementHoldResponse> data = settlementHoldService.listHolds(status, from, to, instructorId, orderNumber);
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 보류 목록을 조회했습니다",
                    data
            ));
        } catch (Exception e) {
            log.error("정산 보류 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 보류 목록 조회 중 오류가 발생했습니다"));
        }
    }

    // 상세 조회
    @GetMapping("/{holdId}")
    public ResponseEntity<CommonResponse<SettlementHoldResponse>> getHold(@PathVariable Long holdId) {
        try {
            SettlementHoldResponse data = settlementHoldService.getHold(holdId);
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 보류 상세를 조회했습니다",
                    data
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("정산 보류 상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 보류 상세 조회 중 오류가 발생했습니다"));
        }
    }

    // 보류 해제
    @PostMapping("/{holdId}/release")
    public ResponseEntity<CommonResponse<SettlementHoldResponse>> release(@PathVariable Long holdId) {
        try {
            SettlementHoldResponse data = settlementHoldService.releaseHold(holdId);
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 보류를 해제했습니다",
                    data
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("정산 보류 해제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 보류 해제 중 오류가 발생했습니다"));
        }
    }

    // 보류 취소
    @PostMapping("/{holdId}/cancel")
    public ResponseEntity<CommonResponse<SettlementHoldResponse>> cancel(@PathVariable Long holdId) {
        try {
            SettlementHoldResponse data = settlementHoldService.cancelHold(holdId);
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 보류를 취소했습니다",
                    data
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("정산 보류 취소 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 보류 취소 중 오류가 발생했습니다"));
        }
    }
}
