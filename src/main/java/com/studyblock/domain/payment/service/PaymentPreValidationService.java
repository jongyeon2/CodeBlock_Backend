package com.studyblock.domain.payment.service;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.PaymentValidationRequest;
import com.studyblock.domain.payment.service.validator.CookieBalanceValidationService;
import com.studyblock.domain.payment.service.validator.PaymentCouponValidator;
import com.studyblock.domain.payment.service.validator.PaymentAmountValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 결제 승인 전 검증 전담 서비스
// PaymentService.confirmPayment()에서 토스 승인 전에 호출
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPreValidationService {

    private final CookieBalanceValidationService cookieBalanceValidationService;
    private final PaymentCouponValidator paymentCouponValidator;
    private final PaymentAmountValidationService paymentAmountValidationService;
    private final CourseRepository courseRepository;

    // 결제 승인 전 검증 (토스 승인 전에 호출)
    public void validateBeforeApproval(PaymentConfirmRequest request, Long userId) {
        log.info("결제 승인 전 검증 시작 - userId: {}, paymentKey: {}, orderId: {}",
                userId, request.getPaymentKey(), request.getOrderId());

        // 1. 쿠키/현금 금액 계산
        Long cookieAmount = request.getCookieAmount() != null ? request.getCookieAmount().longValue() : 0L;
        Long totalAmount = request.getAmount() != null ? request.getAmount().longValue() : 0L;
        PaymentAmountValidationService.PaymentAmounts amounts =
                paymentAmountValidationService.calculateAmounts(cookieAmount, totalAmount);

        // 2. 혼합 결제 제한 검증
        paymentAmountValidationService.validatePaymentMethod(
                amounts.getCookieAmount(),
                amounts.getCashAmount()
        );

        // 3. 쿠키 잔액 검증
        cookieBalanceValidationService.validateCookieBalance(userId, amounts.getCookieAmount());

        // 4. 쿠폰 검증 (쿠폰 사용 시) - items 기반 baseSum 계산
        if (request.getUserCouponId() != null && request.getItems() != null && !request.getItems().isEmpty()) {
            // items를 순회해서 쿠폰 할인 전 총액 계산 (baseSum)
            long baseSum = 0L;
            log.info("쿠폰 검증용 baseSum 계산 시작 - items 개수: {}", request.getItems().size());
            
            for (int i = 0; i < request.getItems().size(); i++) {
                PaymentValidationRequest.OrderItemRequest item = request.getItems().get(i);
                try {
                    log.info("Item[{}] 처리 시작 - courseId: {}, quantity: {}, unitPrice: {}",
                            i, item.getCourseId(), item.getQuantity(), item.getUnitPrice());
                    
                    Long courseId = item.getCourseIdAsLong();
                    if (courseId != null) {
                        Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                    "강의를 찾을 수 없습니다. ID: " + courseId
                                ));
                        Integer quantity = item.getQuantity() != null && item.getQuantity() > 0 
                                ? item.getQuantity() : 1;
                        Long discountedPrice = course.getDiscountedPrice(); // 쿠폰 할인 전 가격
                        long itemAmount = (discountedPrice != null ? discountedPrice : 0L) * quantity;
                        baseSum += itemAmount;
                        
                        log.info("Item[{}] 처리 완료 - courseId: {}, title: {}, quantity: {}, discountedPrice: {}, itemAmount: {}, baseSum: {}",
                                i, courseId, course.getTitle(), quantity, discountedPrice, itemAmount, baseSum);
                    } else {
                        log.warn("Item[{}] courseId가 null입니다 - item: {}", i, item);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("강의 조회 실패 - item[{}]: {}, error: {}", i, item, e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error("Item[{}] 처리 중 예외 발생 - item: {}, error: {}", i, item, e.getMessage(), e);
                    throw new IllegalArgumentException("주문 항목 처리 중 오류가 발생했습니다: " + e.getMessage());
                }
            }
            
            log.info("쿠폰 검증용 baseSum 계산 완료 - 총 baseSum: {}", baseSum);
            
            // 쿠폰 검증 (baseSum으로 최소 주문 금액 확인)
            paymentCouponValidator.validateCoupon(userId, request.getUserCouponId(), baseSum);
            log.info("쿠폰 검증 완료 - userId: {}, userCouponId: {}, baseSum: {}",
                    userId, request.getUserCouponId(), baseSum);
        }

        log.info("결제 승인 전 검증 완료 - userId: {}, cookieAmount: {}, cashAmount: {}",
                userId, amounts.getCookieAmount(), amounts.getCashAmount());
    }
}

