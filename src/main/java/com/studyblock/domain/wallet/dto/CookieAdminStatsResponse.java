package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 쿠키 잔액 통계 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CookieAdminStatsResponse {
    // 전체 사용자 쿠키 잔액 통계 (cookie_batch.qty_remain 합계)
    private Long totalBalance;  // 총 쿠키 잔액 (모든 사용자의 활성 쿠키 배치의 qty_remain 합계)
    private Long totalBalancePaid;  // 유료 쿠키 잔액 (cookie_type이 PAID인 활성 쿠키 배치의 qty_remain 합계)
    private Long totalBalanceFree;  // 무료 쿠키 잔액 (cookie_type이 FREE인 활성 쿠키 배치의 qty_remain 합계)
    private Long totalBalanceExpiredFree;  // 만료된 무료 쿠키 잔액 (cookie_type이 FREE이고 만료된 쿠키 배치의 qty_remain 합계)
    
    // 기간별 충전 통계 (cookie_batch.qty_total 합계)
    private Long totalCharged;  // 전체 충전량 (유료 + 무료 쿠키)
    private Long totalChargedPaid;  // 유료 쿠키 충전량 (source가 PURCHASE이고 cookieType이 PAID인 것들의 qty_total 합계)
    private Long totalChargedFree;  // 무료 쿠키 충전량 (source가 PURCHASE 또는 BONUS이고 cookieType이 FREE인 것들의 qty_total 합계)
    
    // 월별 통계
    private Long monthlyCharged;  // 이번 달 충전 (전체: 유료 + 무료 쿠키)
    private Long monthlyChargedPaid;  // 이번 달 유료 쿠키 충전 (cookie_batch에서 source가 PURCHASE이고 cookieType이 PAID인 것들의 qty_total 합계)
    private Long monthlyChargedFree;  // 이번 달 무료 쿠키 충전 (cookie_batch에서 source가 PURCHASE 또는 BONUS이고 cookieType이 FREE인 것들의 qty_total 합계)
    private Long monthlyUsed;  // 이번 달 사용 (wallet_ledger에서 이번 달 생성되고 type이 DEBIT인 것들의 cookie_amount 합계)
    
    // 통계 기간
    private Integer year;  // 통계 년도
    private Integer month;  // 통계 월
}

