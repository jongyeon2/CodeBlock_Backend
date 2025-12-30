package com.studyblock.domain.course.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 비디오 업로드 완료 도메인 이벤트
 *
 * 발행 시점: Video 엔티티가 DB에 저장되고 트랜잭션이 커밋된 직후
 * 용도: 비디오 인코딩 프로세스를 비동기로 시작하기 위한 트리거
 *
 * DDD 원칙:
 * - 불변(Immutable) 객체 - 이벤트는 과거에 발생한 사실이므로 변경 불가
 * - 도메인 언어 사용 - "VideoUploaded"는 비즈니스 도메인의 중요한 사건
 */
@Getter
@RequiredArgsConstructor
public class VideoUploadedEvent {

    /**
     * 업로드된 비디오 ID
     *
     * 이벤트는 최소한의 정보만 전달 (ID만 전달, 엔티티 전체를 전달하지 않음)
     * 이유:
     * 1. 느슨한 결합(Loose Coupling) - 수신자가 필요한 정보만 조회
     * 2. 트랜잭션 독립성 - 수신자가 별도 트랜잭션에서 최신 데이터 조회
     * 3. 메모리 효율 - 큰 객체 복사 방지
     */
    private final Long videoId;

    /**
     * 이벤트 발생 시각 (선택사항)
     *
     * 로깅, 디버깅, 이벤트 순서 추적 등에 활용 가능
     */
    private final long timestamp = System.currentTimeMillis();
}