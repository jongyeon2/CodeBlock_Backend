package com.studyblock.domain.course.service.event;

import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.event.PreviewVideoUploadedEvent;
import com.studyblock.domain.course.repository.PreviewVideoRepository;
import com.studyblock.domain.course.service.GenericVideoEncodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 맛보기 비디오 업로드 이벤트 리스너
 *
 * 역할:
 * - PreviewVideoUploadedEvent를 수신하여 맛보기 비디오 인코딩 프로세스 시작
 * - 트랜잭션 커밋 후 실행을 보장하여 데이터 일관성 유지
 * - GenericVideoEncodingService를 활용하여 인코딩 로직 재사용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PreviewVideoEncodingEventListener {

    private final PreviewVideoRepository previewVideoRepository;
    private final GenericVideoEncodingService genericVideoEncodingService;

    @Value("${video.encoding.enabled:false}")
    private boolean encodingEnabled;

    /**
     * 맛보기 비디오 업로드 완료 이벤트 처리
     *
     * @TransactionalEventListener 핵심 개념:
     * - phase = TransactionPhase.AFTER_COMMIT
     * - 이벤트를 발행한 트랜잭션이 완전히 커밋된 "후"에 실행
     * - PreviewVideo 엔티티가 DB에 실제로 저장된 상태를 보장
     *
     * @param event 맛보기 비디오 업로드 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePreviewVideoUploaded(PreviewVideoUploadedEvent event) {
        log.info("🎧 PreviewVideoUploadedEvent 수신 - PreviewVideo ID: {}, Timestamp: {}",
                event.getPreviewVideoId(), event.getTimestamp());

        // 인코딩 활성화 여부 확인
        if (!encodingEnabled) {
            log.info("⏸️  맛보기 비디오 인코딩 비활성화 - PreviewVideo ID: {}, enabled: {}",
                    event.getPreviewVideoId(), encodingEnabled);
            return;
        }

        try {
            // PreviewVideo 엔티티 조회 (Lecture Fetch Join으로 N+1 문제 방지)
            PreviewVideo previewVideo = previewVideoRepository.findByIdWithLectureAndInstructor(event.getPreviewVideoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "맛보기 비디오를 찾을 수 없습니다. ID: " + event.getPreviewVideoId()));

            // 비동기 인코딩 시작 (제네릭 서비스 활용)
            log.info("🚀 맛보기 비디오 인코딩 시작 요청 - PreviewVideo ID: {}", event.getPreviewVideoId());
            genericVideoEncodingService.performEncoding(
                    previewVideo,
                    previewVideo.getLecture().getId(),
                    "preview-video"  // ✅ 단수형으로 수정 (SSE와 일치)
            );

        } catch (Exception e) {
            log.error("맛보기 비디오 인코딩 실패 - PreviewVideo ID: {}", event.getPreviewVideoId(), e);
            
            // 실패 상태로 변경
            previewVideoRepository.findById(event.getPreviewVideoId()).ifPresent(previewVideo -> {
                previewVideo.failEncoding();
                previewVideoRepository.save(previewVideo);
            });
        }
    }
}

