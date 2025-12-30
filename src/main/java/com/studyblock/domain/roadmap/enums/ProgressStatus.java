package com.studyblock.domain.roadmap.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgressStatus {
    NOT_STARTED("시작 전"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료");

    private final String description;
}
