package com.studyblock.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    WITHDRAWN(0, "탈퇴"),
    ACTIVE(1, "활동"),
    DORMANT(2, "휴면"),
    SUSPENDED(3, "정지");

    private final int value;
    private final String description;

    public static UserStatus fromValue(int value) {
        for (UserStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid UserStatus value: " + value);
    }
}
