package com.studyblock.domain.report.enums;

public enum ReportStatus {
    PENDING("대기"),
    REVIEWING("검토 중"),
    RESOLVED("처리 완료"),
    REJECTED("거절");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

