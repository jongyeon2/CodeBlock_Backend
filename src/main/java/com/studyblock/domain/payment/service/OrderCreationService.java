package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

//주문생성 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    //주문생성
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public Order createOrder(User user, PaymentConfirmRequest request, TossPaymentResponse tossResponse, 
                        Long cashAmount, Long cookieAmount, String failureReason) {

        // 결제 타입 결정
        PaymentType paymentType = determinePaymentType(cashAmount, cookieAmount);
            Order order = Order.builder()
                .user(user)
                .paymentType(paymentType)
                .totalAmount(cashAmount + cookieAmount)
                .cookieSpent(cookieAmount.intValue())
                .orderNumber(request.getOrderId())
                .orderType("COURSE_PURCHASE")
                .idempotencyKey(request.getPaymentKey())
                .tossOrderId(tossResponse != null ? tossResponse.getOrderId() : request.getOrderId())
                .orderName(tossResponse != null ? tossResponse.getOrderName() : "결제 실패")
                .customerKey(user.getEmail())
                .build();

        // 성공 시에만 결제 완료 처리
        if (failureReason == null) {
            order.markAsPaid();
            order.setRefundableUntil(LocalDateTime.now().plusDays(7));
        }

        log.info("Order 생성 완료 - orderNumber: {}, totalAmount: {}, paymentType: {}", 
                order.getOrderNumber(), order.getTotalAmount(), order.getPaymentType());

        return order;
    }

    //결제 타입 결정
    private PaymentType determinePaymentType(Long cashAmount, Long cookieAmount) {
        if (cookieAmount > 0 && cashAmount > 0) {
            return PaymentType.MIXED; // 혼합 결제 (향후 지원)
        } else if (cookieAmount > 0) {
            return PaymentType.COOKIE; // 쿠키만
        } else {
            return PaymentType.CASH; // 현금만
        }
    }
}
