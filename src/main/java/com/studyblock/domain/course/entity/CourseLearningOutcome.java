package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_learning_outcome")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseLearningOutcome extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder
    public CourseLearningOutcome(Course course, String content, Integer displayOrder) {
        this.course = course;
        this.content = content;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void updateContent(String content, Integer displayOrder) {
        this.content = content;
        this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
    }
}
