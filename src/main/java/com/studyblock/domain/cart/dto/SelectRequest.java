package com.studyblock.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//장바구니 아이템 선택 상태 업데이트 요청 DTO

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectRequest {
    @NotNull(message = "선택 여부는 필수입니다")
    private Boolean selected;
}

