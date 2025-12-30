package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자용 쿠키 사용 내역 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CookieAdminUsageResponse {
    private Long id;  // wallet_ledger.id
    private Long userId;  // 사용자 ID
    private String userName;  // 사용자 이름
    private String type;  // 거래 유형 (CHARGE, DEBIT, REFUND, EXPIRE)
    private Integer cookieAmount;  // 쿠키 수량 (DEBIT는 음수)
    private Integer balanceAfter;  // 거래 후 잔액
    private String notes;  // 메모
    private String referenceType;  // 참조 타입 (ORDER, PAYMENT 등)
    private Long referenceId;  // 참조 ID
    private LocalDateTime createdAt;  // 생성 일시
}

