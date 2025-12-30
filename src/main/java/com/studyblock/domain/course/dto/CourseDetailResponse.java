package com.studyblock.domain.course.dto;

import com.studyblock.domain.category.dto.CategoryResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class CourseDetailResponse {

    private Long id;
    private String title;
    private String summary;
    private String category;
    private List<CategoryResponse> categories;
    private CourseLevel level;
    private Integer totalLectures;
    private Integer totalDurationMinutes;
    private String thumbnailUrl;            // presigned URL (노출용)
    private String thumbnailOriginalUrl;    // 원본 S3 URL
    private Long price;
    private Integer discountPercentage;
    private Integer discountedPrice;
    private Long enrollmentCount;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> purchasedSectionIds;

    public static CourseDetailResponse from(Course course, int totalLectures) {
        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .summary(course.getSummary())
                .category(course.getPrimaryCategory() != null ? course.getPrimaryCategory().getName() : null)
                .categories(course.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList())
                .level(course.getLevel())
                .totalLectures(totalLectures)
                .totalDurationMinutes(course.getDurationMinutes())
                .thumbnailUrl(course.getThumbnailUrl())
                .thumbnailOriginalUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .discountPercentage(course.getDiscountPercentage())
                .discountedPrice(course.getDiscountedPrice().intValue())
                .enrollmentCount(course.getEnrollmentCount())
                .published(course.getIsPublished())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .purchasedSectionIds(Collections.emptyList())
                .build();
    }
}
