package com.studyblock.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyblock.domain.course.entity.Quiz;
import com.studyblock.domain.course.enums.QuizPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSummaryResponse {

    private Long id;
    private String title;
    private String description;

    @JsonProperty("section_id")
    private Long sectionId;

    private QuizPosition position;

    @JsonProperty("target_lecture_id")
    private Long targetLectureId;

    @JsonProperty("course_id")
    private Long courseId;

    private Integer sequence;

    @JsonProperty("passing_score")
    private Integer passingScore;

    @JsonProperty("max_attempts")
    private Integer maxAttempts;

    @JsonProperty("question_count")
    private Integer questionCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static QuizSummaryResponse from(Quiz quiz) {
        return QuizSummaryResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .sectionId(quiz.getSectionId())
                .position(quiz.getPosition())
                .targetLectureId(quiz.getTargetLectureId())
                .courseId(quiz.getCourseId())
                .sequence(quiz.getSequence())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .questionCount(quiz.getQuestionCount())
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
