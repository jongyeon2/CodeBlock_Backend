package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자료 메타데이터 수정 요청 DTO
 * 파일은 수정되지 않고 제목과 설명만 수정
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;
}