package com.studyblock.domain.user.dto;

import com.studyblock.domain.course.dto.CourseResponse;
import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.course.entity.Section;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// OwnedSectionResponse.java - 보유 강의 전용
@Getter
@Builder
public class OwnedSectionResponse {
    // Section 정보
    private Long sectionId;
    private String sectionTitle;
    private String description;
    private Long cookiePrice;

    // Course 정보
    private CourseResponse course;

    // Ownership 정보 (보유 강의만의 정보)
    private LocalDateTime createdAt; //구매일
    private Enum source;
    private LocalDateTime expiresAt;

    public static OwnedSectionResponse from(LectureOwnership ownership) {
        Section section = ownership.getSection();

        return OwnedSectionResponse.builder()
                .sectionId(section.getId())
                .sectionTitle(section.getTitle())
                .description(section.getDescription())
                .cookiePrice(section.getCookiePrice())
                .course(CourseResponse.from(section.getCourse()))
                // Ownership 정보
                .createdAt(ownership.getCreatedAt())
                .source(ownership.getSource())
                .expiresAt(ownership.getExpiresAt())
                .build();
    }
}