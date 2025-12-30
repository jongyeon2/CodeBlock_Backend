package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseQuestionUpdateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
