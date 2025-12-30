package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.enums.CoursePrerequisiteType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_prerequisite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePrerequisite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoursePrerequisiteType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder
    public CoursePrerequisite(Course course, CoursePrerequisiteType type,
                              String description, Integer displayOrder) {
        this.course = course;
        this.type = type;
        this.description = description;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void updatePrerequisite(CoursePrerequisiteType type, String description, Integer displayOrder) {
        this.type = type;
        this.description = description;
        this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
    }
}
