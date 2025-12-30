package com.studyblock.domain.coupon.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.studyblock.domain.coupon.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponUpdateRequest {
    private String name;
    private String description;
    private CouponType type;
    private Integer discountValue;
    private Integer minimumAmount;
    private Integer maximumDiscount;
    
    // 날짜 형식 유연하게 처리 (yyyy-MM-dd 또는 yyyy-MM-ddTHH:mm:ss)
    @JsonDeserialize(using = CouponCreateRequest.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime validFrom;
    
    // validUntil은 날짜만 받으면 23:59:59로 설정
    @JsonDeserialize(using = CouponCreateRequest.FlexibleLocalDateTimeDeserializerForUntil.class)
    private LocalDateTime validUntil;
    
    private Integer usageLimit;
    private Boolean isActive;
}
