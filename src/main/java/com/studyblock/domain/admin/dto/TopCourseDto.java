package com.studyblock.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TopCourseDto {
    private String title;      // 강의 제목
    private Integer students;  // 수강자 수
}

