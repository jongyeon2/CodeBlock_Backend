package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCookieService {

    private final WalletService walletService;

    public void validateCookieBalance(Long userId, Long cookieAmount) {
        if (cookieAmount != null && cookieAmount > 0L) {
            if (!walletService.hasSufficientBalance(userId, cookieAmount)) {
                throw new IllegalStateException("쿠키 잔액이 부족합니다");
            }
        }
    }

    public void deductCookies(Long userId, Long cookieAmount, Order order) {
        if (cookieAmount == null || cookieAmount <= 0L) return;
        try {
            walletService.deductCookies(
                userId,
                cookieAmount.intValue(),
                order,
                null,
                String.format("주문 %s - 강의 구매", order.getOrderNumber())
            );
            log.info("쿠키 차감 완료 - userId: {}, orderId: {}, cookieAmount: {}",
                    userId, order.getId(), cookieAmount);
        } catch (Exception e) {
            log.error("쿠키 차감 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("쿠키 차감 실패: " + e.getMessage(), e);
        }
    }
}


