package com.studyblock.domain.course.entity;

import com.studyblock.domain.course.enums.EncodingStatus;

/**
 * Video와 PreviewVideo의 공통 인터페이스
 * - 비디오 리소스의 공통 동작을 정의
 * - VideoEncodingService에서 제네릭 타입으로 사용
 */
public interface VideoResource {
    
    Long getId();
    
    String getName();
    
    String getOriginalUrl();
    
    String getUrl1080p();
    
    String getUrl720p();
    
    String getUrl540p();
    
    String getThumbnailUrl();
    
    String getSubtitleUrl();
    
    String getResolution();
    
    Long getFileSize();
    
    Integer getDurationSeconds();

    EncodingStatus getEncodingStatus();

    Integer getEncodingProgress();

    // 인코딩 상태 변경 메서드
    void startEncoding();

    void completeEncoding();

    void failEncoding();

    // 인코딩 진행률 업데이트 메서드
    void updateEncodingProgress(Integer progress);

    // URL 업데이트 메서드
    void updateVideoUrls(String url1080p, String url720p, String url540p);
    
    // 인코딩 완료 여부 확인
    default boolean isEncodingCompleted() {
        return getEncodingStatus() == EncodingStatus.COMPLETED;
    }
    
    // 사용 가능한 최고 해상도 URL 조회 (우선순위: 720p -> 1080p -> 540p -> 원본)
    // 변경 사항: 720p가 기본 인코딩 해상도이므로 우선순위 최상위로 변경
    default String getAvailableVideoUrl() {
        if (getUrl720p() != null) {
            return getUrl720p();
        } else if (getUrl1080p() != null) {
            return getUrl1080p();
        } else if (getUrl540p() != null) {
            return getUrl540p();
        } else if (getOriginalUrl() != null) {
            return getOriginalUrl();
        } else {
            throw new IllegalStateException("사용 가능한 비디오 URL이 없습니다. ID: " + getId());
        }
    }
}

