package com.studyblock.domain.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponIssueResponse {
    private Long couponId;
    private String couponName;
    private Integer totalIssued;
    private Integer successCount;
    private Integer failCount;
    private List<String> failedUserIds;
    private String message;
}
