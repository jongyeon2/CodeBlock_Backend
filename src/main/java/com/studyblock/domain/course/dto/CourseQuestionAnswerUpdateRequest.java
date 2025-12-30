package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseQuestionAnswerUpdateRequest {

    @NotBlank
    private String content;
}

