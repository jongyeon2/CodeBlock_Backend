package com.studyblock.domain.refund.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.enums.RefundStatus;
import com.studyblock.domain.refund.repository.RefundRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

private final RefundRepository refundRepository;
private final UserRepository userRepository;
private final OrderRepository orderRepository;
private final PaymentRepository paymentRepository;
private final RefundValidationService refundValidationService;
private final RefundExecutionService refundExecutionService;
private final RefundIdempotencyHelper idempotencyHelper;

    // 1. 환불 요청 및 처리 (사용자용)
    @Transactional
    public Refund requestRefund(Long userId, Long orderId, String reason, String idempotencyKey) {
    // 1-1. 엔티티 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));

    // 1-2. 멱등성 키 검증 (중복 요청 방지)
    Refund existingRefund = idempotencyHelper.checkDuplicateRequest(userId, idempotencyKey);
    if (existingRefund != null) {
        return existingRefund;
    }

    // 1-3. 환불 검증
    refundValidationService.validate(user, order);

    // 1-4. Payment 조회
    List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
    if (payments.isEmpty()) {
        throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다");
    }
    Payment payment = payments.get(0); // 첫 번째 결제 정보 사용

    // 1-5. 환불 금액 계산 (쿠폰 할인 금액 제외한 실제 결제 금액)
    Integer refundAmountCash = 0;
    Integer refundAmountCookie = 0;

    // 실제 결제 금액 = 원래 금액 - 쿠폰 할인 금액
    Long totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : 0L;
    Integer totalDiscountAmount = order.getTotalDiscountAmount() != null ? order.getTotalDiscountAmount() : 0;
    Long actualPaidAmount = totalAmount - totalDiscountAmount;

    if (order.getPaymentType() == PaymentType.CASH) {
        // 현금 결제: 쿠폰 할인 후 실제 결제한 금액 환불
        refundAmountCash = actualPaidAmount.intValue();
        log.info("전체 환불 - 현금 결제: 원래금액={}, 쿠폰할인={}, 실제결제금액={}, 환불금액={}", 
                totalAmount, totalDiscountAmount, actualPaidAmount, refundAmountCash);
    } else if (order.getPaymentType() == PaymentType.COOKIE) {
        // 쿠키 결제: 쿠키 환불
        refundAmountCookie = order.getCookieSpent() != null ? order.getCookieSpent() : 0;
        log.info("전체 환불 - 쿠키 결제: 쿠키환불={}", refundAmountCookie);
    } else if (order.getPaymentType() == PaymentType.MIXED) {
        // 혼합 결제 (현재 미지원, 향후 확장용)
        // 현재는 결제 검증에서 혼합 결제를 차단하므로 이 코드는 실행되지 않음
        Integer cookieSpent = order.getCookieSpent() != null ? order.getCookieSpent() : 0;
        refundAmountCash = (int) (actualPaidAmount - cookieSpent);
        refundAmountCookie = cookieSpent;
        log.warn("MIXED 결제 타입 환불 요청 - orderId: {} (현재 미지원), 원래금액={}, 쿠폰할인={}, 실제결제금액={}, 현금환불={}, 쿠키환불={}", 
                orderId, totalAmount, totalDiscountAmount, actualPaidAmount, refundAmountCash, refundAmountCookie);
    }

    // 1-6. Refund 생성 (실제 결제 금액 기준)
    // 전체 환불의 경우 모든 OrderItem을 환불 처리 (order_id로 OrderItem 조회 가능)
    Refund refund = Refund.builder()
        .user(user)
        .order(order)
        .payment(payment)
        .amount(actualPaidAmount.intValue())
        .idempotencyKey(idempotencyKey)
        .reason(reason)
        .refundRoute(order.getPaymentType() == PaymentType.COOKIE ? "COOKIE" : "CASH")
        .refundAmountCash(refundAmountCash)
        .refundAmountCookie(refundAmountCookie)
        .build();

    refundRepository.save(refund);
    // Propagation.REQUIRES_NEW로 별도 트랜잭션에서 실행되므로
    // Refund 저장 후 flush하여 즉시 커밋되도록 함
    refundRepository.flush();

    log.info("환불 요청 생성 - refundId: {}, userId: {}, orderId: {}, idempotencyKey: {}",
            refund.getId(), userId, orderId, idempotencyKey);

    // 1-7. 즉시 환불 처리 (현재는 자동 승인)
    // 전체 환불 시 orderItemIds는 null (모든 OrderItem 환불)
    idempotencyHelper.createIdempotencyKey(user, idempotencyKey);

    try {
        refundExecutionService.processRefund(refund, null); // 전체 환불은 null
        idempotencyHelper.markAsUsed(idempotencyKey);
    } catch (Exception e) {
        log.error("환불 처리 실패 - refundId: {}, error: {}", refund.getId(), e.getMessage());
        idempotencyHelper.markAsFailed(idempotencyKey, "환불 처리 실패: " + e.getMessage());
        throw e;
    }

    return refund;
}

    // 1-a. 부분환불 요청 및 처리 (order_items 또는 금액 기준)
    @Transactional
    public Refund requestPartialRefund(Long userId, Long orderId, java.util.List<Long> orderItemIds, Integer partialAmount, String reason) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));

    refundValidationService.validate(user, order);

    java.util.List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
    if (payments.isEmpty()) throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다");
    Payment payment = payments.get(0);

    String idemKey = payment.getPaymentKey() + ":refund:partial";
    
    // 멱등성 키 검증 (중복 요청 방지)
    Refund duplicateRefund = idempotencyHelper.checkDuplicateRequest(userId, idemKey);
    if (duplicateRefund != null) {
        throw new IllegalStateException("이미 환불 요청이 접수되었거나 처리 중입니다");
    }
    
    // 멱등성 키 생성
    idempotencyHelper.createIdempotencyKey(user, idemKey);

    // 금액 산정: order_items 합산 (쿠폰 할인 반영된 최종 금액 사용)
    long targetAmount = 0L;
    if (orderItemIds != null && !orderItemIds.isEmpty()) {
        // 서버에서 해당 order_items 금액 합산 (쿠폰 할인 반영된 최종 금액 사용)
        java.util.List<com.studyblock.domain.payment.entity.OrderItem> items = order.getOrderItems();
        for (com.studyblock.domain.payment.entity.OrderItem oi : items) {
            if (orderItemIds.contains(oi.getId())) {
                // getTotalFinalAmount(): 수량 반영된 최종 금액 (쿠폰 할인 포함)
                targetAmount += oi.getTotalFinalAmount() != null ? oi.getTotalFinalAmount() : 0L;
            }
        }
        log.info("부분환불 - orderItemIds로 계산: 선택항목={}, 원래합계={}, 쿠폰할인반영후합계={}", 
                orderItemIds.size(), 
                items.stream()
                    .filter(oi -> orderItemIds.contains(oi.getId()))
                    .mapToLong(oi -> oi.getOriginalAmount() != null ? oi.getOriginalAmount() * oi.getQuantity() : 0L)
                    .sum(),
                targetAmount);
    }
    if (partialAmount != null && partialAmount > 0) {
        // partialAmount가 직접 제공된 경우: 그대로 사용 (이미 쿠폰 할인 반영된 금액으로 가정)
        targetAmount = partialAmount;
        log.info("부분환불 - partialAmount 직접 제공: 환불금액={}", targetAmount);
    }
    if (targetAmount <= 0) {
        throw new IllegalArgumentException("부분환불 금액이 올바르지 않습니다");
    }

    // 환불 엔티티 생성(부분환불 금액으로)
    // 부분 환불 시 orderItemIds는 RefundExecutionService에 직접 전달 (order_id로 OrderItem 조회 가능)
    Refund refund = Refund.builder()
            .user(user)
            .order(order)
            .payment(payment)
            .amount((int) targetAmount)
            .reason(reason)
            .refundRoute("CASH")
            .refundAmountCash((int) targetAmount)
            .refundAmountCookie(0)
            .build();
    refundRepository.save(refund);

    // 즉시 처리: Toss partial cancel 호출, 상태 갱신 등은 processRefund에서 공용 처리
    // 부분 환불 시 orderItemIds를 직접 전달 (전체 환불은 null)
    try {
        refundExecutionService.processRefund(refund, orderItemIds);
        idempotencyHelper.markAsUsed(idemKey);
    } catch (Exception e) {
        log.error("부분 환불 처리 실패 - refundId: {}, error: {}", refund.getId(), e.getMessage());
        idempotencyHelper.markAsFailed(idemKey, "부분 환불 처리 실패: " + e.getMessage());
        throw e;
    }
    
    return refund;
}


    // 4. 사용자의 환불 내역 조회
    @Transactional(readOnly = true)
    public List<Refund> getUserRefunds(Long userId) {
        return refundRepository.findByUser_Id(userId);
    }

    // 4-1. 사용자의 환불 내역 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<Refund> getUserRefunds(Long userId, Pageable pageable) {
        return refundRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    // 5. 환불 상세 조회
    @Transactional(readOnly = true)
    public Refund getRefund(Long refundId) {
    return refundRepository.findById(refundId)
        .orElseThrow(() -> new IllegalArgumentException("환불 정보를 찾을 수 없습니다"));
}

    // 6. 주문의 환불 내역 조회
    @Transactional(readOnly = true)
    public List<Refund> getOrderRefunds(Long orderId) {
    return refundRepository.findByOrder_Id(orderId);
}

    // 7. 대기 중인 환불 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    public List<Refund> getPendingRefunds() {
        return refundRepository.findByStatus(RefundStatus.PENDING);
    }

    // 7-1. 대기 중인 환불 목록 조회 (페이징, 관리자용)
    @Transactional(readOnly = true)
    public Page<Refund> getPendingRefunds(Pageable pageable) {
        return refundRepository.findByStatusOrderByCreatedAtDesc(RefundStatus.PENDING, pageable);
    }

    // 10. 전체 환불 목록 조회 (관리자용) - Order fetch join 포함
    @Transactional(readOnly = true)
    public List<Refund> getAllRefunds() {
        // Order fetch join으로 조회하여 LazyInitializationException 방지
        return refundRepository.findAllWithOrder();
    }

    // 11. 상태별 환불 목록 조회 (관리자용) - Order fetch join 포함
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByStatus(RefundStatus status) {
        if (status == null) {
            return refundRepository.findAllWithOrder();
        }
        // Order fetch join으로 조회하여 LazyInitializationException 방지
        return refundRepository.findByStatusWithOrder(status);
    }

    // 8. 환불 승인 (관리자용 - 수동 승인 필요 시)
    @Transactional
    public void approveRefund(Long refundId, Long adminId) {
    Refund refundEntity = refundRepository.findById(refundId)
        .orElseThrow(() -> new IllegalArgumentException("환불 정보를 찾을 수 없습니다"));

    refundEntity.approve(adminId);
    refundRepository.save(refundEntity);

    // 승인 후 실제 환불 처리
    // 관리자 승인의 경우 전체 환불로 처리 (null 전달)
    refundExecutionService.processRefund(refundEntity, null);

    log.info("환불 승인 완료 - refundId: {}, adminId: {}", refundId, adminId);
}

    // 9. 환불 거부 (관리자용)
    @Transactional
    public void rejectRefund(Long refundId, Long adminId, String reason) {
    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new IllegalArgumentException("환불 정보를 찾을 수 없습니다"));

    refund.reject(adminId, reason);
    refundRepository.save(refund);

    log.info("환불 거부 완료 - refundId: {}, adminId: {}, reason: {}", 
            refundId, adminId, reason);
}
}
