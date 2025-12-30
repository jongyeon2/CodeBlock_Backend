package com.studyblock.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePaymentRequest {
    private Long ledgerId;
    private String bankAccountInfo;
    private String notes;
    private String confirmationNumber; // 확인 번호 (선택사항, 있으면 자동 완료 처리)
}
