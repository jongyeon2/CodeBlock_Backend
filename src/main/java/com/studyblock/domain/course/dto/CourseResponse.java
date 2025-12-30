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

/**
 * 코스 응답 DTO (목록 조회용)
 * - 프론트엔드 응답 형식에 맞춤
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class CourseResponse {

    private Long id;
    private String title;
    private String summary;
    private CourseLevel level;
    private Integer durationMinutes;
    private String thumbnailUrl;            // presigned URL (노출용)
    private String thumbnailOriginalUrl;    // 원본 S3 URL
    private Long price;               // 현금 가격 (KRW)
    private Long discountedPrice;     // 할인 적용된 가격
    private Integer discountPercentage;
    private Long enrollmentCount;
    private Boolean isPublished;
    private List<CategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 강사 정보
    private Long instructorId;
    private String instructorName;
    private String instructorChannelName;

    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .summary(course.getSummary())
                .level(course.getLevel())
                .durationMinutes(course.getDurationMinutes())
                .thumbnailUrl(course.getThumbnailUrl())
                .thumbnailOriginalUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .discountedPrice(course.getDiscountedPrice())
                .discountPercentage(course.getDiscountPercentage())
                .enrollmentCount(course.getEnrollmentCount())
                .isPublished(course.getIsPublished())
                .categories(course.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .instructorId(course.getInstructorId())
                .instructorName(course.getInstructorName())
                .instructorChannelName(course.getInstructorChannelName())
                .build();
    }
}