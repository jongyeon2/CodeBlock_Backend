package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCourseResponse {

    private Long id;                        //id
    private String title;                   //코스 제목
    private String summary;                 //코스 요약
    //private List<String> categories;      //코스 카테고리
    //private String thumbnailUrl;          //코스 썸네일 이미지
    private CourseLevel level;              //코스 레벨
    private Integer discountPercentage;     //코스 할인율
    private String thumbnailUrl;            //노출용 썸네일
    private String thumbnailOriginalUrl;    //원본 썸네일(필요 시)
    private String instructorName;          //강사명
    private Double averageRating;           // 평균 평점
    private Long reviewCount;               // 리뷰 개수
    private Long enrollmentCount;           // 수강인원
    private Long discountedPrice;           //코스 할인된금액
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SearchCourseResponse from(Course course) {
        return SearchCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .summary(course.getSummary())
                // .category(course.getCategory())
                // .thumbnailUrl(course.getThumbnailUrl())
                .thumbnailUrl(course.getThumbnailUrl())
                .thumbnailOriginalUrl(course.getThumbnailUrl())
                .level(course.getLevel())
                .discountPercentage(course.getDiscountPercentage())
                .discountedPrice(course.getDiscountedPrice())
                .instructorName(course.getInstructorName())
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .enrollmentCount(course.getEnrollmentCount())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
