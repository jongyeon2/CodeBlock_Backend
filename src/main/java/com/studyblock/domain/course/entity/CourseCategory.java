package com.studyblock.domain.course.entity;

import com.studyblock.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    private CourseCategory(Course course, Category category) {
        this.course = course;
        this.category = category;
    }

    public static CourseCategory of(Course course, Category category) {
        return new CourseCategory(course, category);
    }
}
