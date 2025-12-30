package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionRequest {

    @NotBlank(message = "섹션 제목은 필수입니다")
    @Size(max = 255, message = "섹션 제목은 255자를 초과할 수 없습니다")
    private String title;

    private String description;

    @NotNull(message = "순서는 필수입니다")
    @Min(value = 1, message = "순서는 1 이상이어야 합니다")
    private Integer sequence;

    @Min(value = 0, message = "쿠키 가격은 0 이상이어야 합니다")
    private Long cookiePrice;

    @Min(value = 0, message = "할인율은 0 이상이어야 합니다")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다")
    private Integer discountPercentage;
}