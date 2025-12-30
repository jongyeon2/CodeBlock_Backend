package com.studyblock.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 강의별 정산 통계 정보
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStatisticsResponse {
    private Long courseId;
    private String courseName;
    private Long totalSales;        // 총 판매 건수
    private Integer totalAmount;    // 총 정산 금액 (순수익)
    private Integer avgAmount;      // 평균 정산 금액
    private Integer minAmount;      // 최소 정산 금액
    private Integer maxAmount;      // 최대 정산 금액
    private Long pendingCount;      // 정산 대기 건수
    private Long eligibleCount;     // 정산 가능 건수
    private Long settledCount;      // 정산 완료 건수
}
