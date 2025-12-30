package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieChargeHistoryPageResponse {

    // 현재 보유 쿠키 (wallet_balance.amount: 총량 기준)
    private Long balanceAmount;

    // 총 충전 쿠키 (PAID 배치 qty_total 합)
    private Integer totalCharged;

    // 총 사용 쿠키 (배치별 (qty_total - qty_remain) 합)
    private Integer totalUsed;

    // 상세 목록 (배치 + order_items 요약)
    private List<CookieChargeHistoryResponse> items;
}


