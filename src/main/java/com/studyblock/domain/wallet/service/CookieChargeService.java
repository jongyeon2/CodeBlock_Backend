package com.studyblock.domain.wallet.service;

import com.studyblock.domain.activitylog.enums.ActionType;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.payment.service.validator.PaymentCouponValidator;
import com.studyblock.domain.payment.service.CouponProcessingService;
import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.payment.client.TossPaymentClient;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.wallet.dto.CookieChargeRequest;
import com.studyblock.domain.wallet.dto.CookieChargeResponse;
import com.studyblock.domain.payment.entity.CookieBundle;
import com.studyblock.domain.payment.repository.CookieBundleRepository;
import com.studyblock.domain.wallet.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//쿠키 충전 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieChargeService {

private final CookieBundleRepository cookieBundleRepository;
private final UserRepository userRepository;
private final OrderRepository orderRepository;
private final OrderItemRepository orderItemRepository;
private final PaymentRepository paymentRepository;
private final WalletService walletService;
private final TossPaymentClient tossPaymentClient; // POST approval 직접 호출
private final CookieChargeCalculationService calculationService;
private final CookieChargeOrderFactory orderFactory;
private final CookieLedgerManager ledgerManager;
private final ObjectMapper objectMapper;
private final WalletLedgerRepository walletLedgerRepository;
private final PaymentCouponValidator paymentCouponValidator;
private final CouponProcessingService couponProcessingService;
        private final ActivityLogService activityLogService;

        // 쿠키 충전 처리 (토스페이먼츠 연동)
@Transactional
public CookieChargeResponse processChargeWithToss(Long userId, CookieChargeRequest request, String paymentKey, String orderId, Integer amount) {
// 1. 사용자 조회
User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

// 2. 쿠키 번들 조회
CookieBundle bundle = cookieBundleRepository.findById(request.getBundleId())
        .orElseThrow(() -> new IllegalArgumentException("쿠키 번들을 찾을 수 없습니다"));

if (!bundle.getIsActive()) {
        throw new IllegalStateException("비활성화된 쿠키 번들입니다");
}

        // 3. 총 쿠키 수량/금액 계산
int paidCookieQuantity = bundle.getBaseCookieAmount() * request.getQuantity();
int bonusCookieQuantity = bundle.getBonusCookieAmount() * request.getQuantity();
        Integer totalCookieQuantity = paidCookieQuantity + bonusCookieQuantity;
        var calc = calculationService.calculate(bundle, request.getQuantity(), userId, request.getUserCouponId());
        Long totalCashAmount = calc.totalCashAmount;
        Long discount = calc.discount;
        Long discountedAmount = calc.discountedAmount;

        log.info("쿠키 충전 금액 계산 - bundlePrice: {}, quantity: {}, totalCashAmount: {}, userCouponId: {}", 
                bundle.getPrice(), request.getQuantity(), totalCashAmount, request.getUserCouponId());

        // 3-0. 쿠폰 예약(선택)
        CouponProcessingService.CouponProcessingResult couponResult = null;
        if (request.getUserCouponId() != null) {
                couponResult = couponProcessingService.processCouponReservation(request.getUserCouponId(), userId);
        }
        log.info("쿠폰 계산 결과 - totalCash: {}, discount: {}, discounted: {}", totalCashAmount, discount, discountedAmount);

        // 3-1. 금액 검증: 프론트 전달 금액과 번들 가격×수량 일치 여부 확인
        if (amount == null) {
                throw new IllegalArgumentException("결제 금액이 제공되지 않았습니다. amount가 필요합니다.");
        }
        
        if (!discountedAmount.equals(amount.longValue())) {
                log.error("결제 금액 불일치 상세 - 기대금액(할인적용): {}, 요청금액: {}, 원래금액: {}, 할인금액: {}, bundleId: {}, quantity: {}, userCouponId: {}", 
                        discountedAmount, amount, totalCashAmount, discount, request.getBundleId(), request.getQuantity(), request.getUserCouponId());
                throw new IllegalArgumentException(
                        String.format("결제 금액 불일치: 기대금액(할인적용)=%d, 요청금액=%d, 원래금액=%d, 할인금액=%d", 
                                discountedAmount, amount, totalCashAmount, discount)
                );
        }
        
        log.info("금액 검증 통과 - discountedAmount: {}, amount: {}", discountedAmount, amount);

        // 4-0. 멱등: 이미 처리된 paymentKey인지 먼저 확인
        Payment existingPayment = paymentRepository.findByPaymentKey(paymentKey).orElse(null);
        if (existingPayment != null) {
                log.info("🎯 멱등 처리: 이미 처리된 paymentKey - {}", paymentKey);
                // 금액 일치 여부 확인 (요청 금액 및 번들 기준 금액 모두 검증)
                Long existingAmount = existingPayment.getAmount();
                if (existingAmount == null || existingAmount.intValue() != amount
                        || !existingAmount.equals(discountedAmount)) {
                        throw new IllegalStateException("동일 paymentKey에 금액 불일치가 감지되었습니다. 기존: " + existingAmount + ", 요청: " + amount);
                }

                // 이미 처리된 결제인 경우, 누락된 wallet_ledger와 cookie_batch를 확인하고 생성
                Order existingOrder = existingPayment.getOrder();
                if (existingOrder == null) {
                        log.warn("⚠️ 이미 처리된 paymentKey지만 Order가 없습니다. paymentId: {}", existingPayment.getId());
                        throw new IllegalStateException("결제 정보에 주문 정보가 없습니다");
                }
                
                // OrderItem 조회 (쿠키 충전용)
                List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(existingOrder.getId());
                OrderItem cookieOrderItem = orderItems.isEmpty() ? null : orderItems.get(0);
                
                if (cookieOrderItem == null) {
                        log.warn("⚠️ 이미 처리된 결제지만 OrderItem이 없습니다. orderId: {}", existingOrder.getId());
                }
                
                // WalletLedger 확인 (이미 생성되었는지)
                List<com.studyblock.domain.wallet.entity.WalletLedger> existingLedgers = 
                        walletLedgerRepository.findByReferenceTypeAndReferenceId("PAYMENT", existingPayment.getId());
                
                // WalletLedger가 없으면 chargeCookies를 호출 (자동으로 CookieBatch도 생성됨)
                if (existingLedgers.isEmpty()) {
                        log.warn("⚠️ 결제는 완료되었지만 wallet_ledger가 없습니다. 보완 처리 시작... paymentId: {}", existingPayment.getId());
                        
                        // 누락된 쿠키 충전 데이터 생성
                        if (cookieOrderItem == null) {
                                // OrderItem이 없으면 새로 생성
                                cookieOrderItem = OrderItem.builder()
                                        .order(existingOrder)
                                        .course(null)
                                        .section(null)
                                        .itemType(com.studyblock.domain.payment.enums.ItemType.COOKIE_BUNDLE)
                                        .quantity(request.getQuantity())
                                        .unitPrice(bundle.getPrice())
                                        .coupon(null)
                                        .originalAmount(totalCashAmount)
                                        .discountAmount(0L)
                                        .build();
                                cookieOrderItem = orderItemRepository.save(cookieOrderItem);
                                log.info("🎯 누락된 OrderItem 생성 완료 - orderItemId: {}", cookieOrderItem.getId());
                        }
                        
                        // 쿠키 충전 처리 (유료/보너스 분리, 보완 생성)
                        walletService.chargeCookies(
                                userId,
                                paidCookieQuantity,
                                bonusCookieQuantity,
                                existingOrder,
                                existingPayment,
                                cookieOrderItem,
                                String.format("쿠키 번들 충전 (보완): %s", bundle.getName())
                        );
                        log.info("✅ 누락된 쿠키 충전 데이터 보완 완료 - paymentId: {}, orderId: {}", 
                                existingPayment.getId(), existingOrder.getId());
                } else {
                        log.info("✅ 이미 쿠키 충전 데이터가 존재합니다 - ledgerCount: {}", existingLedgers.size());
                }
                
                // 이미 처리된 결제 응답 반환
                Long newBalance = walletService.getCookieBalance(userId);
                return CookieChargeResponse.builder()
                        .orderId(existingOrder.getId())
                        .paymentId(existingPayment.getId())
                        .bundleId(bundle.getId())
                        .bundleName(bundle.getName())
                        .cookieQuantity(totalCookieQuantity)
                        .cashAmount(totalCashAmount.intValue())
                        .newBalance(newBalance)
                        .chargedAt(existingPayment.getCreatedAt() != null ? existingPayment.getCreatedAt() : LocalDateTime.now())
                        .build();
        }

        // 4. 토스페이먼츠 결제 승인 (POST approval - CookieChargeService에 직접 구현)
        TossPaymentResponse tossResponse;
        try {
                log.info("토스페이먼츠 쿠키 충전 승인 요청 - paymentKey: {}, orderId: {}, amount: {}", 
                        paymentKey, orderId, amount);
                
                // POST approval: 토스페이먼츠 결제 승인 API 직접 호출
                tossResponse = tossPaymentClient.confirm(paymentKey, orderId, amount);
                
                log.info("토스페이먼츠 쿠키 충전 승인 성공 - paymentKey: {}, status: {}, method: {}, totalAmount: {}", 
                        paymentKey, tossResponse.getStatus(), tossResponse.getMethod(), tossResponse.getTotalAmount());
        } catch (IllegalStateException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                log.error("토스페이먼츠 쿠키 충전 승인 실패 - paymentKey: {}, orderId: {}, amount: {}, error: {}", 
                        paymentKey, orderId, amount, msg, e);
                
                // 토스에서 이미 처리됨 응답 시 멱등 처리
                if (msg.contains("ALREADY_PROCESSED_PAYMENT")) {
                        log.info("토스에서 이미 처리된 결제 - paymentKey: {}", paymentKey);
                        return buildAlreadyProcessedResponse(userId, paymentKey);
                } else {
                        throw new IllegalArgumentException("쿠키 충전 결제 승인 실패: " + msg);
                }
        } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                log.error("토스페이먼츠 쿠키 충전 승인 중 예상치 못한 오류 - paymentKey: {}, orderId: {}, amount: {}, error: {}", 
                        paymentKey, orderId, amount, msg, e);
                throw new IllegalArgumentException("쿠키 충전 결제 승인 실패: " + msg);
        }

// 5. Order 생성
Order order = orderFactory.createOrder(user, orderId, discountedAmount, tossResponse);

// 6. Payment 생성 (토스페이먼츠 연동)
Payment payment = orderFactory.createPayment(order, paymentKey, orderId, discountedAmount, tossResponse);

        // 쿠키 충전은 정산 대상이 아님(강의 결제 흐름에서 SettlementService 호출)

        // 6. OrderItem 생성 (쿠키 번들 구매 상세)
        OrderItem orderItem = orderFactory.createOrderItem(order, bundle, request.getQuantity(), totalCashAmount, discount);
        // 결제 배분 저장 (쿠키 충전은 단일 항목이므로 전액 배분)
        try {
                com.studyblock.domain.payment.entity.PaymentAllocation alloc = com.studyblock.domain.payment.entity.PaymentAllocation.builder()
                        .payment(payment)
                        .orderItem(orderItem)
                        .amount(discountedAmount.intValue())
                        .build();
                payment.addAllocation(alloc);
                paymentRepository.save(payment);
        } catch (Exception ignore) { }
        log.info("🎯 OrderItem 저장 완료 - orderItemId: {}", orderItem.getId());

        // 7. 쿠키 충전 (유료/보너스 분리)
        ledgerManager.chargeCookies(userId, paidCookieQuantity, bonusCookieQuantity, order, payment, orderItem, bundle);

        // 8. 쿠폰 사용 완료 처리 (RESERVED → USED)
        if (couponResult != null && couponResult.getUserCoupon() != null) {
                try {
                        couponProcessingService.processCouponUsage(couponResult.getUserCoupon());
                        log.info("쿠키 충전 쿠폰 사용 완료 처리 - userCouponId: {}", couponResult.getUserCoupon().getId());
                } catch (Exception e) {
                        log.error("쿠키 충전 쿠폰 사용 완료 처리 중 오류 - userCouponId: {}, error: {}", 
                                couponResult.getUserCoupon().getId(), e.getMessage(), e);
                        // 쿠폰 사용 처리 실패는 쿠키 충전 완료에 영향을 주지 않음 (로그만 남김)
                }
        }

// 9. 충전 후 잔액 조회
Long newBalance = walletService.getCookieBalance(userId);

log.info("쿠키 충전 완료 - userId: {}, bundleId: {}, quantity: {}, totalCookies: {}, totalCash: {}, newBalance: {}", 
        userId, request.getBundleId(), request.getQuantity(), totalCookieQuantity, totalCashAmount, newBalance);

        // 로그 저장
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bundleId", bundle.getId());
        metadata.put("bundleName", bundle.getName());
        metadata.put("quantity", request.getQuantity());
        metadata.put("paidCookies", paidCookieQuantity);
        metadata.put("bonusCookies", bonusCookieQuantity);
        metadata.put("totalCookies", totalCookieQuantity);
        metadata.put("orderId", order.getId());
        metadata.put("paymentId", payment.getId());

        activityLogService.createLog(
                userId,
                ActionType.COOKIE_CHARGE,
                "ORDER",
                order.getId(),
                String.format("%s (%d쿠키) 충전", bundle.getName(), totalCookieQuantity),
                null,
                metadata
        );

return CookieChargeResponse.builder()
        .orderId(order.getId())
        .paymentId(payment.getId())
        .bundleId(bundle.getId())
        .bundleName(bundle.getName())
        .cookieQuantity(totalCookieQuantity)
                .cashAmount(discountedAmount.intValue())
        .newBalance(newBalance)
        .chargedAt(LocalDateTime.now())
        .build();
}

    // 내부: 쿠폰 검증 위임(PaymentCouponValidator의 로직 재사용)
private Coupon invokeCouponValidate(Long userId, Long userCouponId, Long orderAmount) {
        return paymentCouponValidator.validateCoupon(userId, userCouponId, orderAmount);
}

    // 내부: 할인 계산 위임
private Long calculateCouponDiscount(Coupon coupon, Long orderAmount) {
        return paymentCouponValidator.calculateCouponDiscount(coupon, orderAmount);
}

    // 이미 처리된 결제에 대한 응답 구성 (멱등)
        private CookieChargeResponse buildAlreadyProcessedResponse(Long userId, String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .map(p -> {
                        Long orderId = p.getOrder() != null ? p.getOrder().getId() : null;
                        Long bundleId = null; // 쿠키 번들 ID는 OrderItem에서 역추적 가능하면 보강
                        String bundleName = null;
                        Long newBalance = walletService.getCookieBalance(userId);
                        return CookieChargeResponse.builder()
                                .orderId(orderId)
                                .paymentId(p.getId())
                                .bundleId(bundleId)
                                .bundleName(bundleName)
                                .cookieQuantity(0) // 알 수 없으면 0, 프론트는 상태만 확인
                                .cashAmount(p.getAmount() != null ? p.getAmount().intValue() : null)
                                .newBalance(newBalance)
                                .chargedAt(LocalDateTime.now())
                                .build();
                })
                .orElseThrow(() -> new IllegalStateException("이미 처리된 결제이지만 로컬 DB에 결제 정보가 없습니다"));
        }



// 쿠키 충전 처리 (기존 - 토스페이먼츠 연동 없이 즉시 처리)
@Transactional
public CookieChargeResponse processCharge(Long userId, CookieChargeRequest request) {
// 1. 사용자 조회
User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

// 2. 쿠키 번들 조회
CookieBundle bundle = cookieBundleRepository.findById(request.getBundleId())
        .orElseThrow(() -> new IllegalArgumentException("쿠키 번들을 찾을 수 없습니다"));

if (!bundle.getIsActive()) {
        throw new IllegalStateException("비활성화된 쿠키 번들입니다");
}

// 3. 총 쿠키 수량 계산 (기본 + 보너스) - 수량 반영
int paidCookieQuantity = bundle.getBaseCookieAmount() * request.getQuantity();
int bonusCookieQuantity = bundle.getBonusCookieAmount() * request.getQuantity();
Integer totalCookieQuantity = paidCookieQuantity + bonusCookieQuantity;
Long totalCashAmount = bundle.getPrice() * request.getQuantity();

// 4. Order 생성
Order order = Order.builder()
        .user(user)
        .totalAmount(totalCashAmount)
        .paymentType(PaymentType.CASH)
        .orderNumber("cookie-charge-" + System.currentTimeMillis())
        .orderType("COOKIE_CHARGE")
        .build();
order.markAsPaid(); // 결제 완료 처리
order = orderRepository.save(order);

// 5. Payment 생성 (현금 결제)
Payment payment = Payment.builder()
        .order(order)
        .method(com.studyblock.domain.payment.enums.PaymentMethod.CARD)
        .amount(totalCashAmount)
        .paymentKey("cookie-charge-" + System.currentTimeMillis())
        .merchantUid("cookie-charge-" + System.currentTimeMillis())
        .idempotencyKey("cookie-charge-" + System.currentTimeMillis())
        .provider("toss")
           // ← 추가
        .build();
payment.setTossResponse("{}");
payment.capture();
payment = paymentRepository.save(payment);

        // 6. OrderItem 생성 (쿠키 번들 구매 상세)
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .course(null) // 쿠키 충전은 강의가 아님
                .section(null) // 쿠키 충전은 섹션이 아님
                .itemType(com.studyblock.domain.payment.enums.ItemType.COOKIE_BUNDLE) // 쿠키 번들 타입
                .quantity(request.getQuantity())
                .unitPrice(bundle.getPrice())
                .coupon(null) // 쿠폰 없음
                .originalAmount(totalCashAmount)
                .discountAmount(0L)
                .build();
        orderItem = orderItemRepository.save(orderItem);
        log.info("🎯 OrderItem 저장 완료 - orderItemId: {}", orderItem.getId());

        // 7. 쿠키 충전 (유료/보너스 분리)
        walletService.chargeCookies(
                userId,
                paidCookieQuantity,
                bonusCookieQuantity,
                order,
                payment,
                orderItem,
                String.format("쿠키 번들 충전: %s", bundle.getName())
        );

// 7. 충전 후 잔액 조회
Long newBalance = walletService.getCookieBalance(userId);

log.info("쿠키 충전 완료 - userId: {}, bundleId: {}, quantity: {}, totalCookies: {}, totalCash: {}, newBalance: {}", 
        userId, request.getBundleId(), request.getQuantity(), totalCookieQuantity, totalCashAmount, newBalance);

        // 로그 저장
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bundleId", bundle.getId());
        metadata.put("bundleName", bundle.getName());
        metadata.put("quantity", request.getQuantity());
        metadata.put("paidCookies", paidCookieQuantity);
        metadata.put("bonusCookies", bonusCookieQuantity);
        metadata.put("totalCookies", totalCookieQuantity);
        metadata.put("cashAmount", totalCashAmount);
        metadata.put("orderId", order.getId());
        metadata.put("paymentId", payment.getId());

        activityLogService.createLog(
                userId,
                ActionType.COOKIE_CHARGE,
                "ORDER",
                order.getId(),
                String.format("%s (%d쿠키) 충전", bundle.getName(), totalCookieQuantity),
                null,
                metadata
        );

return CookieChargeResponse.builder()
        .orderId(order.getId())
        .paymentId(payment.getId())
        .bundleId(bundle.getId())
        .bundleName(bundle.getName())
        .cookieQuantity(totalCookieQuantity)
        .cashAmount(totalCashAmount.intValue())
        .newBalance(newBalance)
        .chargedAt(LocalDateTime.now())
        .build();
}
}
