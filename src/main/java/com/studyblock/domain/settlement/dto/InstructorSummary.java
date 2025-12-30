package com.studyblock.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorSummary {
    private Long instructorId;
    private String instructorName;
    private Integer pendingAmount;
    private Integer eligibleAmount;
    private Integer settledAmount;
    private Integer paidAmount; // 지급 완료 금액 (SettlementPayment status = COMPLETED)
    private Long pendingCount;
    private Long eligibleCount;
    private Long settledCount;
}