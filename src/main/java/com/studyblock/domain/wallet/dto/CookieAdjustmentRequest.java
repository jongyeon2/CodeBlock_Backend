package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//관리자 쿠키 조정 요청 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieAdjustmentRequest {
    private Long targetUserId;      // 대상 사용자 ID
    private Integer adjustAmount;   // 조정할 쿠키 수량 (양수: 증가, 음수: 감소)
    private String reason;          // 조정 사유
}

