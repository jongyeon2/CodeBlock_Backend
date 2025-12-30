package com.studyblock.domain.wallet.service;

import com.studyblock.domain.payment.client.TossPaymentClient;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieChargeApprovalService {

    private final TossPaymentClient tossPaymentClient;

    public TossPaymentResponse confirm(String paymentKey, String orderId, Integer amount) {
        log.info("토스 결제 승인 - paymentKey: {}, orderId: {}, amount: {}", paymentKey, orderId, amount);
        return tossPaymentClient.confirm(paymentKey, orderId, amount);
    }
}


