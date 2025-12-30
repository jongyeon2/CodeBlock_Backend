package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.enums.EncodingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 맛보기 비디오 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewVideoUploadResponse {

    private Long id;
    private Long lectureId;
    private String originalUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private String resolution;
    private EncodingStatus encodingStatus;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static PreviewVideoUploadResponse from(PreviewVideo previewVideo) {
        return PreviewVideoUploadResponse.builder()
                .id(previewVideo.getId())
                .lectureId(previewVideo.getLecture().getId())
                .originalUrl(previewVideo.getOriginalUrl())
                .thumbnailUrl(previewVideo.getThumbnailUrl())
                .fileName(previewVideo.getName())
                .fileSize(previewVideo.getFileSize())
                .resolution(previewVideo.getResolution())
                .encodingStatus(previewVideo.getEncodingStatus())
                .createdAt(previewVideo.getCreatedAt())
                .build();
    }
}

