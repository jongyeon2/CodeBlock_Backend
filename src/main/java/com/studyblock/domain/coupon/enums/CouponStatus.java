package com.studyblock.domain.coupon.enums;

/**
 * 쿠폰 상태 Enum
 */
public enum CouponStatus {
    AVAILABLE,   // 사용 가능 (목록에 표시, 결제 가능)
    RESERVED,    // 예약됨 (결제 진행 중, 다른 세션에서 사용 불가)
    USED,        // 사용됨 (사용 완료, 목록에서 제거)
    EXPIRED      // 만료됨 (기간 만료로 사용 불가)
}
