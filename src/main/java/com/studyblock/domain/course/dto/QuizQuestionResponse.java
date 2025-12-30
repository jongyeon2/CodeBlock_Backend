package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.QuizQuestion;
import com.studyblock.domain.course.enums.QuestionType;
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
public class QuizQuestionResponse {

    private Long id;
    private String questionText;
    private QuestionType questionType;
    private String explanation;
    private Integer points;
    private Integer sequence;
    private List<QuizOptionResponse> options;

    public static QuizQuestionResponse from(QuizQuestion question) {
        return QuizQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .explanation(question.getExplanation())
                .points(question.getPoints())
                .sequence(question.getSequence())
                .options(question.getOptions().stream()
                        .map(QuizOptionResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
