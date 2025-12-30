package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.enums.EncodingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 비디오 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {

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

    /**
     * Entity -> DTO 변환
     */
    public static VideoResponse from(Video video) {
        List<String> availableResolutions = new ArrayList<>();
        if (video.getUrl1080p() != null) availableResolutions.add("1080p");
        if (video.getUrl720p() != null) availableResolutions.add("720p");
        if (video.getUrl540p() != null) availableResolutions.add("540p");

        return VideoResponse.builder()
                .id(video.getId())
                .lectureId(video.getLecture().getId())
                .lectureName(video.getLecture().getTitle())
                .name(video.getName())
                .originalUrl(video.getOriginalUrl())
                .url1080p(video.getUrl1080p())
                .url720p(video.getUrl720p())
                .url540p(video.getUrl540p())
                .thumbnailUrl(video.getThumbnailUrl())
                .subtitleUrl(video.getSubtitleUrl())
                .durationSeconds(video.getDurationSeconds())
                .resolution(video.getResolution())
                .fileSize(video.getFileSize())
                .encodingStatus(video.getEncodingStatus())
                .encodingProgress(video.getEncodingProgress())
                .defaultResolution(video.getDefaultResolution())
                .availableResolutions(availableResolutions)
                .createdAt(video.getCreatedAt())
                .build();
    }

    /**
     * Entity -> 간단한 DTO 변환 (Lecture 정보 제외)
     */
    public static VideoResponse fromSimple(Video video) {
        List<String> availableResolutions = new ArrayList<>();
        if (video.getUrl1080p() != null) availableResolutions.add("1080p");
        if (video.getUrl720p() != null) availableResolutions.add("720p");
        if (video.getUrl540p() != null) availableResolutions.add("540p");

        return VideoResponse.builder()
                .id(video.getId())
                .lectureId(video.getLecture().getId())
                .name(video.getName())
                .originalUrl(video.getOriginalUrl())
                .url1080p(video.getUrl1080p())
                .url720p(video.getUrl720p())
                .url540p(video.getUrl540p())
                .thumbnailUrl(video.getThumbnailUrl())
                .subtitleUrl(video.getSubtitleUrl())
                .durationSeconds(video.getDurationSeconds())
                .resolution(video.getResolution())
                .fileSize(video.getFileSize())
                .encodingStatus(video.getEncodingStatus())
                .encodingProgress(video.getEncodingProgress())
                .defaultResolution(video.getDefaultResolution())
                .availableResolutions(availableResolutions)
                .createdAt(video.getCreatedAt())
                .build();
    }
}