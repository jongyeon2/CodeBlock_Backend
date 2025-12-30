package com.studyblock.domain.payment.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.enrollment.event.PaymentCompletedEvent;
import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.payment.service.CouponProcessingService;
import com.studyblock.domain.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSuccessProcessor {

    private final OrderItemCreationService orderItemCreationService;
    private final DailyLimitService dailyLimitService;
    private final SettlementService settlementService;
    private final PaymentCookieService paymentCookieService;
    private final CouponProcessingService couponProcessingService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public void process(Order order,
                        PaymentConfirmRequest request,
                        Long cookieAmount,
                        Long cashAmount,
                        Long userId,
                        CouponProcessingService.CouponProcessingResult couponResult) {
        log.info("결제 성공 처리 시작 - request items: {}", request.getItems());

        if ((request.getItems() == null || request.getItems().isEmpty()) &&
                (order.getOrderItems() == null || order.getOrderItems().isEmpty())) {
            log.warn("OrderItem이 없습니다 - 강의 결제가 아닌 경우");
            return;
        }
        try {
            // 쿠폰 사용 완료 처리 (RESERVED → USED)
            if (couponResult != null && couponResult.getUserCoupon() != null) {
                couponProcessingService.processCouponUsage(couponResult.getUserCoupon());
                log.info("결제 성공 쿠폰 사용 완료 처리 - userCouponId: {}", 
                        couponResult.getUserCoupon().getId());
            }

            int totalDiscountAmount;
            int couponCountForOrder;
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                Coupon appliedCoupon = couponResult != null ? couponResult.getAppliedCoupon() : null;
                UserCoupon userCoupon = couponResult != null ? couponResult.getUserCoupon() : null;
                totalDiscountAmount = orderItemCreationService.createOrderItems(
                        order, request, appliedCoupon, userCoupon, userId);
                couponCountForOrder = appliedCoupon != null ? 1 : 0;
            } else {
                totalDiscountAmount = order.getOrderItems().stream()
                        .mapToInt(oi -> oi.getDiscountAmount().intValue())
                        .sum();
                boolean anyCoupon = order.getOrderItems().stream().anyMatch(oi -> oi.getCoupon() != null);
                couponCountForOrder = anyCoupon ? 1 : 0;
            }

            // 주문 할인 정보 업데이트
            order.updateDiscountInfo(totalDiscountAmount, couponCountForOrder);
            orderRepository.save(order); // 할인 정보 업데이트 후 저장

            // 결제 완료 시 OrderItem들의 상태를 PAID로 변경
            if (order.isPaid()) {
                java.util.List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
                for (OrderItem orderItem : orderItems) {
                    if (!orderItem.isPaid()) {
                        orderItem.markAsPaid();
                        orderItemRepository.save(orderItem);
                        log.debug("OrderItem 상태 전이 완료 - PENDING → PAID, orderItemId: {}", orderItem.getId());
                    }
                }
                log.info("결제 완료 후 OrderItem 상태 변경 완료 - orderId: {}, orderItemsCount: {}", 
                        order.getId(), orderItems.size());
            }

            // 쿠키 차감
            paymentCookieService.deductCookies(userId, cookieAmount, order);

            // 일일 한도 업데이트
            dailyLimitService.updateDailyLimit(userId, cashAmount.intValue(), cookieAmount.intValue());

            // 정산 레코드 생성
            settlementService.createSettlementLedgers(order.getId());

            // 결제 완료 이벤트 발행 (수강 등록/섹션 소유권 부여)
            publishPaymentCompletedEvent(order);

        } catch (Exception e) {
            log.error("결제 성공 처리 중 오류 발생 - orderId: {}, error: {}", order.getId(), e.getMessage(), e);
            throw new IllegalStateException("결제 성공 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 이벤트 발행
     * 별도 트랜잭션에서 수강 등록/섹션 소유권이 생성되도록 이벤트를 발행합니다.
     *
     * @param order 결제 완료된 주문
     */
    private void publishPaymentCompletedEvent(Order order) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(this, order);
            eventPublisher.publishEvent(event);
            log.info("결제 완료 이벤트 발행 완료 - orderId: {}, orderNumber: {}",
                    order.getId(), order.getOrderNumber());
        } catch (Exception e) {
            log.error("결제 완료 이벤트 발행 실패 - orderId: {}, error: {}",
                    order.getId(), e.getMessage(), e);
            // 이벤트 발행 실패는 결제를 실패시키지 않음 (수동 처리 가능)
        }
    }

}


