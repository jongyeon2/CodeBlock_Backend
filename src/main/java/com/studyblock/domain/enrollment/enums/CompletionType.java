package com.studyblock.domain.enrollment.enums;

/**
 * 강의 완료 타입
 * How a lecture was marked as completed
 */
public enum CompletionType {
    /**
     * 비디오 시청으로 완료
     * Completed by watching video to 90%+ or end
     */
    VIDEO_WATCHED("비디오 시청", "Completed by watching video"),

    /**
     * 퀴즈 통과로 완료
     * Completed by passing quiz
     */
    QUIZ_PASSED("퀴즈 통과", "Completed by passing quiz"),

    /**
     * 수동으로 완료 처리
     * Manually marked as complete by user
     */
    MANUAL("수동 완료", "Manually marked complete"),

    /**
     * 자동으로 완료 처리 (시스템 로직)
     * Automatically marked complete by system
     */
    AUTO("자동 완료", "Automatically marked complete");

    private final String koreanName;
    private final String description;

    CompletionType(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 사용자 행동으로 완료되었는지 확인
     * Check if completed through user action
     */
    public boolean isUserAction() {
        return this == VIDEO_WATCHED || this == QUIZ_PASSED || this == MANUAL;
    }

    /**
     * 자동 완료인지 확인
     * Check if automatically completed
     */
    public boolean isAutomatic() {
        return this == AUTO;
    }
}
