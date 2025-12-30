package com.studyblock.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis 저장용 VideoProgress DTO
 * - Serializable 구현 필수 (Redis 직렬화)
 * - 계산된 필드(getter 메서드)는 Redis에 저장하지 않음
 * - 알 수 없는 필드 무시 (이전 데이터 호환성)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoProgressDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long videoId;
    private Integer position;       // 시청 위치 (초)
    private Integer duration;       // 전체 길이 (초)
    private LocalDateTime updatedAt;

    /**
     * 진도율 계산
     * - Redis에 저장하지 않음 (계산된 값)
     *
     * @return 진도율 (0~100)
     */
    @JsonIgnore
    public Double getProgressPercent() {
        if (duration == null || duration == 0) {
            return 0.0;
        }
        return Math.min((position * 100.0) / duration, 100.0);
    }

    /**
     * 완료 여부 확인 (90% 이상)
     * - Redis에 저장하지 않음 (계산된 값)
     *
     * @return 완료 여부
     */
    @JsonIgnore
    public Boolean isCompleted() {
        return getProgressPercent() >= 90.0;
    }
}