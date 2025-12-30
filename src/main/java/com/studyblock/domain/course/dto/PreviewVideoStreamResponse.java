package com.studyblock.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 맛보기 비디오 스트리밍 URL 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewVideoStreamResponse {

    private Long previewVideoId;
    private String streamingUrl;
    private Integer expiresInMinutes;
    private String message;

    /**
     * 스트리밍 URL 생성
     */
    public static PreviewVideoStreamResponse of(Long previewVideoId, String streamingUrl, int expiresInMinutes) {
        return PreviewVideoStreamResponse.builder()
                .previewVideoId(previewVideoId)
                .streamingUrl(streamingUrl)
                .expiresInMinutes(expiresInMinutes)
                .message(expiresInMinutes + "분간 유효한 스트리밍 URL입니다.")
                .build();
    }
}

