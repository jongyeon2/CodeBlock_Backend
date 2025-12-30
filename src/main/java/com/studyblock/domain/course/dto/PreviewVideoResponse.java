package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.enums.EncodingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 맛보기 비디오 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewVideoResponse {

    private Long id;
    private Long lectureId;
    private String lectureName;
    private String name;
    private String originalUrl;
    private String url1080p;
    private String url720p;
    private String url540p;
    private String thumbnailUrl;
    private String subtitleUrl;
    private Integer durationSeconds;
    private String resolution;
    private Long fileSize;
    private EncodingStatus encodingStatus;
    private Integer encodingProgress;
    private String defaultResolution;
    private List<String> availableResolutions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환 (전체 정보 포함)
     */
    public static PreviewVideoResponse from(PreviewVideo previewVideo) {
        List<String> availableResolutions = new ArrayList<>();
        if (previewVideo.getUrl1080p() != null) availableResolutions.add("1080p");
        if (previewVideo.getUrl720p() != null) availableResolutions.add("720p");
        if (previewVideo.getUrl540p() != null) availableResolutions.add("540p");

        return PreviewVideoResponse.builder()
                .id(previewVideo.getId())
                .lectureId(previewVideo.getLecture().getId())
                .lectureName(previewVideo.getLecture().getTitle())
                .name(previewVideo.getName())
                .originalUrl(previewVideo.getOriginalUrl())
                .url1080p(previewVideo.getUrl1080p())
                .url720p(previewVideo.getUrl720p())
                .url540p(previewVideo.getUrl540p())
                .thumbnailUrl(previewVideo.getThumbnailUrl())
                .subtitleUrl(previewVideo.getSubtitleUrl())
                .durationSeconds(previewVideo.getDurationSeconds())
                .resolution(previewVideo.getResolution())
                .fileSize(previewVideo.getFileSize())
                .encodingStatus(previewVideo.getEncodingStatus())
                .encodingProgress(previewVideo.getEncodingProgress())
                .defaultResolution(previewVideo.getDefaultResolution())
                .availableResolutions(availableResolutions)
                .createdAt(previewVideo.getCreatedAt())
                .updatedAt(previewVideo.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> 간단한 DTO 변환 (Lecture 정보 제외)
     */
    public static PreviewVideoResponse fromSimple(PreviewVideo previewVideo) {
        List<String> availableResolutions = new ArrayList<>();
        if (previewVideo.getUrl1080p() != null) availableResolutions.add("1080p");
        if (previewVideo.getUrl720p() != null) availableResolutions.add("720p");
        if (previewVideo.getUrl540p() != null) availableResolutions.add("540p");

        return PreviewVideoResponse.builder()
                .id(previewVideo.getId())
                .lectureId(previewVideo.getLecture().getId())
                .name(previewVideo.getName())
                .originalUrl(previewVideo.getOriginalUrl())
                .url1080p(previewVideo.getUrl1080p())
                .url720p(previewVideo.getUrl720p())
                .url540p(previewVideo.getUrl540p())
                .thumbnailUrl(previewVideo.getThumbnailUrl())
                .subtitleUrl(previewVideo.getSubtitleUrl())
                .durationSeconds(previewVideo.getDurationSeconds())
                .resolution(previewVideo.getResolution())
                .fileSize(previewVideo.getFileSize())
                .encodingStatus(previewVideo.getEncodingStatus())
                .encodingProgress(previewVideo.getEncodingProgress())
                .defaultResolution(previewVideo.getDefaultResolution())
                .availableResolutions(availableResolutions)
                .createdAt(previewVideo.getCreatedAt())
                .updatedAt(previewVideo.getUpdatedAt())
                .build();
    }
}

