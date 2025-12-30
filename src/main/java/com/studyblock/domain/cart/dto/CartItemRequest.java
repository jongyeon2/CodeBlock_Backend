package com.studyblock.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//장바구니 아이템 추가 요청 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    @NotNull(message = "강의 ID는 필수입니다")
    @Positive(message = "강의 ID는 양수여야 합니다")
    private Long courseId;
    
    @NotNull(message = "강의명은 필수입니다")
    private String name;
    
    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 양수여야 합니다")
    private Integer price;
    
    @NotNull(message = "원래 가격은 필수입니다")
    @Positive(message = "원래 가격은 양수여야 합니다")
    private Integer originalPrice;
    
    @Builder.Default
    private Integer discountPercentage = 0;
    
    @Builder.Default
    private Boolean hasDiscount = false;
    
    @Builder.Default
    private Boolean selected = true;
}

