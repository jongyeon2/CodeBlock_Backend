package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseReviewCreateRequest {

    // userId 제거: SecurityContext에서 인증된 사용자 정보를 가져옴

    private Long lectureId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String content;

    private Boolean lectureSpecific;
}
