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
public class CookieChargeHistoryResponse {

    // cookie_batch 컬럼 기반 응답
    private Long id;                // batch id
    private Long userId;            // user_id
    private Long orderItemsId;      // order_items_id
    private Integer qtyTotal;       // qty_total
    private Integer qtyRemain;      // qty_remain
    private String cookieType;      // cookie_type (PAID/FREE)
    private String source;          // source (PURCHASE/BONUS/...)
    private LocalDateTime expiresAt; // expires_at
    private Boolean isActive;       // is_active
    private LocalDateTime createdAt; // created_at

    // order_items 핵심 컬럼(가용 필드만 매핑)
    private Long orderId;           // orders_id
    private String itemType;        // item_type
    private Long unitAmount;        // unit_amount
    private Long amount;            // amount (OrderItem.amount)
    private Long originalAmount;    // original_amount
    private Long discountAmount;    // discount_amount
    private Long finalAmount;       // final_amount
    private Integer quantity;       // quantity
    private LocalDateTime orderItemCreatedAt; // order_items.created_at
}


