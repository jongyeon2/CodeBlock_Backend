package com.studyblock.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewSummaryResponse {

    private long totalReviews;
    private double averageRating;

    /**
     * 평점별 리뷰 개수 분포 (배열 형식)
     * 5점부터 1점까지 내림차순으로 정렬
     * 예: [
     *   { "stars": 5, "count": 74, "percentage": 74.0 },
     *   { "stars": 4, "count": 20, "percentage": 20.0 },
     *   { "stars": 3, "count": 5, "percentage": 5.0 },
     *   { "stars": 2, "count": 1, "percentage": 1.0 },
     *   { "stars": 1, "count": 0, "percentage": 0.0 }
     * ]
     */
    private List<RatingDistribution> ratingDistribution;

    /**
     * 추천율 (%)
     * 4-5점 리뷰 비율 (0.0 ~ 100.0)
     */
    private double recommendationRate;

    /**
     * 평점 분포 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistribution {
        private int stars;        // 평점 (1-5)
        private long count;       // 해당 평점의 리뷰 개수
        private double percentage; // 전체 대비 비율 (0.0-100.0)
    }
}
