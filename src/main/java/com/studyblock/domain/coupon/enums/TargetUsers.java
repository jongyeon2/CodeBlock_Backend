package com.studyblock.domain.coupon.enums;

//쿠폰 발급 대상 사용자
public enum TargetUsers {
    ALL("전체 사용자"),
    NEW("신규 사용자"),
    EXISTING("기존 사용자"),
    SPECIFIC("특정 사용자");

    private final String description;

    TargetUsers(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
