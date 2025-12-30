package com.studyblock.domain.admin.dto;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.CourseReview;
import com.studyblock.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewListResponse {

    private Long id;
    private String content;
    private Integer rating;
    private String name;
    private String title;
    private LocalDateTime update_at;
    private Long categoryId;
    private String categoryName;

    public static ReviewListResponse from(CourseReview courseReview) {
        User user = courseReview.getUser();
        Course course = courseReview.getCourse();
        var primaryCategory = course.getPrimaryCategory();

        return ReviewListResponse.builder()
                .id(courseReview.getId())
                .content(courseReview.getContent())
                .rating(courseReview.getRating())
                .name(user.getName())
                .title(course.getTitle())
                .update_at(course.getUpdatedAt())
                .categoryId(primaryCategory != null ? primaryCategory.getId() : null)
                .categoryName(primaryCategory != null ? primaryCategory.getName() : null)
                .build();
    }
}
