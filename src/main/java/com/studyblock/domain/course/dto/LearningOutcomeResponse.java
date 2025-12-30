package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.CourseLearningOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningOutcomeResponse {

    private Long id;
    private String content;
    private Integer order;

    public static LearningOutcomeResponse from(CourseLearningOutcome outcome) {
        return LearningOutcomeResponse.builder()
                .id(outcome.getId())
                .content(outcome.getContent())
                .order(outcome.getDisplayOrder())
                .build();
    }
}
