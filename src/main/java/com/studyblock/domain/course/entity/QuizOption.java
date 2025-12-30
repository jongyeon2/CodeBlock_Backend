package com.studyblock.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "is_correct", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isCorrect = false;

    @Column(nullable = false)
    private Integer sequence;

    @Builder
    public QuizOption(QuizQuestion quizQuestion, String optionText, Boolean isCorrect, Integer sequence) {
        this.quizQuestion = quizQuestion;
        this.optionText = optionText;
        this.isCorrect = isCorrect != null ? isCorrect : false;
        this.sequence = sequence;
    }

    // Business methods
    public void updateOption(String optionText, Boolean isCorrect) {
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }

    public void markAsCorrect() {
        this.isCorrect = true;
    }

    public void markAsIncorrect() {
        this.isCorrect = false;
    }
}
