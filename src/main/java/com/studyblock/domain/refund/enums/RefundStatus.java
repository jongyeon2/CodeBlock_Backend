package com.studyblock.domain.refund.enums;

public enum RefundStatus {
    PENDING,    // 대기 중
    APPROVED,   // 승인됨
    PROCESSED,  // 처리됨
    REJECTED,   // 거부됨
    FAILED      // 실패
}
