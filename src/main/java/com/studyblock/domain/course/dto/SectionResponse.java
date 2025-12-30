package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Section;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer sequence;
    private Integer durationMinutes;
    private Long cookiePrice;
    private Integer discountPercentage;
    private Integer lectureCount;
    private List<LectureSummaryResponse> lectures;
    private List<QuizSummaryResponse> quizzes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //course
    private CourseResponse course;
    
    // 사용자 접근성 정보
    private Boolean owned;
    private Boolean hasAccess;
    private Boolean locked;

    public static SectionResponse from(Section section) {
        return SectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse().getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .sequence(section.getSequence())
                .durationMinutes(section.getDurationMinutes())
                .cookiePrice(section.getCookiePrice())
                .discountPercentage(section.getDiscountPercentage())
                .lectureCount(section.getLectureCount())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .course(CourseResponse.from(section.getCourse()))
                .build();
    }

    public static SectionResponse fromWithLectures(Section section) {
        return fromWithLectures(
                section,
                section.getLectures().stream()
                        .map(LectureSummaryResponse::from)
                        .collect(Collectors.toList())
        );
    }

    public static SectionResponse fromWithLectures(Section section, List<LectureSummaryResponse> lectures) {
        return SectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse().getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .sequence(section.getSequence())
                .durationMinutes(section.getDurationMinutes())
                .cookiePrice(section.getCookiePrice())
                .discountPercentage(section.getDiscountPercentage())
                .lectureCount(lectures != null ? lectures.size() : 0)
                .lectures(lectures)
                .quizzes(section.getQuizzes().stream()
                        .map(QuizSummaryResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }

    /**
     * 사용자 접근성을 포함한 섹션 응답 생성
     */
    public static SectionResponse fromWithLecturesForUser(Section section,
                                                          List<LectureSummaryResponse> lectures,
                                                          boolean hasAccess) {
        SectionResponse response = fromWithLectures(section, lectures);
        response.setOwned(hasAccess);
        response.setHasAccess(hasAccess);
        response.setLocked(!hasAccess);
        return response;
    }
}
