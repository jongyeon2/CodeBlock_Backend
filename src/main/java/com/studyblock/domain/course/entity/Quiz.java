package com.studyblock.domain.course.entity;

import com.studyblock.domain.course.enums.QuizPosition;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizPosition position = QuizPosition.AFTER_LECTURE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_lecture_id")
    private Lecture targetLecture;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "passing_score", nullable = false)
    private Integer passingScore = 60;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> questions = new ArrayList<>();

    @Builder
    public Quiz(Lecture lecture, Section section, Course course, String title, String description,
                Integer passingScore, Integer maxAttempts, Integer sequence,
                QuizPosition position, Lecture targetLecture) {
        this.lecture = lecture;
        this.section = section;
        this.course = course;
        this.title = title;
        this.description = description;
        this.passingScore = passingScore != null ? passingScore : 60;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.sequence = sequence;
        this.position = position != null ? position : QuizPosition.AFTER_LECTURE;
        this.targetLecture = targetLecture;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void updateInfo(String title, String description, Integer passingScore, Integer maxAttempts) {
        this.title = title;
        this.description = description;
        this.passingScore = passingScore;
        this.maxAttempts = maxAttempts;
    }

    public void updatePosition(QuizPosition position, Lecture targetLecture, Integer sequence) {
        this.position = position;
        this.targetLecture = targetLecture;
        this.sequence = sequence;
    }

    public void updateSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public void addQuestion(QuizQuestion question) {
        this.questions.add(question);
    }

    public int getTotalPoints() {
        return questions.stream()
                .mapToInt(QuizQuestion::getPoints)
                .sum();
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public Long getCourseId() {
        return this.course != null ? this.course.getId() : null;
    }

    public Long getSectionId() {
        return this.section != null ? this.section.getId() : null;
    }

    public Long getTargetLectureId() {
        return this.targetLecture != null ? this.targetLecture.getId() : null;
    }
}
