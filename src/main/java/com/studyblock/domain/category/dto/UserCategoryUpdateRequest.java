package com.studyblock.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 관심 카테고리 일괄 업데이트 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 관심 카테고리 일괄 업데이트 요청")
public class UserCategoryUpdateRequest {

    @NotEmpty(message = "카테고리 ID 목록은 비어있을 수 없습니다.")
    @Schema(description = "새로운 관심 카테고리 ID 목록", example = "[1, 2, 3]", required = true)
    private List<Long> categoryIds;
}





