package com.studyblock.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 관심 카테고리 추가 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 관심 카테고리 추가 요청")
public class UserCategoryAddRequest {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    @Schema(description = "추가할 카테고리 ID", example = "1", required = true)
    private Long categoryId;
}





