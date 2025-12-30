package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.CourseReview;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
public class CourseReviewResponse {

    private Long id;
    private Integer rating;
    private String content;
    private Boolean lectureSpecific;
    private Long lectureId;
    private String lectureTitle;
    private String authorNickname;
    private String authorProfileImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long courseId;
    private String courseTitle;

    // DTO Projection용 생성자 (JPQL에서 사용)
    public CourseReviewResponse(
            Long id,
            Integer rating,
            String content,
            Boolean lectureSpecific,
            Long lectureId,
            String lectureTitle,
            String authorNickname,
            String authorProfileImage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long courseId,
            String courseTitle
    ) {
        this.id = id;
        this.rating = rating;
        this.content = content;
        this.lectureSpecific = lectureSpecific;
        this.lectureId = lectureId;
        this.lectureTitle = lectureTitle;
        this.authorNickname = authorNickname;
        this.authorProfileImage = authorProfileImage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
    }

    public static CourseReviewResponse from(CourseReview review) {
        return CourseReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .content(review.getContent())
                .lectureSpecific(review.getLectureSpecific())
                .lectureId(review.getLecture() != null ? review.getLecture().getId() : null)
                .lectureTitle(review.getLecture() != null ? review.getLecture().getTitle() : null)
                .authorNickname(review.getUser().getNickname())
                .authorProfileImage(review.getUser().getImg())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .courseId(review.getCourse().getId())
                .courseTitle(review.getCourse().getTitle())
                .build();
    }
}
