package com.studyblock.domain.wallet.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.payment.entity.CookieBundle;
import com.studyblock.domain.payment.service.validator.PaymentCouponValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieChargeCalculationService {

    private final PaymentCouponValidator paymentCouponValidator;

    public static class Result {
        public final long totalCashAmount;
        public final long discount;
        public final long discountedAmount;
        public Result(long totalCashAmount, long discount, long discountedAmount) {
            this.totalCashAmount = totalCashAmount;
            this.discount = discount;
            this.discountedAmount = discountedAmount;
        }
    }

    public Result calculate(CookieBundle bundle, int quantity, Long userId, Long userCouponId) {
        long totalCashAmount = bundle.getPrice() * (long) quantity;
        long discount = 0L;
        long discountedAmount = totalCashAmount;

        if (userCouponId != null) {
            Coupon coupon = paymentCouponValidator.validateCoupon(userId, userCouponId, totalCashAmount);
            discount = paymentCouponValidator.calculateCouponDiscount(coupon, totalCashAmount);
            discountedAmount = totalCashAmount - discount;
        }
        return new Result(totalCashAmount, discount, discountedAmount);
    }
}


