package com.studyblock.domain.activitylog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum ActionType {

    SIGNUP("회원가입"),
    COOKIE_CHARGE("쿠키 충전"),
    COURSE_PURCHASE("강의 구매"),
    REPORT("신고"),
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    COURSE_REVIEW("리뷰 작성");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
