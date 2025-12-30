package com.studyblock.domain.course.service;

import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.repository.PreviewVideoRepository;
import com.studyblock.domain.course.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 비디오 인코딩 상태 SSE(Server-Sent Events) 서비스
 * 
 * 역할:
 * - 클라이언트의 SSE 연결 관리
 * - 인코딩 상태 변경 시 실시간 푸시
 * - Video와 PreviewVideo 모두 지원
 * 
 * 폴링 대비 장점:
 * - 서버 부하 99% 감소 (100회 요청 → 3회 이벤트)
 * - 즉시 알림 (3초 지연 → 0초)
 * - 네트워크 비용 절감
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEncodingStatusService {

    private final VideoRepository videoRepository;
    private final PreviewVideoRepository previewVideoRepository;

    // Video ID별 SSE Emitter 목록 (가급적 1개 유지; 중복 접속 시 기존 연결 정리 후 교체)
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * SSE Emitter 생성 및 등록
     *
     * @param videoId 비디오 ID
     * @param videoType 비디오 타입 ("video" 또는 "preview-video")
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long videoId, String videoType) {
        String key = getKey(videoId, videoType);
        // 2분 타임아웃 (과도한 장기 연결 방지)
        SseEmitter emitter = new SseEmitter(120_000L);

        // 연결 종료 또는 타임아웃 시 Emitter를 목록에서 제거하는 정리 로직
        Runnable cleanup = () -> {
            log.info("🧹 SSE 연결 정리 시작 - Key: {}", key);
            removeEmitter(key, emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.error("❌ SSE Emitter 오류 - Key: {}", key, e);
            cleanup.run();
        });

        try {
            // 1) 현재 상태 확인
            EncodingStatus current = getCurrentStatus(videoId, videoType);

            // 2) 종료 상태면 단발 이벤트 전송 후 즉시 종료, 등록하지 않음
            if (current == EncodingStatus.COMPLETED || current == EncodingStatus.FAILED) {
                sendInitialStatus(videoId, videoType, emitter);
                try {
                    emitter.complete();
                } catch (Exception ignore) {}
                log.info("⏹️ 종료 상태로 단발 전송 후 연결 종료 - Key: {}, Status: {}", key, current);
                return emitter;
            }

            // 3) 초기 상태 전송
            sendInitialStatus(videoId, videoType, emitter);

            // 4) 기존 동일 키 연결이 있으면 정리(단일 유지 정책)
            List<SseEmitter> existing = emitters.get(key);
            if (existing != null && !existing.isEmpty()) {
                existing.forEach(e -> {
                    try { e.complete(); } catch (Exception ignore) {}
                });
                emitters.remove(key);
                log.info("🔁 기존 Emitter 정리 후 재등록 - Key: {}", key);
            }

            // 5) 등록
            emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
            log.info("✅ SSE Emitter 등록 완료 - Key: {}. 현재 구독자: {}", key, emitters.get(key).size());

        } catch (Exception e) {
            // 초기 상태 전송 실패 시, 클라이언트에게 에러를 알리고 연결을 즉시 종료합니다.
            log.error("❌ 초기 상태 전송 및 Emitter 등록 실패. 연결을 종료합니다. - Key: {}", key, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 인코딩 상태 변경 알림 (모든 구독자에게 푸시) - 진행률 포함
     *
     * @param videoId 비디오 ID
     * @param videoType 비디오 타입 ("video" 또는 "preview-video")
     * @param status 변경된 인코딩 상태
     * @param progress 인코딩 진행률 (0-100, PROCESSING 상태에서만 유효)
     */
    public void notifyStatusChange(Long videoId, String videoType, EncodingStatus status, Integer progress) {
        String key = getKey(videoId, videoType);
        List<SseEmitter> emitterList = emitters.get(key);

        if (emitterList == null || emitterList.isEmpty()) {
            log.debug("📭 구독자 없음 - Key: {}", key);
            return;
        }

        log.info("📢 인코딩 상태 알림 - Key: {}, Status: {}, Progress: {}, 구독자: {}명",
                key, status, progress, emitterList.size());

        // 데이터 맵 생성
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("videoId", videoId);
        data.put("videoType", videoType);
        data.put("encodingStatus", status.toString());
        data.put("timestamp", System.currentTimeMillis());

        // 요구사항에 따라 progress 값 설정
        if (status == EncodingStatus.PENDING) {
            data.put("progress", 0);
        } else if (status == EncodingStatus.COMPLETED) {
            data.put("progress", 100);
        } else if (progress != null && status == EncodingStatus.PROCESSING) {
            data.put("progress", progress);
        }

        // 모든 구독자에게 이벤트 전송
        emitterList.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("status-update")
                        .data(data));
                log.debug("✉️ 이벤트 전송 성공 - Key: {}", key);
            } catch (IOException e) {
                log.error("❌ 이벤트 전송 실패 - Key: {}", key, e);
                removeEmitter(key, emitter);
            }
        });

        // 완료/실패 시 연결 정리
        if (status == EncodingStatus.COMPLETED || status == EncodingStatus.FAILED) {
            log.info("🧹 인코딩 종료로 연결 정리 - Key: {}", key);
            emitterList.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("Emitter 종료 실패", e);
                }
            });
            emitters.remove(key);
        }
    }

    /**
     * 인코딩 상태 변경 알림 (하위 호환성 유지)
     */
    public void notifyStatusChange(Long videoId, String videoType, EncodingStatus status) {
        notifyStatusChange(videoId, videoType, status, null);
    }

    /**
     * 연결 직후 현재 인코딩 상태를 조회하여 첫 이벤트("initial-status")를 전송합니다.
     * @throws IOException 이벤트 전송 실패 시 예외 발생
     */
    private void sendInitialStatus(Long videoId, String videoType, SseEmitter emitter) throws IOException {
        EncodingStatus currentStatus = getCurrentStatus(videoId, videoType);

        // ✅ DB에서 실제 진행률 조회 (모달 재오픈 시 복원)
        int progress = getCurrentProgress(videoId, videoType);

        emitter.send(SseEmitter.event()
                .name("initial-status")
                .data(Map.of(
                        "videoId", videoId,
                        "videoType", videoType,
                        "encodingStatus", currentStatus.toString(),
                        "progress", progress,
                        "timestamp", System.currentTimeMillis()
                )));

        log.info("📤 초기 상태 전송 완료 - Key: {}, Status: {}, Progress: {}",
                getKey(videoId, videoType), currentStatus, progress);
    }

    /**
     * 현재 인코딩 상태 조회
     */
    private EncodingStatus getCurrentStatus(Long videoId, String videoType) {
        if ("preview-video".equals(videoType)) {
            return previewVideoRepository.findById(videoId)
                    .map(PreviewVideo::getEncodingStatus)
                    .orElse(EncodingStatus.PENDING);
        } else {
            return videoRepository.findById(videoId)
                    .map(Video::getEncodingStatus)
                    .orElse(EncodingStatus.PENDING);
        }
    }

    /**
     * 현재 인코딩 진행률 조회 (DB에서)
     * - 모달 재오픈 시 정확한 진행률 복원
     * - 서버 재시작 후에도 진행률 유지
     */
    private int getCurrentProgress(Long videoId, String videoType) {
        if ("preview-video".equals(videoType)) {
            return previewVideoRepository.findById(videoId)
                    .map(PreviewVideo::getEncodingProgress)
                    .orElse(0);
        } else {
            return videoRepository.findById(videoId)
                    .map(Video::getEncodingProgress)
                    .orElse(0);
        }
    }

    /**
     * Emitter 제거
     */
    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> emitterList = emitters.get(key);
        if (emitterList != null) {
            emitterList.remove(emitter);
            log.debug("🗑️ Emitter 제거 - Key: {}, 남은 구독자: {}명", key, emitterList.size());
            
            if (emitterList.isEmpty()) {
                emitters.remove(key);
                log.info("🧹 빈 목록 제거 - Key: {}", key);
            }
        }
    }

    /**
     * Key 생성 (videoId + videoType)
     */
    private String getKey(Long videoId, String videoType) {
        return videoType + ":" + videoId;
    }

    /**
     * 모든 연결 수 조회 (모니터링용)
     */
    public int getTotalConnections() {
        return emitters.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}

