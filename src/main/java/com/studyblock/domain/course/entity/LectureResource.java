package com.studyblock.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_resource")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = true)  // 섹션 자료는 lecture_id가 null
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    private Integer sequence;

    @Column(name = "upload_at", nullable = false, updatable = false)
    private LocalDateTime uploadAt = LocalDateTime.now();

    @Builder
    public LectureResource(Lecture lecture, Section section, String title, String fileUrl, String fileType,
                           Long fileSize, String description, Integer sequence) {
        this.lecture = lecture;
        this.section = section;
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.description = description;
        this.sequence = sequence;
        this.downloadCount = 0;
        this.uploadAt = LocalDateTime.now();
    }

    // Business methods
    public void increaseDownloadCount() {
        this.downloadCount++;
    }

    public void updateResource(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateFileInfo(String fileUrl, String fileType, Long fileSize) {
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    /**
     * 엔티티 검증 (저장/수정 전 자동 실행)
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        // section은 필수
        if (section == null) {
            throw new IllegalStateException("자료는 반드시 섹션에 속해야 합니다.");
        }
    }

    /**
     * 강의 자료인지 확인
     * @return lecture_id가 있으면 true
     */
    public boolean isLectureMaterial() {
        return this.lecture != null;
    }

    /**
     * 섹션 자료인지 확인
     * @return lecture_id가 null이면 true
     */
    public boolean isSectionMaterial() {
        return this.lecture == null;
    }
}
