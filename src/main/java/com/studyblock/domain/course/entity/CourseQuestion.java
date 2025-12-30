package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.enums.CourseQuestionStatus;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseQuestionStatus status = CourseQuestionStatus.PENDING;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseQuestionAnswer> answers = new ArrayList<>();

    @Builder
    public CourseQuestion(Course course, User user, String title,
                          String content, String answer, CourseQuestionStatus status) {
        this.course = course;
        this.user = user;
        this.title = title;
        this.content = content;
        this.answer = answer;
        this.status = status != null ? status : CourseQuestionStatus.PENDING;
    }

    public void updateQuestion(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateAnswer(String answer, CourseQuestionStatus status) {
        this.answer = answer;
        this.status = status != null ? status : this.status;
    }

    public List<CourseQuestionAnswer> getAnswers() {
        return answers;
    }

    public void addAnswer(CourseQuestionAnswer answer) {
        answers.add(answer);
    }
}
