package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.enums.CourseLevel;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 부분 수정 요청 DTO (PATCH 전용)
 * - 전달된 필드만 업데이트 (null은 변경 없음)
 * - 필드명 카멜케이스 통일
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePatchRequest {

    @Nullable private String title;
    @Nullable private String description;
    @Nullable private CourseLevel difficulty;
    @Nullable private Integer durationMinutes;
    @Nullable private String thumbnailUrl;
    @Nullable private Long price;                 // 정규코스 현금 가격 (KRW)
    @Nullable private Integer discountPercentage; // 0~100
    @Nullable private Boolean isPublished;
    @Nullable private CategoryInfo category;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        @Nullable private Long parentId;
        @Nullable private Long childId;
    }
}







