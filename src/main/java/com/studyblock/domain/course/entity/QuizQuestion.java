package com.studyblock.domain.course.entity;

import com.studyblock.domain.course.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private Integer points = 1;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizOption> options = new ArrayList<>();

    @Builder
    public QuizQuestion(Quiz quiz, String questionText, QuestionType questionType,
                        String explanation, Integer points, Integer sequence) {
        this.quiz = quiz;
        this.questionText = questionText;
        this.questionType = questionType;
        this.explanation = explanation;
        this.points = points != null ? points : 1;
        this.sequence = sequence;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void updateQuestion(String questionText, String explanation, Integer points) {
        if (questionText != null) {
            this.questionText = questionText;
        }
        if (explanation != null) {
            this.explanation = explanation;
        }
        if (points != null) {
            this.points = points;
        }
    }

    public void addOption(QuizOption option) {
        this.options.add(option);
    }

    /**
     * 모든 옵션 삭제 (cascade로 DB에서도 삭제됨)
     */
    public void clearOptions() {
        this.options.clear();
    }

    /**
     * 옵션 전체 교체
     * 기존 옵션 삭제 후 새 옵션 추가
     */
    public void replaceOptions(List<QuizOption> newOptions) {
        this.options.clear();
        if (newOptions != null) {
            this.options.addAll(newOptions);
        }
    }

    public boolean isMultipleChoice() {
        return this.questionType == QuestionType.MULTIPLE_CHOICE;
    }

    public QuizOption getCorrectOption() {
        return options.stream()
                .filter(QuizOption::getIsCorrect)
                .findFirst()
                .orElse(null);
    }

    /**
     * 퀴즈 ID 조회 (편의 메서드)
     */
    public Long getQuizId() {
        return this.quiz != null ? this.quiz.getId() : null;
    }
}
