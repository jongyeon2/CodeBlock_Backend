package com.studyblock.domain.course.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 비디오 진도율 응답 DTO
 */
@Getter
@Builder
public class VideoProgressResponse {

    private Long videoId;
    private Integer lastPosition;    // 마지막 시청 위치 (초)
    private Integer duration;        // 전체 길이 (초)
    private Boolean isCompleted;     // 완료 여부
    private Double progressPercent;  // 진도율 (0~100)

    /**
     * VideoProgressDto로부터 응답 DTO 생성
     *
     * @param dto Redis 저장 DTO
     * @return 응답 DTO
     */
    public static VideoProgressResponse from(VideoProgressDto dto) {
        if (dto == null) {
            return null;
        }

        return VideoProgressResponse.builder()
                .videoId(dto.getVideoId())
                .lastPosition(dto.getPosition())
                .duration(dto.getDuration())
                .isCompleted(dto.isCompleted())
                .progressPercent(dto.getProgressPercent())
                .build();
    }
}