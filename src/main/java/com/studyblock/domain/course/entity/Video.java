package com.studyblock.domain.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.studyblock.domain.course.enums.EncodingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "video")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video implements VideoResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 강의 (다:1 관계)
     * - @JsonIgnore: Jackson 직렬화 시 순환 참조 방지 (LAZY 로딩 무한 루프 방지)
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "url_1080p")
    private String url1080p;

    @Column(name = "url_720p")
    private String url720p;

    @Column(name = "url_540p")
    private String url540p;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 20)
    private String resolution;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "encoding_status", nullable = false)
    private EncodingStatus encodingStatus = EncodingStatus.PENDING;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "subtitle_url")
    private String subtitleUrl;

    @Column(name = "default_resolution", nullable = false, length = 10)
    private String defaultResolution = "720p"; // 기본 인코딩 해상도: 720p (속도 및 메모리 최적화)

    @Column(name = "encoding_progress")
    private Integer encodingProgress = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public Video(Lecture lecture, String name, String originalUrl, String url1080p,
                 String url720p, String url540p, Integer durationSeconds,
                 String resolution, Long fileSize, String thumbnailUrl, String subtitleUrl) {
        this.lecture = lecture;
        this.name = name;
        this.originalUrl = originalUrl;
        this.url1080p = url1080p;
        this.url720p = url720p;
        this.url540p = url540p;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.fileSize = fileSize;
        this.encodingStatus = EncodingStatus.PENDING;
        this.thumbnailUrl = thumbnailUrl;
        this.subtitleUrl = subtitleUrl;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void startEncoding() {
        this.encodingStatus = EncodingStatus.PROCESSING;
    }

    public void completeEncoding() {
        this.encodingStatus = EncodingStatus.COMPLETED;
    }

    public void failEncoding() {
        this.encodingStatus = EncodingStatus.FAILED;
    }

    public void updateVideoInfo(String originalUrl, Integer durationSeconds, String resolution, Long fileSize) {
        this.originalUrl = originalUrl;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.fileSize = fileSize;
    }

    /**
     * 인코딩된 비디오 URL 업데이트 (3개 해상도)
     * @param url1080p 1080p 해상도 URL
     * @param url720p 720p 해상도 URL
     * @param url540p 540p 해상도 URL
     */
    public void updateVideoUrls(String url1080p, String url720p, String url540p) {
        this.url1080p = url1080p;
        this.url720p = url720p;
        this.url540p = url540p;
    }

    public void updateDefaultResolution(String resolution) {
        this.defaultResolution = resolution;
    }

    public void updateEncodingProgress(Integer progress) {
        if (progress != null && progress >= 0 && progress <= 100) {
            this.encodingProgress = progress;
        }
    }

    public boolean isEncodingCompleted() {
        return this.encodingStatus == EncodingStatus.COMPLETED;
    }
}
