package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.CoursePrerequisite;
import com.studyblock.domain.course.enums.CoursePrerequisiteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePrerequisiteResponse {

    private Long id;
    private CoursePrerequisiteType type;
    private String description;
    private Integer order;

    public static CoursePrerequisiteResponse from(CoursePrerequisite prerequisite) {
        return CoursePrerequisiteResponse.builder()
                .id(prerequisite.getId())
                .type(prerequisite.getType())
                .description(prerequisite.getDescription())
                .order(prerequisite.getDisplayOrder())
                .build();
    }
}
