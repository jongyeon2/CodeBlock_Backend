package com.studyblock.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {

    private Long quizId;
    private Integer score;
    private Integer correctCount;
    private Integer totalQuestions;
    private Boolean passed;
    private Integer attemptNumber;
}
