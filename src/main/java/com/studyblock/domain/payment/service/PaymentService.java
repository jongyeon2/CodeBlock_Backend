package com.studyblock.domain.payment.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.idempotency.service.IdempotencyKeyService;
import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.PaymentConfirmResponse;
import com.studyblock.domain.payment.dto.PaymentFailureLogRequest;
import com.studyblock.domain.payment.dto.PaymentValidationRequest;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.payment.service.validator.PaymentCouponValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 결제 처리 오케스트레이션 서비스
// 각 전담 서비스들을 조합하여 전체 결제 프로세스 관리
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final IdempotencyKeyService idempotencyKeyService;
    private final ObjectMapper objectMapper;

    // 전담 서비스들
    private final OrderCreationService orderCreationService;
    private final PaymentCreationService paymentCreationService;
    private final CouponProcessingService couponProcessingService;
    private final PaymentApprovalService paymentApprovalService;
    private final PaymentSuccessProcessor paymentSuccessProcessor;
    private final OrderReuseService orderReuseService;
    private final PaymentPreValidationService paymentPreValidationService;
    private final PaymentCouponValidator paymentCouponValidator;

    // 결제 승인 처리 (메인 오케스트레이션)
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request, Long userId,
                                                String ipAddress, String userAgent, String paymentSource) {
    // 1. 사용자 조회
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 1-1. 멱등성 키 검증 및 생성 (결제 전에 먼저 처리)
    String idempotencyKey = request.getPaymentKey(); // paymentKey를 멱등성 키로 사용
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        try {
            // 멱등성 키가 이미 처리되었는지 확인
            boolean alreadyProcessed = idempotencyKeyService.isAlreadyProcessed(userId, idempotencyKey);
            if (alreadyProcessed) {
                // 이미 처리된 요청이면 기존 Order 반환
                Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                        "이미 처리된 결제 요청입니다. 중복 결제를 방지합니다."
                    ));

                // 기존 응답 반환 (캐시된 응답이 있으면 사용)
                String cachedResponse = idempotencyKeyService.getCachedResponse(idempotencyKey);
                if (cachedResponse != null) {
                    log.info("캐시된 응답 반환 - idempotencyKey: {}", idempotencyKey);
                    // TODO: 캐시된 응답을 파싱해서 반환 (필요시 구현)
                }

                // 기존 결제 정보로 응답 생성
                Payment existingPayment = paymentRepository.findByOrder_Id(existingOrder.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다"));

                log.info("중복 결제 요청 차단 - idempotencyKey: {}, orderId: {}",
                        idempotencyKey, existingOrder.getId());

                return PaymentConfirmResponse.builder()
                        .success(true)
                        .message("이미 처리된 결제 요청입니다")
                        .orderId(existingOrder.getOrderNumber())
                        .paymentKey(existingPayment.getPaymentKey())
                        .amount(existingPayment.getAmount() != null ? existingPayment.getAmount().intValue() : 0)
                        .paymentMethod(existingPayment.getMethod() != null ? existingPayment.getMethod().toString() : "카드")
                        .orderDatabaseId(existingOrder.getId())
                        .paidAt(existingOrder.getPaidAt())
                        .build();
            }

            // 멱등성 키 생성 (요청 해시는 선택적)
            idempotencyKeyService.createIdempotencyKey(user, idempotencyKey, null);
        } catch (IllegalStateException e) {
            log.warn("멱등성 키 검증 실패 - idempotencyKey: {}, error: {}", idempotencyKey, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("멱등성 키 처리 중 오류 - idempotencyKey: {}, error: {}", idempotencyKey, e.getMessage());
            // 멱등성 키 처리 실패는 결제 처리에 영향을 주지 않음 (로그만 남김)
        }
    }

    // 2. 결제 승인 전 검증 (토스 승인 전에 모든 검증 완료)
    Long cookieAmount = request.getCookieAmount() != null ? request.getCookieAmount().longValue() : 0L;
    Long cashAmount = request.getAmount() != null ? request.getAmount().longValue() : 0L;
    
    try {
        paymentPreValidationService.validateBeforeApproval(request, userId);
        log.info("결제 승인 전 검증 완료 - userId: {}, cookieAmount: {}, cashAmount: {}",
                userId, cookieAmount, cashAmount);
    } catch (IllegalArgumentException | IllegalStateException e) {
        log.error("결제 승인 전 검증 실패 - userId: {}, error: {}", userId, e.getMessage());
        // 검증 실패 시 쿠폰 롤백은 아직 예약되지 않았으므로 불필요
        throw e;
    }

    // 3. 토스페이먼츠 결제 승인 처리 (검증 완료 후 호출)
    TossPaymentResponse tossResponse = paymentApprovalService.approve(request);
    String failureReason = tossResponse == null ? "토스페이먼츠 결제 승인 실패" : null;

    // 4. 쿠폰 처리 (예약)
    CouponProcessingService.CouponProcessingResult couponResult =
        couponProcessingService.processCouponReservation(request.getUserCouponId(), userId);
    
    // 4-1. 전체 주문 금액 계산 (쿠폰 할인 계산용)
    long totalOriginalAmount = 0L;
    if (request.getItems() != null && !request.getItems().isEmpty()) {
        for (PaymentValidationRequest.OrderItemRequest itemReq : request.getItems()) {
            Integer quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            Long unitPrice = itemReq.getUnitPrice();
            totalOriginalAmount += unitPrice * quantity;
        }
    }
    
    // 4-2. 쿠폰 할인 금액 계산
    Long totalDiscountAmount = 0L;
    Coupon appliedCoupon = couponResult != null ? couponResult.getAppliedCoupon() : null;
    if (appliedCoupon != null && totalOriginalAmount > 0) {
        totalDiscountAmount = paymentCouponValidator.calculateCouponDiscount(appliedCoupon, totalOriginalAmount);
        log.info("전체 주문 쿠폰 할인 계산 - totalOriginalAmount: {}, totalDiscountAmount: {}", 
                totalOriginalAmount, totalDiscountAmount);
    }

    // 5. 주문 생성 또는 기존 PENDING 주문 재사용 (쿠폰 정보 전달)
    Order order = orderReuseService.reuseOrNull(request.getOrderId(), tossResponse, failureReason, 
                                                appliedCoupon, totalDiscountAmount);
    if (order == null) {
        order = orderCreationService.createOrder(user, request, tossResponse, cashAmount, cookieAmount, failureReason);
        orderRepository.save(order);
        log.info("Order 저장 완료 - orderId: {}", order.getId());
    }

    // 6. 결제 성공 시에만 추가 처리
    if (failureReason == null) {
        paymentSuccessProcessor.process(order, request, cookieAmount, cashAmount, userId, couponResult);
    }

    // 7. 결제 생성
    Payment payment = paymentCreationService.createPayment(
            order, request, tossResponse, failureReason, ipAddress, userAgent, paymentSource);
    
    // 주의: Order 저장은 이미 OrderReuseService 또는 OrderCreationService에서 처리됨
    // PaymentSuccessProcessor에서도 updateDiscountInfo 후 저장하므로 여기서는 불필요

    // 8. 결제 실패 시 쿠폰 롤백 및 멱등성 키 실패 처리
    if (failureReason != null) {
        if (couponResult != null && couponResult.getUserCoupon() != null) {
            couponProcessingService.processCouponRollback(couponResult.getUserCoupon());
        }
        
        // 멱등성 키 실패 처리
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String errorSnapshot = objectMapper.writeValueAsString(
                    java.util.Map.of("error", failureReason, "timestamp", java.time.LocalDateTime.now())
                );
                idempotencyKeyService.markAsFailed(idempotencyKey, errorSnapshot);
                log.info("멱등성 키 실패 처리 완료 - idempotencyKey: {}, error: {}", idempotencyKey, failureReason);
            } catch (Exception e) {
                log.error("멱등성 키 실패 처리 중 오류 - idempotencyKey: {}, error: {}", 
                        idempotencyKey, e.getMessage(), e);
            }
        }
        
        throw new IllegalStateException(failureReason);
    }

    // 9. 성공 응답 생성
    String paymentMethod = tossResponse != null && tossResponse.getMethod() != null 
        ? tossResponse.getMethod() 
        : "카드";
    
    PaymentConfirmResponse response = PaymentConfirmResponse.builder()
            .success(true)
            .message("결제가 완료되었습니다")
            .orderId(order.getOrderNumber())
            .paymentKey(payment.getPaymentKey())
            .amount(payment.getAmount() != null ? payment.getAmount().intValue() : 0)
            .paymentMethod(paymentMethod)
            .orderDatabaseId(order.getId())
            .paidAt(order.getPaidAt())
            .build();

    // 10. 멱등성 키 완료 처리 및 응답 캐싱
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        try {
            String responseSnapshot = objectMapper.writeValueAsString(response);
            idempotencyKeyService.markAsUsed(idempotencyKey, responseSnapshot);
            log.info("멱등성 키 완료 처리 및 응답 캐싱 완료 - idempotencyKey: {}", idempotencyKey);
        } catch (Exception e) {
            log.error("멱등성 키 완료 처리 중 오류 - idempotencyKey: {}, error: {}", 
                    idempotencyKey, e.getMessage(), e);
            // 멱등성 키 처리 실패는 결제 성공 응답에 영향을 주지 않음
        }
    }

    return response;
}

    


    // 결제 실패 로그 기록
    public void logPaymentFailure(PaymentFailureLogRequest request) {
    log.warn("[결제 위젯 실패] orderId={}, paymentKey={}, errorCode={}, reason={}, message={}, userAgent={}", 
        request.getOrderId(),
        request.getPaymentKey(),
        request.getErrorCode(),
        request.getReason(),
        request.getMessage(),
        request.getUserAgent()
    );
}
}