package com.studyblock.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyRevenueDto {
    private String month;      // "1월", "2월"
    private Long amount;       // 수입 (환불 제외)
}

