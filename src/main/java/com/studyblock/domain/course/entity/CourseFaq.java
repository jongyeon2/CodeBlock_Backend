package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseFaq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(length = 50)
    private String tag;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder
    public CourseFaq(Course course, String question, String answer, String tag, Integer displayOrder) {
        this.course = course;
        this.question = question;
        this.answer = answer;
        this.tag = tag;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void updateFaq(String question, String answer, String tag, Integer displayOrder) {
        this.question = question;
        this.answer = answer;
        this.tag = tag;
        this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
    }
}
