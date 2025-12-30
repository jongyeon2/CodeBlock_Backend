package com.studyblock.domain.refund.enums;

// 환불 방법
public enum RefundMethod {
    CARD,             // 카드 환불 (원결제 카드로 환불)
    BANK,             // 계좌 환불 (은행 계좌로 환불)
    VIRTUAL_ACCOUNT,  // 가상계좌 환불
    EASY_PAY,         // 간편결제 환불
    MOBILE            // 휴대폰 결제 환불
}

