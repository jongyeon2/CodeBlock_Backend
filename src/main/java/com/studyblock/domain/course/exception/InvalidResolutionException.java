package com.studyblock.domain.course.exception;

/**
 * 유효하지 않은 해상도가 요청되었을 때 발생하는 예외
 */
public class InvalidResolutionException extends RuntimeException {

    private final String providedResolution;

    public InvalidResolutionException(String providedResolution) {
        super(String.format("유효하지 않은 해상도입니다: %s (허용: 1080p, 720p, 540p)", providedResolution));
        this.providedResolution = providedResolution;
    }

    public String getProvidedResolution() {
        return providedResolution;
    }
}