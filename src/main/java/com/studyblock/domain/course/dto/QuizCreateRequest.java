package com.studyblock.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyblock.domain.course.enums.QuizPosition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 퀴즈 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizCreateRequest {

    @NotBlank(message = "퀴즈 제목을 입력해주세요.")
    private String title;

    private String description;

    @NotNull(message = "섹션 ID를 입력해주세요.")
    @JsonProperty("section_id")
    private Long sectionId;

    @NotNull(message = "퀴즈 위치를 입력해주세요.")
    private QuizPosition position;

    @JsonProperty("target_lecture_id")
    private Long targetLectureId;

    @Min(value = 0, message = "합격 점수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "합격 점수는 100 이하여야 합니다.")
    @JsonProperty("passing_score")
    private Integer passingScore = 60;

    @Min(value = 1, message = "최대 시도 횟수는 1 이상이어야 합니다.")
    @JsonProperty("max_attempts")
    private Integer maxAttempts = 3;

    /**
     * 문제 정보 (선택 사항 - 나중에 별도 API로 추가 가능)
     */
    private QuizQuestionCreateRequest question;
}