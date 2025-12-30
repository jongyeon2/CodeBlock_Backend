package com.studyblock.domain.enrollment.enums;

/**
 * 수강신청 상태
 * Enrollment status for course ownership
 */
public enum EnrollmentStatus {
    /**
     * 활성 - 현재 수강 중
     * Currently enrolled and has access to course content
     */
    ACTIVE("활성", "Currently enrolled"),

    /**
     * 완료 - 강좌를 완강함
     * Course has been completed (100% progress)
     */
    COMPLETED("완료", "Course completed"),

    /**
     * 취소됨 - 환불 또는 관리자에 의해 취소됨
     * Enrollment revoked (due to refund or admin action)
     */
    REVOKED("취소됨", "Enrollment revoked"),

    /**
     * 만료됨 - 기간 제한 강좌의 접근 기간이 만료됨
     * Time-limited access has expired
     */
    EXPIRED("만료됨", "Access expired");

    private final String koreanName;
    private final String description;

    EnrollmentStatus(String koreanName, String description) {
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
     * 수강 중인 상태인지 확인
     * Check if enrollment is currently active (can access content)
     */
    public boolean isAccessible() {
        return this == ACTIVE || this == COMPLETED;
    }

    /**
     * 진행 중인 수강신청인지 확인 (완료되지 않음)
     * Check if enrollment is in progress (not yet completed)
     */
    public boolean isInProgress() {
        return this == ACTIVE;
    }

    /**
     * 종료된 수강신청인지 확인 (취소 또는 만료)
     * Check if enrollment has been terminated
     */
    public boolean isTerminated() {
        return this == REVOKED || this == EXPIRED;
    }
}
