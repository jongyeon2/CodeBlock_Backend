package com.studyblock.domain.payment.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// 주문 재사용 전담 서비스 (PENDING → PAID 전이)
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReuseService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // 기존 PENDING 주문을 찾아서 PAID로 전이
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public Order reuseOrNull(String orderNumber, TossPaymentResponse tossResponse, String failureReason,
                             Coupon appliedCoupon, Long totalDiscountAmount) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return null;
        }

        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            log.debug("기존 주문을 찾을 수 없음 - orderNumber: {}", orderNumber);
            return null;
        }

        log.info("기존 PENDING 주문 재사용 - orderNumber: {}, 현재 상태: {}", 
                order.getOrderNumber(), order.getStatus());

        // 쿠폰 할인 정보가 있으면 기존 OrderItem들의 할인 정보 업데이트
        if (appliedCoupon != null && totalDiscountAmount != null && totalDiscountAmount > 0) {
            updateOrderItemsDiscount(order, appliedCoupon, totalDiscountAmount);
        }

        // 성공 시에만 상태 전이
        if (failureReason == null) {
            order.markAsPaid();
            order.setRefundableUntil(LocalDateTime.now().plusDays(7));
            
            // OrderItem들의 상태도 PAID로 변경
            List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
            for (OrderItem orderItem : orderItems) {
                if (!orderItem.isPaid()) {
                    orderItem.markAsPaid();
                    orderItemRepository.save(orderItem);
                    log.debug("OrderItem 상태 전이 완료 - PENDING → PAID, orderItemId: {}", orderItem.getId());
                }
            }
            
            log.info("주문 상태 전이 완료 - PENDING → PAID, orderId: {}, orderItemsCount: {}", 
                    order.getId(), orderItems.size());
        }

        // 토스 정보/키 보강
        if (tossResponse != null) {
            order.setTossOrderId(tossResponse.getOrderId());
            order.setOrderName(tossResponse.getOrderName());
        }

        return orderRepository.save(order);
    }

    // 기존 OrderItem들의 쿠폰 할인 정보 업데이트 (비율 분배)
    private void updateOrderItemsDiscount(Order order, Coupon appliedCoupon, Long totalDiscountAmount) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
        if (orderItems.isEmpty()) {
            log.warn("OrderItem이 없습니다 - orderId: {}", order.getId());
            return;
        }

        // 전체 원래 금액 계산
        long totalOriginalAmount = orderItems.stream()
                .mapToLong(item -> item.getOriginalAmount() != null ? item.getOriginalAmount() : 0L)
                .sum();

        if (totalOriginalAmount == 0) {
            log.warn("전체 원래 금액이 0입니다 - orderId: {}", order.getId());
            return;
        }

        log.info("OrderItem 쿠폰 할인 업데이트 시작 - orderId: {}, totalOriginalAmount: {}, totalDiscountAmount: {}", 
                order.getId(), totalOriginalAmount, totalDiscountAmount);

        // 각 OrderItem에 할인 금액 비율 분배
        long savedTotalDiscountAmount = 0L;
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            Long originalAmount = item.getOriginalAmount() != null ? item.getOriginalAmount() : 0L;
            
            Long discountAmount = 0L;
            if (i == orderItems.size() - 1) {
                // 마지막 항목: 나머지 할인 금액 모두 할당 (반올림 오차 보정)
                discountAmount = totalDiscountAmount - savedTotalDiscountAmount;
            } else {
                // 비율 계산: (항목 원래 금액 / 전체 원래 금액) * 전체 할인 금액
                discountAmount = Math.round((double) originalAmount / totalOriginalAmount * totalDiscountAmount);
            }
            savedTotalDiscountAmount += discountAmount;

            // OrderItem의 쿠폰 할인 정보 업데이트
            item.applyCoupon(appliedCoupon, discountAmount);
            orderItemRepository.save(item);

            log.info("OrderItem 쿠폰 할인 업데이트 완료 - orderItemId: {}, originalAmount: {}, discountAmount: {}, finalAmount: {}", 
                    item.getId(), originalAmount, discountAmount, item.getFinalAmount());
        }

        log.info("OrderItem 쿠폰 할인 업데이트 완료 - orderId: {}, totalOriginalAmount: {}, totalDiscountAmount: {}, savedTotalDiscountAmount: {}", 
                order.getId(), totalOriginalAmount, totalDiscountAmount, savedTotalDiscountAmount);
    }
}

