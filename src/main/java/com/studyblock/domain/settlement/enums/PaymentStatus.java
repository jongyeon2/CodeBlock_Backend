package com.studyblock.domain.settlement.enums;

//정산 지급 상태
public enum PaymentStatus {
    PENDING("지급 대기"),
    COMPLETED("지급 완료"),
    FAILED("지급 실패"),
    CANCELLED("지급 취소");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
