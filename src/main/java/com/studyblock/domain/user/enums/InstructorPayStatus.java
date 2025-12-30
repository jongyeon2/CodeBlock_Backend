package com.studyblock.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InstructorPayStatus {
    ACTIVE("활성"),
    SUSPENDED("정지"),
    READY("준비중");

    private final String description;
}
