package com.studyblock.domain.payment.service.validator;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.enums.CouponStatus;
import com.studyblock.domain.coupon.enums.CouponType;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// 결제 도메인 쿠폰 검증 전담 서비스
// 단일 책임: 쿠폰 검증 및 할인 금액 계산
@Slf4j
@Service("paymentCouponValidator")
@RequiredArgsConstructor
public class PaymentCouponValidator {

    private final UserCouponRepository userCouponRepository;

    // 쿠폰 검증 (쿠폰 사용 시) - userCouponId 기준으로 Coupon 반환
    public Coupon validateCoupon(Long userId, Long userCouponId, Long orderAmount) {
        // 쿠폰을 사용하지 않는 경우
        if (userCouponId == null) {
            return null; // 검증 통과
        }

        LocalDateTime now = LocalDateTime.now();
        log.info("🔍 쿠폰 검증 시작 - userId: {}, userCouponId: {}", userId, userCouponId);

        // 1) 사용자 쿠폰 존재 확인(쿠폰 fetch join)
        UserCoupon userCoupon = userCouponRepository.findByIdWithCoupon(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "쿠폰을 찾을 수 없습니다. ID: " + userCouponId
                ));

        log.info("🔍 쿠폰 정보 - userCouponId: {}, 소유자ID: {}, 요청자ID: {}",
                userCouponId, userCoupon.getUser().getId(), userId);

        // 2) 소유자 확인
        if (!userCoupon.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 쿠폰이 아닙니다.");
        }
        log.info("쿠폰 소유자 확인 완료");

        // 3) 이미 사용된 쿠폰인지 확인
        if (Boolean.TRUE.equals(userCoupon.getIsUsed())) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        log.info("✅ 쿠폰 미사용 확인 완료");

        // 4) 쿠폰 만료 확인
        if (userCoupon.getExpiresAt() != null && userCoupon.getExpiresAt().isBefore(now)) {
            throw new IllegalStateException("만료된 쿠폰입니다.");
        }
        log.info("✅ 쿠폰 유효 기간 확인 완료");

        // 5) 쿠폰 상태 확인 (AVAILABLE이어야 함)
        if (userCoupon.getStatus() != CouponStatus.AVAILABLE) {
            throw new IllegalStateException(
                "사용할 수 없는 쿠폰입니다. 현재 상태: " + userCoupon.getStatus()
            );
        }
        log.info("✅ 쿠폰 상태 확인 완료 - status: {}", userCoupon.getStatus());

        // 6) Coupon 마스터 정보 조회 (fetch join으로 이미 로드됨)
        Coupon coupon = userCoupon.getCoupon();

        // 7) 쿠폰 활성화 여부 확인
        if (!coupon.getIsActive()) {
            throw new IllegalStateException("비활성화된 쿠폰입니다.");
        }
        log.info("✅ 쿠폰 활성화 확인 완료");

        // 8) 쿠폰 사용 가능 기간 확인
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new IllegalStateException("쿠폰 사용 가능 기간이 아닙니다.");
        }
        log.info("✅ 쿠폰 사용 기간 확인 완료");

        // 9) 쿠폰 사용 한도 확인
        if (coupon.getUsageLimit() != null && 
            coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalStateException("쿠폰 사용 한도가 초과되었습니다.");
        }
        log.info("✅ 쿠폰 사용 한도 확인 완료");

        // 10) 최소 주문 금액 확인 (쿠폰 적용 전 원래 금액으로 검증)
        // 할인 전 금액이 최소 주문금액을 충족해야 함 (할인 후 금액은 검증하지 않음)
        if (coupon.getMinimumAmount() != null && orderAmount < coupon.getMinimumAmount()) {
            throw new IllegalArgumentException(
                String.format("최소 주문 금액을 충족하지 않습니다. 최소: %d원, 주문 금액(할인 전): %d원",
                    coupon.getMinimumAmount(), orderAmount)
            );
        }
        log.info("✅ 최소 주문 금액 확인 완료 (할인 전 금액 기준) - minimumAmount: {}, orderAmount(할인전): {}",
                coupon.getMinimumAmount(), orderAmount);
        log.info("✅ 쿠폰 검증 완료 - userCouponId: {}, couponName: {}",
                userCouponId, coupon.getName());

        return coupon;
    }

    // 쿠폰 할인 금액 계산 (Coupon 기반)
    public long calculateCouponDiscount(Coupon coupon, long orderAmount) {
        if (coupon == null) {
            return 0L;
        }

        long discount = 0L;
        long calculatedDiscount = 0L;

        // 할인 타입에 따라 계산
        if (coupon.getType() == CouponType.DISCOUNT_PERCENTAGE) {
            calculatedDiscount = Math.round(orderAmount * (coupon.getDiscountValue() / 100.0));
            log.info("💰 퍼센트 할인 계산 - orderAmount: {}, 할인율: {}%, 계산된 할인금액: {}", 
                    orderAmount, coupon.getDiscountValue(), calculatedDiscount);
        } else if (coupon.getType() == CouponType.DISCOUNT_AMOUNT) {
            calculatedDiscount = coupon.getDiscountValue();
            log.info("💰 금액 할인 계산 - 할인금액: {}", calculatedDiscount);
        }

        discount = calculatedDiscount;

        // 최대 할인 금액 제한 (0 또는 음수는 제한 없음으로 간주)
        if (coupon.getMaximumDiscount() != null && coupon.getMaximumDiscount() > 0) {
            log.info("💰 최대 할인 금액 확인 - 계산된 할인: {}, 최대 할인: {}", 
                    discount, coupon.getMaximumDiscount());
            discount = Math.min(discount, coupon.getMaximumDiscount());
            log.info("💰 최대 할인 적용 후 - discount: {}", discount);
        }
        
        // 음수 방지
        if (discount < 0) {
            log.warn("💰 음수 할인 방지 - discount: {} -> 0", discount);
            discount = 0;
        }
        
        // 주문 금액보다 큰 할인 방지
        if (discount > orderAmount) {
            log.warn("💰 주문금액 초과 할인 방지 - discount: {}, orderAmount: {} -> discount: {}", 
                    discount, orderAmount, orderAmount);
            discount = orderAmount;
        }

        log.info("💰 쿠폰 할인 계산 완료 - couponId: {}, type: {}, value: {}, orderAmount: {}, 계산된할인: {}, 최종할인: {}, 최종금액: {}",
                coupon.getId(), coupon.getType(), coupon.getDiscountValue(), orderAmount, 
                calculatedDiscount, discount, orderAmount - discount);

        return discount;
    }
}


