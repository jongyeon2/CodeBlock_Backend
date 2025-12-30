package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "course_question_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseQuestionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private CourseQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1)")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Boolean deleted = false;

    @Builder
    public CourseQuestionAnswer(CourseQuestion question, User author, String content) {
        this.question = question;
        this.author = author;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}

