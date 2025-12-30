package com.studyblock.domain.course.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비디오 진도율 저장 요청 DTO
 */
@Getter
@NoArgsConstructor
public class VideoProgressRequest {

    private Double position;  // 시청 위치 (초, 소수 포함)
    private Double duration;  // 전체 길이 (초, 소수 포함)

    public Integer getNormalizedPosition() {
        return normalizeToSeconds(position);
    }

    public Integer getNormalizedDuration() {
        return normalizeToSeconds(duration);
    }

    private Integer normalizeToSeconds(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }

        long rounded = Math.round(value);
        if (rounded < 0) {
            return 0;
        }

        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) rounded;
    }
}