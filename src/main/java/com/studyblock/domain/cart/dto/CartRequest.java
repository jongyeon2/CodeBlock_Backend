package com.studyblock.domain.cart.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

//장바구니 저장/업데이트 요청 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRequest {
    // 빈 배열도 허용 (장바구니 비우기용)
    @Valid
    private List<CartItemRequest> cartItems;
    // 선택된 아이템 ID 목록 (결제 시 사용)
    private List<Long> selectedIds;
}

