package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.dto.PaymentValidationRequest;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingOrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemCreationService orderItemCreationService;

    // 검증 시점에 PENDING 주문과 항목을 생성하고 주문번호를 반환
    @Transactional
    public String createPendingOrder(Long userId, PaymentValidationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 이미 같은 idempotencyKey로 존재하면 재사용
        if (request.getIdempotencyKey() != null) {
            orderRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .ifPresent(existing -> {
                        throw new IllegalStateException("이미 진행 중인 주문이 있습니다");
                    });
        }

        String orderNumber = "ORDER_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 16);

        Long totalAmount = request.getTotalAmount() != null ? request.getTotalAmount() : 0L;

        Order order = Order.builder()
                .user(user)
                .paymentType(PaymentType.CASH)
                .totalAmount(totalAmount)
                .cookieSpent(0)
                .orderNumber(orderNumber)
                .orderType("COURSE_PURCHASE")
                .idempotencyKey(request.getIdempotencyKey())
                .tossOrderId(null)
                .orderName("결제 진행중")
                .customerKey(user.getEmail())
                .build();

        order = orderRepository.save(order);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            orderItemCreationService.createOrderItems(order, request, null, null, userId);
        }

        log.info("PENDING 주문 생성 - orderNumber: {}, items: {}", orderNumber,
                request.getItems() != null ? request.getItems().size() : 0);

        return orderNumber;
    }
}


