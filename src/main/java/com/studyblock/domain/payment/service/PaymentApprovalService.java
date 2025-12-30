package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.client.TossPaymentClient;
import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApprovalService {

    private final TossPaymentClient tossPaymentClient;

    public TossPaymentResponse approve(PaymentConfirmRequest request) {
        try {
            log.info("토스페이먼츠 결제 승인 요청 - paymentKey: {}, orderId: {}, amount: {}",
                    request.getPaymentKey(), request.getOrderId(), request.getAmount());

            TossPaymentResponse response = tossPaymentClient.confirm(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount()
            );

            log.info("토스페이먼츠 결제 승인 성공 - paymentKey: {}, status: {}, method: {}, totalAmount: {}",
                    response.getPaymentKey(), response.getStatus(), response.getMethod(), response.getTotalAmount());
            return response;
        } catch (IllegalStateException e) {
            log.error("토스페이먼츠 결제 승인 실패 - paymentKey: {}, orderId: {}, error: {}",
                    request.getPaymentKey(), request.getOrderId(), e.getMessage());
            return null;
        }
    }
}


