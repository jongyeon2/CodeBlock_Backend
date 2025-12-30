package com.studyblock.domain.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieUsageHistoryResponse {

    private Long id;                    // wallet_ledger id
    private Integer cookieAmount;       // 사용한 쿠키 개수 (절댓값)
    private Integer balanceAfter;       // 사용 후 잔액
    private String notes;               // 사용 내역 설명
    private String referenceType;       // 참조 타입 (ORDER, PAYMENT 등)
    private Long referenceId;           // 참조 ID (order_id, payment_id 등)
    private LocalDateTime createdAt;     // 사용 일시
    
    // 주문 정보 (referenceType이 ORDER인 경우)
    private Long orderId;               // 주문 ID
    private String orderNumber;         // 주문 번호
    
    // 주문 항목 정보
    private String itemType;            // COURSE, SECTION, COOKIE_BUNDLE
    private Long courseId;              // 강의 ID (COURSE 또는 SECTION인 경우)
    private String courseTitle;         // 강의명
    private Long sectionId;             // 섹션 ID (SECTION인 경우)
    private String sectionTitle;        // 섹션명
}

