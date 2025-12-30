package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.dto.OrderResponse;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    
    // 사용자의 주문 목록 조회
    public List<OrderResponse> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        
        return orders.stream()
                .map(order -> {
                    // 각 주문의 환불 정보 조회
                    List<Refund> refunds = refundRepository.findByOrder_Id(order.getId());
                    return OrderResponse.from(order, refunds);
                })
                .collect(Collectors.toList());
    }
}

