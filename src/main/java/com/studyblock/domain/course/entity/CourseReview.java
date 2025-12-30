package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_lecture_specific", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean lectureSpecific = false;

    @Builder
    public CourseReview(Course course, User user, Lecture lecture,
                        Integer rating, String content, Boolean lectureSpecific) {
        this.course = course;
        this.user = user;
        this.lecture = lecture;
        this.rating = rating;
        this.content = content;
        this.lectureSpecific = lectureSpecific != null ? lectureSpecific : false;
    }

    public void updateReview(Integer rating, String content, Boolean lectureSpecific) {
        this.rating = rating;
        this.content = content;
        this.lectureSpecific = lectureSpecific != null ? lectureSpecific : this.lectureSpecific;
    }
}
