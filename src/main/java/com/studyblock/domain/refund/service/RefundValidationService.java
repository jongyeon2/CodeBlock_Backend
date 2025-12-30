package com.studyblock.domain.refund.service;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.enums.OrderStatus;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.repository.RefundRepository;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundValidationService {

    private final RefundRepository refundRepository;

    public void validate(User user, Order order) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 주문만 환불할 수 있습니다");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문만 환불할 수 있습니다");
        }
        if (!order.isRefundable()) {
            throw new IllegalStateException("환불 가능 기간이 지났습니다");
        }
        List<Refund> existingRefunds = refundRepository.findActiveRefundsByOrderId(order.getId());
        if (!existingRefunds.isEmpty()) {
            throw new IllegalStateException("이미 환불 요청이 진행 중입니다");
        }
    }
}


