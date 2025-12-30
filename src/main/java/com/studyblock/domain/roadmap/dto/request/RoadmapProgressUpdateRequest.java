package com.studyblock.domain.roadmap.dto.request;

import com.studyblock.domain.roadmap.enums.ProgressStatus;
import jakarta.validation.constraints.NotNull;

public record RoadmapProgressUpdateRequest(
        @NotNull(message = "진행 상태는 필수입니다.")
        ProgressStatus status
) {
}
