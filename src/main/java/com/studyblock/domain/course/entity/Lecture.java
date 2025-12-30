package com.studyblock.domain.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.enums.LectureStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lecture")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private com.studyblock.domain.user.entity.InstructorProfile instructor;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "upload_date", nullable = false)
    private LocalDate uploadDate;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "is_free", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isFree = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LectureStatus status = LectureStatus.ACTIVE;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    /**
     * 강의 메인 비디오 (1:1 관계)
     * - 기존 1:N 관계에서 1:1로 변경
     * - @JsonIgnore: Jackson 직렬화 시 순환 참조 방지 (LAZY 로딩 무한 루프 방지)
     */
    @JsonIgnore
    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Video video;

    /**
     * 맛보기 강의 비디오 (1:1 관계)
     * - 무료 미리보기용
     * - @JsonIgnore: Jackson 직렬화 시 순환 참조 방지 (LAZY 로딩 무한 루프 방지)
     */
    @JsonIgnore
    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PreviewVideo previewVideo;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quiz> quizzes = new ArrayList<>();

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureResource> resources = new ArrayList<>();

    @Builder
    public Lecture(Course course, Section section, com.studyblock.domain.user.entity.InstructorProfile instructor,
                   String title, String description, String thumbnailUrl,
                   LocalDate uploadDate, Integer sequence, Boolean isFree) {
        this.course = course;
        this.section = section;
        this.instructor = instructor;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.uploadDate = uploadDate;
        this.sequence = sequence;
        this.isFree = isFree != null ? isFree : false;
        this.status = LectureStatus.ACTIVE;
        this.viewCount = 0L;
    }

    // Business methods
    public void activate() {
        this.status = LectureStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = LectureStatus.INACTIVE;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void updateInfo(String title, String description, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateSequence(Integer sequence) {
        if (sequence != null && sequence > 0) {
            this.sequence = sequence;
        }
    }

    public void updateStatus(LectureStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void assignInstructor(com.studyblock.domain.user.entity.InstructorProfile instructor) {
        this.instructor = instructor;
    }

    /**
     * 메인 비디오 설정
     */
    public void setVideo(Video video) {
        this.video = video;
    }

    /**
     * 맛보기 비디오 설정
     */
    public void setPreviewVideo(PreviewVideo previewVideo) {
        this.previewVideo = previewVideo;
    }

    /**
     * 맛보기 비디오 제거
     * - orphanRemoval=true 설정으로 인해 null로 설정하면 자동으로 DB에서 삭제됨
     */
    public void removePreviewVideo() {
        this.previewVideo = null;
    }

    /**
     * 메인 비디오 제거
     * - orphanRemoval=true 설정으로 인해 null로 설정하면 자동으로 DB에서 삭제됨
     */
    public void removeVideo() {
        this.video = null;
    }

    /**
     * 메인 비디오 존재 여부 확인
     */
    public boolean hasVideo() {
        return this.video != null;
    }

    /**
     * 맛보기 비디오 존재 여부 확인
     * - @JsonIgnore: Jackson 직렬화 시 호출 방지 (N+1 문제 방지)
     * - 이 메서드는 비즈니스 로직에서만 사용하고, Jackson 직렬화 시에는 호출되지 않도록 함
     */
    @JsonIgnore
    public boolean hasPreviewVideo() {
        // Hibernate LAZY 프록시 초기화 방지를 위해 Hibernate.isInitialized() 사용 고려 가능
        // 하지만 단순 null 체크도 프록시 초기화를 트리거할 수 있음
        // Fetch Join으로 미리 로드하거나, 직접 쿼리로 존재 여부만 확인하는 것이 안전
        return this.previewVideo != null;
    }

    // Helper methods
    public String getInstructorName() {
        return this.instructor != null ? this.instructor.getInstructorName() : null;
    }

    public String getInstructorChannelName() {
        return this.instructor != null ? this.instructor.getChannelName() : null;
    }

    public String getInstructorBio() {
        return this.instructor != null ? this.instructor.getBio() : null;
    }

}
