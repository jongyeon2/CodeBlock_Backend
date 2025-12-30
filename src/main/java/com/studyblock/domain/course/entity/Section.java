package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "cookie_price")
    private Long cookiePrice;

    @Column(name = "discount_percentage")
    private Integer discountPercentage = 0;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quiz> quizzes = new ArrayList<>();

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureResource> resources = new ArrayList<>();

    @Builder
    public Section(Course course, String title, String description, Integer sequence, Integer durationMinutes,
                   Long cookiePrice, Integer discountPercentage) {
        this.course = course;
        this.title = title;
        this.description = description;
        this.sequence = sequence;
        this.durationMinutes = durationMinutes;
        this.cookiePrice = cookiePrice;
        this.discountPercentage = discountPercentage != null ? discountPercentage : 0;
    }

    // Business methods
    public void updateInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updatePricing(Long cookiePrice, Integer discountPercentage) {
        this.cookiePrice = cookiePrice;
        this.discountPercentage = discountPercentage != null ? discountPercentage : 0;
    }

    public void updateSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public void addLecture(Lecture lecture) {
        this.lectures.add(lecture);
    }

    public void addQuiz(Quiz quiz) {
        this.quizzes.add(quiz);
    }

    public void calculateDurationMinutes() {
        this.durationMinutes = lectures.stream()
                .map(Lecture::getVideo)
                .filter(video -> video != null)
                .mapToInt(video -> {
                    Integer durationSeconds = video.getDurationSeconds();
                    return durationSeconds != null && durationSeconds > 0 ? durationSeconds / 60 : 0;
                })
                .sum();
    }

    // Helper methods
    public int getLectureCount() {
        return lectures.size();
    }

    public String getCourseName() {
        return this.course != null ? this.course.getTitle() : null;
    }

    public Long getDiscountedPrice() {
        if (cookiePrice == null || discountPercentage == null || discountPercentage == 0) {
            return cookiePrice;
        }
        return cookiePrice - (cookiePrice * discountPercentage / 100);
    }
}
