package com.studyblock.domain.course.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 맛보기 비디오 업로드 완료 도메인 이벤트
 *
 * 발행 시점: PreviewVideo 엔티티가 DB에 저장되고 트랜잭션이 커밋된 직후
 * 용도: 맛보기 비디오 인코딩 프로세스를 비동기로 시작하기 위한 트리거
 *
 * VideoUploadedEvent와 동일한 구조이지만, PreviewVideo 전용으로 분리하여
 * 향후 다른 비즈니스 로직 추가 시 유연성 확보
 */
@Getter
@RequiredArgsConstructor
public class PreviewVideoUploadedEvent {

    /**
     * 업로드된 맛보기 비디오 ID
     */
    private final Long previewVideoId;

    /**
     * 이벤트 발생 시각
     */
    private final long timestamp = System.currentTimeMillis();
}

