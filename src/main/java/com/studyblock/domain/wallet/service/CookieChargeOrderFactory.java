package com.studyblock.domain.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.payment.entity.CookieBundle;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieChargeOrderFactory {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public Order createOrder(User user, String orderId, long discountedAmount, TossPaymentResponse toss) {
        Order order = Order.builder()
                .user(user)
                .totalAmount(discountedAmount)
                .paymentType(PaymentType.CASH)
                .orderNumber(orderId)
                .orderType("COOKIE_CHARGE")
                .tossOrderId(toss.getOrderId())
                .orderName(toss.getOrderName())
                .customerKey(user.getEmail())
                .build();
        order.markAsPaid();
        return orderRepository.save(order);
    }

    public Payment createPayment(Order order, String paymentKey, String orderId, long discountedAmount, TossPaymentResponse toss) {
        Payment payment = Payment.builder()
                .order(order)
                .method(com.studyblock.domain.payment.enums.PaymentMethod.CARD)
                .amount(discountedAmount)
                .paymentKey(paymentKey)
                .merchantUid(orderId)
                .idempotencyKey(paymentKey)
                .provider("toss")
                .build();
        try {
            payment.setTossResponse(objectMapper != null ? objectMapper.writeValueAsString(toss) : "{}");
        } catch (Exception ignore) {
            payment.setTossResponse("{}");
        }
        payment.capture();
        return paymentRepository.save(payment);
    }

    public OrderItem createOrderItem(Order order, CookieBundle bundle, int quantity, long totalCashAmount, long discount) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .course(null)
                .section(null)
                .itemType(ItemType.COOKIE_BUNDLE)
                .quantity(quantity)
                .unitPrice(bundle.getPrice())
                .coupon(null)
                .originalAmount(totalCashAmount)
                .discountAmount(discount)
                .build();
        return orderItemRepository.save(item);
    }
}


