package com.studyblock.domain.activitylog.controller;

import com.studyblock.domain.activitylog.dto.ActivityLogResponse;
import com.studyblock.domain.activitylog.enums.ActionType;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
@Tag(name = "활동 로그", description = "활동 로그 조회 API")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    // 최근 활동 로그 조회
    @Operation(summary = "최근 활동 로그 조회")
    @GetMapping("/recent")
    public ResponseEntity<List<ActivityLogResponse>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit) {
        List<ActivityLogResponse> logs = activityLogService.getRecentLogs(limit);
        return ResponseEntity.ok(logs);
    }

    // 특정 사용자의 로그 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityLogResponse>> getUserLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByUserId(userId, limit);
        return ResponseEntity.ok(logs);
    }

    // 특정 액션 타입별 로그 조회
    @GetMapping("/action/{actionType}")
    public ResponseEntity<List<ActivityLogResponse>> getActionTypeLogs(
            @PathVariable ActionType actionType,
            @RequestParam(defaultValue = "50") int limit) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByActionType(actionType, limit);
        return ResponseEntity.ok(logs);
    }

    // 특정 사용자와 액션 타입 조회
    @GetMapping("/user/{userId}/action/{actionType}")
    public ResponseEntity<List<ActivityLogResponse>> getUserAndActionTypeLogs(
            @PathVariable Long userId,
            @PathVariable ActionType actionType,
            @RequestParam(defaultValue = "50") int limit) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByUserIdAndActionType(userId, actionType, limit);
        return ResponseEntity.ok(logs);
    }
}
