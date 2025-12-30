package com.studyblock.domain.admin.dto;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
public class CourseListResponse {

    private Long id;
    private String title;
    private String summary;
    private String name;
    private String categoryName;
    private Long categoryId;
    private CourseLevel level;
    private Integer durationMinutes;
    private Long price;
    private Long enrollmentCount;
    private Boolean isPublished;

    public static CourseListResponse from(Course course) {
        return CourseListResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .summary(course.getSummary())
                .name(course.getInstructorName())
                .categoryName(course.getPrimaryCategory() != null ?
                              course.getPrimaryCategory().getName() : null)
                .categoryId(course.getPrimaryCategory() != null ?
                            course.getPrimaryCategory().getId() : null)
                .level(course.getLevel())
                .durationMinutes(course.getDurationMinutes())
                .price(course.getPrice())
                .enrollmentCount(course.getEnrollmentCount())
                .isPublished(course.getIsPublished())
                .build();
    }
}
