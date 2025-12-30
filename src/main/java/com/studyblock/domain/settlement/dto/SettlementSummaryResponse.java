package com.studyblock.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 강사별 정산 요약 정보
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementSummaryResponse {
    private Long instructorId;
    private String instructorName;
    private Integer pendingAmount; // 환불 기간 중 (정산 대기)
    private Integer eligibleAmount; // 정산 가능 금액
    private Integer settledAmount; // 정산 완료 금액
    private Integer totalAmount; // 전체 금액 (pending + eligible + settled)
    private Long pendingCount; // 정산 대기 건수
    private Long eligibleCount; // 정산 가능 건수
    private Long settledCount; // 정산 완료 건수
}


