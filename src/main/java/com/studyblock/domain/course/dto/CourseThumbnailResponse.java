package com.studyblock.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 썸네일 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseThumbnailResponse {

    private String originalUrl;
    private String thumbnailUrl;
    private String filename;
    private Long size;
}


