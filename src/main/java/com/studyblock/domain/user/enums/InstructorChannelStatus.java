package com.studyblock.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InstructorChannelStatus {
    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String description;
}
