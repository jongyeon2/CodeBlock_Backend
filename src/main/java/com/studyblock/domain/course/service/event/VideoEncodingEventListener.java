package com.studyblock.domain.course.service.event;

import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.event.VideoUploadedEvent;
import com.studyblock.domain.course.repository.VideoRepository;
import com.studyblock.domain.course.service.GenericVideoEncodingService;
import com.studyblock.domain.course.service.VideoEncodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 비디오 업로드 이벤트 리스너
 *
 * 역할:
 * - VideoUploadedEvent를 수신하여 비디오 인코딩 프로세스 시작
 * - 트랜잭션 커밋 후 실행을 보장하여 데이터 일관성 유지
 *
 * 왜 별도 클래스로 분리했는가?
 * - 관심사 분리(SoC): 업로드 로직 vs 인코딩 시작 로직
 * - 테스트 용이성: 이벤트 발행과 처리를 독립적으로 테스트 가능
 * - 확장성: 다른 리스너 추가 용이 (예: 알림, 로깅, 통계)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoEncodingEventListener {

    // 기존 VideoEncodingService (하위 호환성 유지)
    private final VideoEncodingService videoEncodingService;
    
    // 제네릭 인코딩 서비스 (신규 - 재사용 가능)
    private final GenericVideoEncodingService genericVideoEncodingService;
    
    private final VideoRepository videoRepository;

    @Value("${video.encoding.enabled:false}")
    private boolean encodingEnabled;
    
    @Value("${video.encoding.use-generic:false}")
    private boolean useGenericService;

    /**
     * 비디오 업로드 완료 이벤트 처리
     *
     * @TransactionalEventListener 핵심 개념:
     *
     * phase = TransactionPhase.AFTER_COMMIT
     * → 이벤트를 발행한 트랜잭션이 완전히 커밋된 "후"에 실행
     * → Video 엔티티가 DB에 실제로 저장된 상태를 보장
     * → 트랜잭션 타이밍 문제 해결!
     *
     * 실행 흐름:
     * 1. VideoService.uploadVideo() 실행 (트랜잭션 A)
     * 2. Video 엔티티 저장
     * 3. VideoUploadedEvent 발행 (메모리에만 보관)
     * 4. uploadVideo() 메서드 종료
     * 5. 트랜잭션 A 커밋 ← DB에 실제 저장!
     * 6. 🎯 이 메서드 실행 (트랜잭션 완료 후)
     * 7. videoEncodingService.startEncodingAsync() 호출
     *
     * fallbackExecution = false (기본값)
     * → 트랜잭션이 없는 경우 이벤트를 처리하지 않음
     * → 안전성 보장 (데이터가 저장된 경우에만 인코딩 시작)
     *
     * @param event 비디오 업로드 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVideoUploaded(VideoUploadedEvent event) {
        log.info("🎧 VideoUploadedEvent 수신 - Video ID: {}, Timestamp: {}",
                event.getVideoId(), event.getTimestamp());

        // 인코딩 활성화 여부 확인
        if (!encodingEnabled) {
            log.info("⏸️  비디오 인코딩 비활성화 - Video ID: {}, enabled: {}",
                    event.getVideoId(), encodingEnabled);
            return;
        }

        // 제네릭 서비스 사용 여부에 따라 분기
        if (useGenericService) {
            try {
                // 제네릭 서비스 사용 (Video와 PreviewVideo 공통 로직)
                log.info("🚀 비디오 인코딩 시작 요청 (Generic) - Video ID: {}", event.getVideoId());
                Video video = videoRepository.findById(event.getVideoId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "비디오를 찾을 수 없습니다. ID: " + event.getVideoId()));
                
                genericVideoEncodingService.performEncoding(
                        video,
                        video.getLecture().getId(),
                        "video"
                );
            } catch (Exception e) {
                log.error("비디오 인코딩 실패 (Generic) - Video ID: {}", event.getVideoId(), e);
                videoRepository.findById(event.getVideoId()).ifPresent(video -> {
                    video.failEncoding();
                    videoRepository.save(video);
                });
            }
        } else {
            // 기존 VideoEncodingService 사용 (하위 호환성)
            log.info("🚀 비디오 인코딩 시작 요청 (Legacy) - Video ID: {}", event.getVideoId());
            videoEncodingService.startEncodingAsync(event.getVideoId());
        }
    }

    /**
     * 참고: 다른 이벤트 리스너 추가 예시
     *
     * 같은 이벤트를 여러 리스너가 처리할 수 있음 (확장성)
     */

    // 예시 1: 업로드 완료 알림 전송
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void sendUploadNotification(VideoUploadedEvent event) {
    //     notificationService.sendVideoUploadedNotification(event.getVideoId());
    // }

    // 예시 2: 업로드 통계 기록
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void recordUploadStatistics(VideoUploadedEvent event) {
    //     statisticsService.recordVideoUpload(event.getVideoId());
    // }
}