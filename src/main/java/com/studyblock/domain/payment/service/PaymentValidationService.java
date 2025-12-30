package com.studyblock.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.payment.dto.PaymentValidationRequest;
import com.studyblock.domain.payment.service.validator.CookieBalanceValidationService;
import com.studyblock.domain.payment.service.validator.PaymentCouponValidator;
import com.studyblock.domain.payment.service.validator.CourseValidationService;
import com.studyblock.domain.payment.service.validator.DailyLimitValidationService;
import com.studyblock.domain.payment.service.validator.IdempotencyKeyValidationService;
import com.studyblock.domain.payment.service.validator.PaymentAmountValidationService;
import com.studyblock.domain.payment.service.validator.UserValidationService;

import java.util.List;

// 결제 검증 오케스트레이션 서비스
// 각 전담 검증 서비스들을 조합하여 전체 결제 검증 프로세스 관리
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentValidationService {

    private final CourseValidationService courseValidationService;
    private final UserValidationService userValidationService;
    private final CookieBalanceValidationService cookieBalanceValidationService;
    private final PaymentCouponValidator paymentCouponValidator;
    private final IdempotencyKeyValidationService idempotencyKeyValidationService;
    private final PaymentAmountValidationService paymentAmountValidationService;
    private final DailyLimitValidationService dailyLimitValidationService;

// ===== 메인 검증 메서드 (Controller에서 호출) =====
public void validatePayment(PaymentValidationRequest request) {
    // 진입 로깅
    try {
        log.info("[결제검증] userId={}, itemsCount={}, couponId={}, cookieAmount={}, totalAmount={}",
                request.getUserId(),
                request.getItems() != null ? request.getItems().size() : 0,
                request.getUserCouponId(),
                request.getCookieAmount(),
                request.getTotalAmount());
    } catch (Exception ignore) { }

    // 0. 기본 검증: 주문 항목이 비어있는지 확인
    if (request.getItems() == null || request.getItems().isEmpty()) {
        throw new IllegalArgumentException("주문 항목이 비어있습니다.");
    }

        // 0-1. 쿠키/현금 금액 계산 및 혼합 결제 제한 검증
        PaymentAmountValidationService.PaymentAmounts amounts = 
                paymentAmountValidationService.calculateAmounts(
                        request.getCookieAmount(), 
                        request.getTotalAmount()
                );
        paymentAmountValidationService.validatePaymentMethod(
                amounts.getCookieAmount(), 
                amounts.getCashAmount()
        );

    // 1. 사용자 검증
        userValidationService.validateUser(request.getUserId());

    long serverItemsSum = 0L; // 서버 기준 합계(항상 서버 가격 기준으로 계산)
    log.info("[결제검증] serverItemsSum 계산 시작 - items 개수: {}", request.getItems().size());

    // 2~4. 각 강의별 검증 (강의 존재, 가격, 중복 구매) + 서버 합계 계산
    List<Long> invalidCourseIds = new java.util.ArrayList<>(); // 찾을 수 없는 강의 ID 목록
    
    for (int i = 0; i < request.getItems().size(); i++) {
        PaymentValidationRequest.OrderItemRequest item = request.getItems().get(i);
        log.info("[결제검증] Item[{}] 처리 시작 - courseId: {}, quantity: {}, unitPrice: {}",
                i, item.getCourseId(), item.getQuantity(), item.getUnitPrice());
        
        Long courseId;
        try {
            courseId = item.getCourseIdAsLong(); // Object를 Long으로 변환
            log.info("[결제검증] Item[{}] courseId 변환 성공 - courseId: {}", i, courseId);
        } catch (IllegalArgumentException e) {
            log.error("[결제검증] Item[{}] courseId 변환 실패 - item: {}, error: {}", i, item, e.getMessage());
            invalidCourseIds.add(null); // null로 표시 (형식 오류)
            continue;
        }
        
        if (courseId == null) {
            log.warn("[결제검증] Item[{}] courseId가 null입니다", i);
            invalidCourseIds.add(null);
            continue;
        }
        
        try {
                // 강의 존재 및 구매 가능 여부 검증
                Course course = courseValidationService.validateCourseExists(courseId);
            int qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;

            // Course는 현금 결제만 허용
                courseValidationService.validatePaymentMethod(amounts.getCookieAmount());

            // 서버 기준 합계 = 할인적용가격 × 수량
            Long discounted = course.getDiscountedPrice();
            long itemAmount = (discounted != null ? discounted : 0L) * qty;
            serverItemsSum += itemAmount;
            
            log.info("[결제검증] Item[{}] 처리 완료 - courseId: {}, title: {}, quantity: {}, discountedPrice: {}, itemAmount: {}, serverItemsSum: {}",
                    i, courseId, course.getTitle(), qty, discounted, itemAmount, serverItemsSum);

                // 중복 구매 검증
                courseValidationService.validateDuplicatePurchase(request.getUserId(), courseId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[결제검증] Item[{}] 강의 검증 실패 - courseId: {}, error: {}", i, courseId, e.getMessage());
            invalidCourseIds.add(courseId);
        }
    }
    
    log.info("[결제검증] serverItemsSum 계산 완료 - 총 serverItemsSum: {}", serverItemsSum);
    
    // 찾을 수 없거나 검증 실패한 강의가 있으면 상세한 오류 메시지 반환
    if (!invalidCourseIds.isEmpty()) {
        String errorMessage;
        if (invalidCourseIds.size() == 1 && invalidCourseIds.get(0) != null) {
            errorMessage = String.format("강의 정보를 불러올 수 없습니다. ID: %s", invalidCourseIds.get(0));
        } else if (invalidCourseIds.size() == 1) {
            errorMessage = "강의 정보를 불러올 수 없습니다. 강의 ID 형식이 올바르지 않습니다.";
        } else {
            List<Long> validIds = invalidCourseIds.stream()
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toList());
            if (validIds.isEmpty()) {
                errorMessage = String.format("강의 정보를 불러올 수 없습니다. %d개 강의의 ID 형식이 올바르지 않습니다.", invalidCourseIds.size());
            } else {
                errorMessage = String.format("다음 강의 정보를 불러올 수 없습니다: %s. 총 %d개 중 %d개 강의를 찾을 수 없습니다.",
                        validIds.toString(), request.getItems().size(), invalidCourseIds.size());
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }

    // 5. 총 금액 검증 (서버 기준 합계(serverItemsSum)를 사용해 검증한다.)
    long baseSum = serverItemsSum;
    log.info("[결제검증] baseSum(serverItemsSum)={}, items 개수: {}", baseSum, request.getItems().size());

    // 6. 쿠키 잔액 검증 (쿠키 결제 시)
        cookieBalanceValidationService.validateCookieBalance(
                request.getUserId(), 
                amounts.getCookieAmount()
        );

    // 7. 쿠폰 검증 + 할인 계산 후 총액 재검증
        log.info("[결제검증] 쿠폰 검증 시작 - userCouponId: {}, baseSum: {}", request.getUserCouponId(), baseSum);
        Coupon coupon = paymentCouponValidator.validateCoupon(
                request.getUserId(), 
                request.getUserCouponId(), 
                baseSum
        );
        long discount = paymentCouponValidator.calculateCouponDiscount(coupon, baseSum);
    long expectedTotal = baseSum - discount;
        
    log.info("[결제검증] userCouponId={}, discount={}, expectedTotal={}, requestTotal={}",
            request.getUserCouponId(), discount, expectedTotal, request.getTotalAmount());
        
        paymentAmountValidationService.validateTotalAmount(expectedTotal, request.getTotalAmount());

    // 8. 멱등성 키 검증 (중복 결제 방지)
        idempotencyKeyValidationService.validateIdempotencyKey(
                request.getUserId(), 
                request.getIdempotencyKey()
        );

    // 9. 일일 한도 검증
        dailyLimitValidationService.validateDailyLimit(
                request.getUserId(), 
                amounts.getCashAmount(), 
                amounts.getCookieAmount()
        );
        
    log.info("✅ [결제검증] 모든 검증 완료");
}
}
