package com.studyblock.domain.course.dto;

import com.studyblock.domain.category.dto.CategoryResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class RelatedCourseResponse {

    private Long id;
    private String title;
    private String thumbnailUrl;            // presigned URL
    private String thumbnailOriginalUrl;    // 원본 S3 URL
    private CourseLevel level;
    private String category;
    private List<CategoryResponse> categories;
    private Long price;
    private Integer discountedPrice;
    private Long enrollmentCount;

    public static RelatedCourseResponse from(Course course) {
        return RelatedCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .thumbnailUrl(course.getThumbnailUrl())
                .thumbnailOriginalUrl(course.getThumbnailUrl())
                .level(course.getLevel())
                .category(course.getPrimaryCategory() != null ? course.getPrimaryCategory().getName() : null)
                .categories(course.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList())
                .price(course.getPrice())
                .discountedPrice(course.getDiscountedPrice().intValue())
                .enrollmentCount(course.getEnrollmentCount())
                .build();
    }
}
