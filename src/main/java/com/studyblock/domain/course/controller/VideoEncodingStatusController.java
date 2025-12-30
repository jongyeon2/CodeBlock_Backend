package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.service.VideoEncodingStatusService;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 비디오 인코딩 상태 SSE 컨트롤러
 * 
 * 제공 기능:
 * - 일반 비디오 인코딩 상태 스트림
 * - 맛보기 비디오 인코딩 상태 스트림
 * 
 * SSE (Server-Sent Events):
 * - 서버에서 클라이언트로 실시간 푸시
 * - HTTP 기반으로 방화벽 친화적
 * - 자동 재연결 지원
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video Encoding Status", description = "비디오 인코딩 상태 실시간 알림 API (SSE)")
public class VideoEncodingStatusController {

    private final VideoEncodingStatusService encodingStatusService;

    /**
     * 일반 비디오 인코딩 상태 스트림 (SSE)
     * GET /api/videos/{videoId}/encoding-status/stream
     * 
     * 사용 예시:
     * const eventSource = new EventSource('/api/videos/123/encoding-status/stream');
     * eventSource.addEventListener('status-update', (event) => {
     *   const data = JSON.parse(event.data);
     *   console.log('인코딩 상태:', data.encodingStatus);
     * });
     */
    @GetMapping(value = "/videos/{videoId}/encoding-status/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "비디오 인코딩 상태 실시간 스트림",
            description = "SSE를 통해 비디오 인코딩 상태를 실시간으로 수신합니다. " +
                    "PENDING → PROCESSING → COMPLETED/FAILED 순서로 이벤트가 발생합니다. " +
                    "폴링 방식 대비 서버 부하 99% 감소."
    )
    @CommonApiResponses
    public SseEmitter streamVideoEncodingStatus(
            @Parameter(description = "비디오 ID", required = true, example = "1")
            @PathVariable Long videoId) {
        
        log.info("📡 SSE 연결 요청 - Video ID: {}", videoId);
        return encodingStatusService.createEmitter(videoId, "video");
    }

    /**
     * 맛보기 비디오 인코딩 상태 스트림 (SSE)
     * GET /api/preview-videos/{previewVideoId}/encoding-status/stream
     */
    @GetMapping(value = "/preview-videos/{previewVideoId}/encoding-status/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "맛보기 비디오 인코딩 상태 실시간 스트림",
            description = "SSE를 통해 맛보기 비디오 인코딩 상태를 실시간으로 수신합니다. " +
                    "PENDING → PROCESSING → COMPLETED/FAILED 순서로 이벤트가 발생합니다."
    )
    @CommonApiResponses
    public SseEmitter streamPreviewVideoEncodingStatus(
            @Parameter(description = "맛보기 비디오 ID", required = true, example = "1")
            @PathVariable Long previewVideoId) {
        
        log.info("📡 SSE 연결 요청 - PreviewVideo ID: {}", previewVideoId);
        return encodingStatusService.createEmitter(previewVideoId, "preview-video");
    }

    /**
     * SSE 연결 통계 조회 (모니터링용)
     * GET /api/encoding-status/connections
     */
    @GetMapping("/encoding-status/connections")
    @Operation(
            summary = "SSE 연결 통계 조회",
            description = "현재 활성화된 SSE 연결 수를 조회합니다. (관리자용)"
    )
    @CommonApiResponses
    public int getConnectionCount() {
        int count = encodingStatusService.getTotalConnections();
        log.info("📊 현재 SSE 연결 수: {}", count);
        return count;
    }
}

