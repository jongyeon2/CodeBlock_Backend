package com.studyblock.domain.payment.service.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 결제 금액 검증 전담 서비스
// 단일 책임: 총액 검증, 혼합 결제 제한
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAmountValidationService {

    // 혼합 결제 제한 검증 (현재는 현금 또는 쿠키 중 하나만 가능)
    public void validatePaymentMethod(Long cookieAmount, Long cashAmount) {
        if (cookieAmount != null && cookieAmount > 0L && 
            cashAmount != null && cashAmount > 0L) {
            throw new IllegalStateException(
                "현재 혼합 결제는 지원하지 않습니다. 현금 결제 또는 쿠키 결제 중 하나만 선택해주세요."
            );
        }
    }

    // 총 금액 검증 (서버 계산값 vs 클라이언트 요청값)
    public void validateTotalAmount(long expectedTotal, Long requestTotal) {
        if (expectedTotal != requestTotal) {
            throw new IllegalArgumentException(
                String.format("총 금액이 일치하지 않습니다. 계산값: %d원, 요청값: %d원", 
                    expectedTotal, requestTotal)
            );
        }
    }

    // 쿠키/현금 금액 계산 헬퍼 메서드
    public PaymentAmounts calculateAmounts(Long cookieAmount, Long totalAmount) {
        Long cookie = (cookieAmount != null) ? cookieAmount : 0L;
        Long cash = totalAmount - cookie;
        return new PaymentAmounts(cookie, cash, totalAmount);
    }

    // 금액 정보를 담는 내부 클래스
    public static class PaymentAmounts {
        private final Long cookieAmount;
        private final Long cashAmount;
        private final Long totalAmount;

        public PaymentAmounts(Long cookieAmount, Long cashAmount, Long totalAmount) {
            this.cookieAmount = cookieAmount;
            this.cashAmount = cashAmount;
            this.totalAmount = totalAmount;
        }

        public Long getCookieAmount() {
            return cookieAmount;
        }

        public Long getCashAmount() {
            return cashAmount;
        }

        public Long getTotalAmount() {
            return totalAmount;
        }
    }
}

