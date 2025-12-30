package com.studyblock.domain.payment.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.CouponUsageLog;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.CouponUsageLogRepository;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.PaymentValidationRequest;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

//주문아이템생성 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemCreationService {

    private final CourseRepository courseRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponUsageLogRepository couponUsageLogRepository;

    //주문아이템들 생성 (PaymentConfirmRequest용)
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public int createOrderItems(Order order, PaymentConfirmRequest request,
                            Coupon appliedCoupon, UserCoupon userCoupon, Long userId) {
        log.info("OrderItem 생성 시작 - items count: {}", request.getItems().size());
        
        // 1단계: 전체 원래 금액 계산
        long totalOriginalAmount = 0L;
        java.util.List<ItemInfo> itemInfos = new java.util.ArrayList<>();
        
        for (PaymentValidationRequest.OrderItemRequest itemReq : request.getItems()) {
            Integer quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            Long unitPrice = itemReq.getUnitPrice();
            Long originalAmount = unitPrice * quantity;
            totalOriginalAmount += originalAmount;
            
            itemInfos.add(new ItemInfo(itemReq, originalAmount));
        }
        
        // 2단계: 전체 주문 금액에 대해 쿠폰 할인 계산
        long totalDiscountAmount = 0L;
        if (appliedCoupon != null && totalOriginalAmount > 0) {
            totalDiscountAmount = calculateCouponDiscount(appliedCoupon, totalOriginalAmount);
            log.info("전체 주문 쿠폰 할인 계산 - totalOriginalAmount: {}, totalDiscountAmount: {}", 
                    totalOriginalAmount, totalDiscountAmount);
        }
        
        // 3단계: 각 OrderItem 생성 및 할인 금액 비율 분배
        int savedTotalDiscountAmount = 0;
        
        for (int i = 0; i < itemInfos.size(); i++) {
            ItemInfo itemInfo = itemInfos.get(i);
            PaymentValidationRequest.OrderItemRequest itemReq = itemInfo.itemReq;
            Long originalAmount = itemInfo.originalAmount;
            
            Long courseId = itemReq.getCourseIdAsLong();
            log.info("Course 조회 시도 - courseId: {}", courseId);

            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "강의를 찾을 수 없습니다. ID: " + courseId
                ));

            log.info("Course 조회 성공 - courseId: {}, title: {}", courseId, course.getTitle());

            Integer quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            Long unitPrice = itemReq.getUnitPrice();
            
            // 각 항목에 할인 금액 비율 분배
            Long discountAmount = 0L;
            if (appliedCoupon != null && totalOriginalAmount > 0 && totalDiscountAmount > 0) {
                // 비율 계산: (항목 원래 금액 / 전체 원래 금액) * 전체 할인 금액
                if (i == itemInfos.size() - 1) {
                    // 마지막 항목: 나머지 할인 금액 모두 할당 (반올림 오차 보정)
                    discountAmount = totalDiscountAmount - savedTotalDiscountAmount;
                } else {
                    discountAmount = Math.round((double) originalAmount / totalOriginalAmount * totalDiscountAmount);
                }
                savedTotalDiscountAmount += discountAmount.intValue();
            }

            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .course(course)
                .section(null)
                .itemType(com.studyblock.domain.payment.enums.ItemType.COURSE)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .coupon(appliedCoupon)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .build();

            log.info("OrderItem 생성 완료 - courseId: {}, quantity: {}, unitPrice: {}, originalAmount: {}, discountAmount: {}, finalAmount: {}", 
                     courseId, quantity, unitPrice, originalAmount, discountAmount, orderItem.getFinalAmount());

            orderItemRepository.save(orderItem);

            log.info("OrderItem 저장 완료 - orderItemId: {}", orderItem.getId());

            // 쿠폰 사용 로그 기록
            if (appliedCoupon != null && userCoupon != null && discountAmount > 0L) {
                CouponUsageLog couponUsageLog = CouponUsageLog.builder()
                    .userCoupon(userCoupon)
                    .order(order)
                    .orderItem(orderItem)
                    .discountAmount(discountAmount.intValue())
                    .usedAt(LocalDateTime.now())
                    .build();
                couponUsageLogRepository.save(couponUsageLog);

                log.info("쿠폰 사용 로그 기록 완료 - userId: {}, orderId: {}, orderItemId: {}, discountAmount: {}", 
                        userId, order.getId(), orderItem.getId(), discountAmount);
            }
        }

        log.info("전체 주문 쿠폰 할인 분배 완료 - totalOriginalAmount: {}, totalDiscountAmount: {}, savedTotalDiscountAmount: {}", 
                totalOriginalAmount, totalDiscountAmount, savedTotalDiscountAmount);
        
        return (int) totalDiscountAmount;
    }

    //주문아이템들 생성 + 배분 저장 (결제 승인 시 Payment와 함께 호출)
    @Transactional
    public int createOrderItems(Order order, com.studyblock.domain.payment.entity.Payment payment, PaymentConfirmRequest request,
                                Coupon appliedCoupon, UserCoupon userCoupon, Long userId) {
        int totalDiscountAmount = createOrderItems(order, request, appliedCoupon, userCoupon, userId);
        // 각 OrderItem에 대해 배분 저장 (원가-할인 = 최종 금액)
        for (OrderItem oi : order.getOrderItems()) {
            long lineAmount = (oi.getOriginalAmount() != null ? oi.getOriginalAmount() : 0L)
                    - (oi.getDiscountAmount() != null ? oi.getDiscountAmount() : 0L);
            if (lineAmount < 0) lineAmount = 0;
            com.studyblock.domain.payment.entity.PaymentAllocation alloc = com.studyblock.domain.payment.entity.PaymentAllocation.builder()
                    .payment(payment)
                    .orderItem(oi)
                    .amount((int) lineAmount)
                    .build();
            payment.addAllocation(alloc);
        }
        return totalDiscountAmount;
    }

    //주문아이템들 생성 (PaymentValidationRequest용)
    @Transactional
    public int createOrderItems(Order order, PaymentValidationRequest request,
                            Coupon appliedCoupon, UserCoupon userCoupon, Long userId) {
        log.info("OrderItem 생성 시작 - items count: {}", request.getItems().size());
        
        // 1단계: 전체 원래 금액 계산
        long totalOriginalAmount = 0L;
        java.util.List<ItemInfo> itemInfos = new java.util.ArrayList<>();
        
        for (PaymentValidationRequest.OrderItemRequest itemReq : request.getItems()) {
            Integer quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            Long unitPrice = itemReq.getUnitPrice();
            Long originalAmount = unitPrice * quantity;
            totalOriginalAmount += originalAmount;
            
            itemInfos.add(new ItemInfo(itemReq, originalAmount));
        }
        
        // 2단계: 전체 주문 금액에 대해 쿠폰 할인 계산
        long totalDiscountAmount = 0L;
        if (appliedCoupon != null && totalOriginalAmount > 0) {
            totalDiscountAmount = calculateCouponDiscount(appliedCoupon, totalOriginalAmount);
            log.info("전체 주문 쿠폰 할인 계산 - totalOriginalAmount: {}, totalDiscountAmount: {}", 
                    totalOriginalAmount, totalDiscountAmount);
        }
        
        // 3단계: 각 OrderItem 생성 및 할인 금액 비율 분배
        int savedTotalDiscountAmount = 0;
        
        for (int i = 0; i < itemInfos.size(); i++) {
            ItemInfo itemInfo = itemInfos.get(i);
            PaymentValidationRequest.OrderItemRequest itemReq = itemInfo.itemReq;
            Long originalAmount = itemInfo.originalAmount;
            
            Long courseId = itemReq.getCourseIdAsLong();
            log.info("Course 조회 시도 - courseId: {}", courseId);

            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "강의를 찾을 수 없습니다. ID: " + courseId
                ));

            log.info("Course 조회 성공 - courseId: {}, title: {}", courseId, course.getTitle());

            Integer quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            Long unitPrice = itemReq.getUnitPrice();
            
            // 각 항목에 할인 금액 비율 분배
            Long discountAmount = 0L;
            if (appliedCoupon != null && totalOriginalAmount > 0 && totalDiscountAmount > 0) {
                // 비율 계산: (항목 원래 금액 / 전체 원래 금액) * 전체 할인 금액
                if (i == itemInfos.size() - 1) {
                    // 마지막 항목: 나머지 할인 금액 모두 할당 (반올림 오차 보정)
                    discountAmount = totalDiscountAmount - savedTotalDiscountAmount;
                } else {
                    discountAmount = Math.round((double) originalAmount / totalOriginalAmount * totalDiscountAmount);
                }
                savedTotalDiscountAmount += discountAmount.intValue();
            }

            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .course(course)
                .section(null)
                .itemType(com.studyblock.domain.payment.enums.ItemType.COURSE)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .coupon(appliedCoupon)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .build();

            log.info("OrderItem 생성 완료 - courseId: {}, quantity: {}, unitPrice: {}, originalAmount: {}, discountAmount: {}, finalAmount: {}", 
                     courseId, quantity, unitPrice, originalAmount, discountAmount, orderItem.getFinalAmount());

            orderItemRepository.save(orderItem);

            log.info("OrderItem 저장 완료 - orderItemId: {}", orderItem.getId());

            // 쿠폰 사용 로그 기록
            if (appliedCoupon != null && userCoupon != null && discountAmount > 0L) {
                CouponUsageLog couponUsageLog = CouponUsageLog.builder()
                    .userCoupon(userCoupon)
                    .order(order)
                    .orderItem(orderItem)
                    .discountAmount(discountAmount.intValue())
                    .usedAt(LocalDateTime.now())
                    .build();
                couponUsageLogRepository.save(couponUsageLog);

                log.info("쿠폰 사용 로그 기록 완료 - userId: {}, orderId: {}, orderItemId: {}, discountAmount: {}", 
                        userId, order.getId(), orderItem.getId(), discountAmount);
            }
        }

        log.info("전체 주문 쿠폰 할인 분배 완료 - totalOriginalAmount: {}, totalDiscountAmount: {}, savedTotalDiscountAmount: {}", 
                totalOriginalAmount, totalDiscountAmount, savedTotalDiscountAmount);
        
        return (int) totalDiscountAmount;
    }
    
    // 내부 헬퍼 클래스
    private static class ItemInfo {
        PaymentValidationRequest.OrderItemRequest itemReq;
        Long originalAmount;
        
        ItemInfo(PaymentValidationRequest.OrderItemRequest itemReq, Long originalAmount) {
            this.itemReq = itemReq;
            this.originalAmount = originalAmount;
        }
    }

    //주문아이템들 생성 + 배분 저장 (결제 승인 시 Payment와 함께 호출) - ValidationRequest 버전
    @Transactional
    public int createOrderItems(Order order, com.studyblock.domain.payment.entity.Payment payment, PaymentValidationRequest request,
                                Coupon appliedCoupon, UserCoupon userCoupon, Long userId) {
        int totalDiscountAmount = createOrderItems(order, request, appliedCoupon, userCoupon, userId);
        for (OrderItem oi : order.getOrderItems()) {
            long lineAmount = (oi.getOriginalAmount() != null ? oi.getOriginalAmount() : 0L)
                    - (oi.getDiscountAmount() != null ? oi.getDiscountAmount() : 0L);
            if (lineAmount < 0) lineAmount = 0;
            com.studyblock.domain.payment.entity.PaymentAllocation alloc = com.studyblock.domain.payment.entity.PaymentAllocation.builder()
                    .payment(payment)
                    .orderItem(oi)
                    .amount((int) lineAmount)
                    .build();
            payment.addAllocation(alloc);
        }
        return totalDiscountAmount;
    }

    //쿠폰 할인 금액 계산
    private Long calculateCouponDiscount(Coupon coupon, Long originalAmount) {
        Long discountAmount = 0L;

        switch (coupon.getType()) {
            case DISCOUNT_PERCENTAGE:
                // 퍼센트 할인: (원가 × 할인율) / 100
                discountAmount = (originalAmount * coupon.getDiscountValue()) / 100;

                // 최대 할인 금액 제한 확인
                if (coupon.getMaximumDiscount() != null && discountAmount > coupon.getMaximumDiscount()) {
                    discountAmount = coupon.getMaximumDiscount().longValue();
                }
                break;

            case DISCOUNT_AMOUNT:
                // 금액 할인: 고정 금액
                discountAmount = coupon.getDiscountValue().longValue();

                // 할인 금액이 원가보다 클 수 없음
                if (discountAmount > originalAmount) {
                    discountAmount = originalAmount;
                }
                break;
        }

        return discountAmount;
    }
}