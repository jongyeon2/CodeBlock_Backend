package com.studyblock.domain.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequest {
    private Long couponId;
    private List<Long> userIds;
}
