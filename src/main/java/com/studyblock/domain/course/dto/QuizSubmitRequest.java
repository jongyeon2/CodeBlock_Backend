package com.studyblock.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {

    // Key: questionId, Value: answerId (객관식) 또는 answerText (주관식)
    private Map<Long, Object> answers;
}
