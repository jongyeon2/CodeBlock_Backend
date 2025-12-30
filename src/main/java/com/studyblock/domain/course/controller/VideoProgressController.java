package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.VideoProgressRequest;
import com.studyblock.domain.course.dto.VideoProgressResponse;
import com.studyblock.domain.course.service.VideoProgressService;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 비디오 진도율 Controller
 * - 진도율 조회, 저장, 영구 저장
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoProgressController {

    private final VideoProgressService videoProgressService;

    /**
     * 진도율 조회
     * GET /api/videos/{videoId}/progress
     *
     * @param videoId 비디오 ID
     * @param user    현재 로그인한 사용자 (JWT에서 추출)
     * @return 진도율 응답 DTO
     */
    @GetMapping("/{videoId}/progress")
    public ResponseEntity<VideoProgressResponse> getProgress(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User user
    ) {
        // 인증 확인
        if (user == null) {
            log.warn("진도율 조회 실패 - 인증되지 않은 사용자 - videoId: {}", videoId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("진도율 조회 요청 - userId: {}, videoId: {}", user.getId(), videoId);

        VideoProgressResponse response = VideoProgressResponse.from(
                videoProgressService.getProgress(user.getId(), videoId)
        );

        if (response == null) {
            log.info("진도율 없음 - userId: {}, videoId: {}", user.getId(), videoId);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 진도율 저장 (Redis에만 저장)
     * POST /api/videos/{videoId}/progress
     *
     * @param videoId 비디오 ID
     * @param request 진도율 요청 (position, duration)
     * @param user    현재 로그인한 사용자
     * @return 204 No Content
     */
    @PostMapping("/{videoId}/progress")
    public ResponseEntity<Void> saveProgress(
            @PathVariable Long videoId,
            @RequestBody VideoProgressRequest request,
            @AuthenticationPrincipal User user
    ) {
        // 인증 확인
        if (user == null) {
            log.warn("진도율 저장 실패 - 인증되지 않은 사용자 - videoId: {}", videoId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request == null) {
            log.warn("진도율 저장 실패 - 요청 본문 없음 - userId: {}, videoId: {}", user.getId(), videoId);
            return ResponseEntity.badRequest().build();
        }

        Integer position = request.getNormalizedPosition();
        Integer duration = request.getNormalizedDuration();

        if (position == null || duration == null || duration <= 0) {
            log.warn("진도율 저장 실패 - 잘못된 데이터 - userId: {}, videoId: {}, position: {}, duration: {}",
                    user.getId(), videoId, request.getPosition(), request.getDuration());
            return ResponseEntity.badRequest().build();
        }

        log.info("진도율 저장 요청 - userId: {}, videoId: {}, position: {}, duration: {}",
                user.getId(), videoId, position, duration);

        videoProgressService.saveProgressToRedis(
                user.getId(),
                videoId,
                position,
                duration
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * 진도율 영구 저장 (Redis → MySQL)
     * POST /api/videos/{videoId}/progress/persist
     * - 영상 종료 시 호출
     * - 페이지 이탈 시 호출 (beforeunload)
     *
     * @param videoId 비디오 ID
     * @param user    현재 로그인한 사용자
     * @return 204 No Content
     */
    @PostMapping("/{videoId}/progress/persist")
    public ResponseEntity<Void> persistProgress(
            @PathVariable Long videoId,
            @RequestBody(required = false) VideoProgressRequest request,
            @AuthenticationPrincipal User user
    ) {
        // 인증 확인
        if (user == null) {
            log.warn("진도율 영구 저장 실패 - 인증되지 않은 사용자 - videoId: {}", videoId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("진도율 영구 저장 요청 - userId: {}, videoId: {}", user.getId(), videoId);

        Integer position = null;
        Integer duration = null;

        if (request != null) {
            position = request.getNormalizedPosition();
            duration = request.getNormalizedDuration();

            if (position == null || duration == null || duration <= 0) {
                log.warn("진도율 영구 저장 실패 - 잘못된 데이터 - userId: {}, videoId: {}, position: {}, duration: {}",
                        user.getId(), videoId,
                        request.getPosition(), request.getDuration());
                return ResponseEntity.badRequest().build();
            }
        }

        videoProgressService.persistToDatabase(user.getId(), videoId, position, duration);

        return ResponseEntity.noContent().build();
    }
}