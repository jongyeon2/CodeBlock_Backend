package com.studyblock.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 퀴즈 문제 수정 요청 DTO
 *
 * 주의사항:
 * - questionType 변경은 불가 (필요시 삭제 후 재생성)
 * - 모든 필드 선택사항
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionUpdateRequest {

    /**
     * 문제 내용
     */
    private String question;

    /**
     * 점수
     */
    @Min(value = 1, message = "점수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "점수는 100 이하여야 합니다.")
    private Integer points;

    /**
     * 객관식 선택지 (MULTIPLE_CHOICE일 때만 사용)
     * 전체 교체 방식: 기존 옵션 전부 삭제 후 새로 생성
     */
    private List<String> options;

    /**
     * 정답
     * - MULTIPLE_CHOICE: 옵션 인덱스 (Integer, 0부터 시작)
     * - SUBJECTIVE/SHORT_ANSWER: 정답 텍스트 (String)
     */
    @JsonProperty("correct_answer")
    private Object correctAnswer;

    /**
     * 해설
     */
    private String explanation;
}