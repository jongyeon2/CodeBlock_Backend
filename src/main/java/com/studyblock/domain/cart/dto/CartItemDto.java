package com.studyblock.domain.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//장바구니 아이템 DTO
//응답에 사용되는 장바구니 아이템 정보
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long courseId;
    private String name;
    private Integer price;
    private Integer originalPrice;
    private Integer discountPercentage;
    private Boolean hasDiscount;
    private Boolean selected;
    private LocalDateTime addedAt;
}

