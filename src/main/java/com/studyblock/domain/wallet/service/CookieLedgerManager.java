package com.studyblock.domain.wallet.service;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.entity.CookieBundle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieLedgerManager {

    private final WalletService walletService;

    public void chargeCookies(Long userId, int paidCookieQuantity, int bonusCookieQuantity,
                              Order order, Payment payment, OrderItem orderItem, CookieBundle bundle) {
        walletService.chargeCookies(
                userId,
                paidCookieQuantity,
                bonusCookieQuantity,
                order,
                payment,
                orderItem,
                String.format("쿠키 번들 충전: %s", bundle.getName())
        );
    }
}


