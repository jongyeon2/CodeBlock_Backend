package com.studyblock.domain.course.exception;

import com.studyblock.domain.course.enums.EncodingStatus;

/**
 * 비디오 인코딩이 완료되지 않았을 때 발생하는 예외
 */
public class VideoEncodingNotCompletedException extends RuntimeException {

    private final Long videoId;
    private final EncodingStatus currentStatus;

    public VideoEncodingNotCompletedException(Long videoId, EncodingStatus currentStatus) {
        super(String.format("비디오 인코딩이 완료되지 않았습니다. Video ID: %d, 현재 상태: %s",
                videoId, currentStatus));
        this.videoId = videoId;
        this.currentStatus = currentStatus;
    }

    public Long getVideoId() {
        return videoId;
    }

    public EncodingStatus getCurrentStatus() {
        return currentStatus;
    }
}