package com.studyblock.domain.community.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardType {
    GENERAL(0,  "일반게시판"),
    NOTICE(1, "공지사항"),
    FAQ(2, "FAQ");

    private final int value;
    private final String description;

    public static BoardType fromValue(int value) {
        for (BoardType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid BoardType value: " + value);
    }
}
