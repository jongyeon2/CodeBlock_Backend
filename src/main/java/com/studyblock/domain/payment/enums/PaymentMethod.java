package com.studyblock.domain.payment.enums;

// 토스페이먼츠 결제 수단 타입
public enum PaymentMethod {
    CARD,              // 카드 결제
    VIRTUAL_ACCOUNT,   // 가상계좌
    TRANSFER,          // 계좌이체
    EASY_PAY,          // 간편결제 (토스페이, 네이버페이, 카카오페이 등)
    MOBILE,            // 휴대폰 결제
    GIFT_CERTIFICATE,  // 상품권
    ACCOUNT            // 일반 계좌 (기존 호환성 유지)
}
