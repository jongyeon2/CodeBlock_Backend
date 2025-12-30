package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Integer passingScore;
    private Integer maxAttempts;
    private Integer sequence;
    private List<QuizQuestionResponse> questions;

    public static QuizDetailResponse from(Quiz quiz) {
        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .sequence(quiz.getSequence())
                .questions(quiz.getQuestions().stream()
                        .sorted((q1, q2) -> q1.getSequence().compareTo(q2.getSequence()))
                        .map(QuizQuestionResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
