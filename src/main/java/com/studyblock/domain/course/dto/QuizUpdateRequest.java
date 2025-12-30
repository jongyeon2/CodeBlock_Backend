package com.studyblock.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyblock.domain.course.enums.QuizPosition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 퀴즈 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizUpdateRequest {

    private String title;

    private String description;

    private QuizPosition position;

    @JsonProperty("target_lecture_id")
    private Long targetLectureId;

    @Min(value = 0, message = "합격 점수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "합격 점수는 100 이하여야 합니다.")
    @JsonProperty("passing_score")
    private Integer passingScore;

    @Min(value = 1, message = "최대 시도 횟수는 1 이상이어야 합니다.")
    @JsonProperty("max_attempts")
    private Integer maxAttempts;
}