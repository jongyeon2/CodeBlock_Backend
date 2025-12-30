package com.studyblock.domain.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

//장바구니 조회 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private List<CartItemDto> cartItems;
    private List<Long> selectedIds;
    private LocalDateTime lastUpdated;
}

