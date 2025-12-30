package com.studyblock.domain.payment.enums;

public enum PaymentType {
    CASH,    // 현금만 결제
    COOKIE,  // 쿠키만 결제
    MIXED    // 혼합 결제 (현금 + 쿠키) - 현재 미지원, 향후 확장용
}
