package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.enums.EncodingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비디오 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadResponse {

    private Long id;
    private Long lectureId;
    private String originalUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private EncodingStatus encodingStatus;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static VideoUploadResponse from(Video video) {
        return VideoUploadResponse.builder()
                .id(video.getId())
                .lectureId(video.getLecture().getId())
                .originalUrl(video.getOriginalUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .fileName(video.getName())
                .fileSize(video.getFileSize())
                .encodingStatus(video.getEncodingStatus())
                .createdAt(video.getCreatedAt())
                .build();
    }
}