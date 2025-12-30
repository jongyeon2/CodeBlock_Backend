package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseReviewUpdateRequest {

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String content;

    private Boolean lectureSpecific;
}
