package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.enums.CourseLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 생성 요청 DTO
 * - 프론트엔드에서 전송하는 데이터 구조에 맞춤
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "코스 제목은 필수입니다.")
    @Size(max = 50, message = "코스 제목은 50자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "코스 설명은 필수입니다.")
    private String description; // 프론트에서 description -> 백엔드 summary로 매핑

    @NotNull(message = "난이도를 선택해주세요.")
    private CourseLevel difficulty; // 프론트에서 difficulty -> 백엔드 level로 매핑

    private Integer durationMinutes;

    private String thumbnailUrl;

    @NotNull(message = "가격은 필수입니다.")
    private Long price; // 정규코스 현금 가격 (KRW)

    private Integer discountPercentage;

    private Long enrollmentCount;

    @NotNull(message = "공개 여부는 필수입니다.")
    private Boolean isPublished; // 프론트에서 isPublished(true/false) -> 백엔드 isPublished(true/false)로 매핑

    @Valid
    @NotNull(message = "카테고리를 선택해주세요.")
    private CategoryInfo category;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        @NotNull(message = "대분류 카테고리를 선택해주세요.")
        private Long parentId;

        @NotNull(message = "소분류 카테고리를 선택해주세요.")
        private Long childId;
    }
}