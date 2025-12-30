package com.studyblock.domain.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.studyblock.domain.course.enums.EncodingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 맛보기 비디오 엔티티 (Lecture와 1:1 관계)
 * - 강의당 하나의 맛보기 비디오만 허용
 * - Video 엔티티와 동일한 구조를 가지지만 별도 테이블로 관리
 * - VideoResource 인터페이스 구현으로 공통 로직 재사용
 */
@Entity
@Table(name = "preview_video")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreviewVideo implements VideoResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 강의 (1:1 관계)
     * - @JsonIgnore: Jackson 직렬화 시 순환 참조 방지 (LAZY 로딩 무한 루프 방지)
     */
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false, unique = true)
    private Lecture lecture;

    @Column(nullable = false)
    private String name;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "url_1080p", length = 500)
    private String url1080p;

    @Column(name = "url_720p", length = 500)
    private String url720p;

    @Column(name = "url_540p", length = 500)
    private String url540p;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 20)
    private String resolution;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "encoding_status", nullable = false, length = 20)
    private EncodingStatus encodingStatus = EncodingStatus.PENDING;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "subtitle_url", length = 500)
    private String subtitleUrl;

    @Column(name = "default_resolution", nullable = false, length = 10)
    private String defaultResolution = "720p"; // 기본 인코딩 해상도: 720p (속도 및 메모리 최적화)

    @Column(name = "encoding_progress")
    private Integer encodingProgress = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder
    public PreviewVideo(Lecture lecture, String name, String originalUrl, String url1080p,
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
        this.updatedAt = LocalDateTime.now();
    }

    // VideoResource 인터페이스 구현 - 인코딩 상태 변경
    @Override
    public void startEncoding() {
        this.encodingStatus = EncodingStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void completeEncoding() {
        this.encodingStatus = EncodingStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void failEncoding() {
        this.encodingStatus = EncodingStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    // VideoResource 인터페이스 구현 - URL 업데이트
    @Override
    public void updateVideoUrls(String url1080p, String url720p, String url540p) {
        this.url1080p = url1080p;
        this.url720p = url720p;
        this.url540p = url540p;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void updateVideoInfo(String originalUrl, Integer durationSeconds, String resolution, Long fileSize) {
        this.originalUrl = originalUrl;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.fileSize = fileSize;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSubtitleUrl(String subtitleUrl) {
        this.subtitleUrl = subtitleUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDefaultResolution(String resolution) {
        this.defaultResolution = resolution;
    }

    public void updateEncodingProgress(Integer progress) {
        if (progress != null && progress >= 0 && progress <= 100) {
            this.encodingProgress = progress;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
