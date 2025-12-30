package com.studyblock.domain.settlement.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 전체 정산 대시보드 정보
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementDashboardResponse {
    private Integer totalPendingAmount; // 전체 정산 대기 금액
    private Integer totalEligibleAmount; // 전체 정산 가능 금액
    private Integer totalSettledAmount; // 전체 정산 완료 금액
    private Integer totalPlatformFee; // 전체 플랫폼 수수료
    private Long totalPendingCount; // 전체 정산 대기 건수
    private Long totalEligibleCount; // 전체 정산 가능 건수
    private Long totalSettledCount; // 전체 정산 완료 건수
    private List<InstructorSummary> instructors;

}


