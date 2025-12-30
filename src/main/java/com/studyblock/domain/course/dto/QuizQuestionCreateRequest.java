package com.studyblock.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyblock.domain.course.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 퀴즈 문제 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionCreateRequest {

    @NotBlank(message = "문제 내용을 입력해주세요.")
    private String question;

    @NotNull(message = "문제 유형을 선택해주세요.")
    @JsonProperty("question_type")
    private QuestionType questionType;

    @Min(value = 1, message = "점수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "점수는 100 이하여야 합니다.")
    private Integer points = 1;

    /**
     * 객관식 선택지 (MULTIPLE_CHOICE일 때만 사용)
     * 최소 2개, 최대 10개
     */
    private List<String> options;

    /**
     * 정답
     * - MULTIPLE_CHOICE: 옵션 인덱스 (Integer, 0부터 시작)
     * - SUBJECTIVE/SHORT_ANSWER: 정답 텍스트 (String)
     */
    @NotNull(message = "정답을 입력해주세요.")
    @JsonProperty("correct_answer")
    private Object correctAnswer;

    /**
     * 해설 (선택사항)
     */
    private String explanation;
}