package com.studyblock.domain.payment.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.enums.CouponStatus;
import com.studyblock.domain.coupon.enums.UserCouponStatus;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//쿠폰처리 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponProcessingService {

    private final UserCouponRepository userCouponRepository;

    //쿠폰조회 및 상태전이 (AVAILABLE → RESERVED)
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public CouponProcessingResult processCouponReservation(Long userCouponId, Long userId) {
        if (userCouponId == null) {
            return new CouponProcessingResult(null, null);
        }

        UserCoupon userCoupon = userCouponRepository.findByIdWithCoupon(userCouponId)
            .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));
        
        // 본인 쿠폰인지 확인
        if (!userCoupon.getUser().getId().equals(userId)) {
            throw new IllegalStateException("다른 사용자의 쿠폰입니다");
        }
        
        // 결제 시작 시점에 상태 전이: AVAILABLE → RESERVED
        if (userCoupon.getStatus() == CouponStatus.AVAILABLE) {
            userCoupon.reserve();  // AVAILABLE → RESERVED
            userCouponRepository.save(userCoupon);
            log.info("🔄 결제 시작으로 쿠폰 상태 전이 - userCouponId: {}, AVAILABLE → RESERVED", userCoupon.getId());
        } else if (userCoupon.getStatus() == CouponStatus.RESERVED) {
            log.info("✅ 이미 RESERVED 상태 - userCouponId: {}", userCoupon.getId());
        } else {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다. 상태: " + userCoupon.getStatus());
        }
        
        Coupon appliedCoupon = userCoupon.getCoupon();
        
        log.info("✅ 쿠폰 확인 완료 - userCouponId: {}, status: {}", userCoupon.getId(), userCoupon.getStatus());
        
        return new CouponProcessingResult(userCoupon, appliedCoupon);
    }

    //쿠폰사용 완료 처리 (RESERVED → USED)
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public void processCouponUsage(UserCoupon userCoupon) {
        if (userCoupon != null && userCoupon.isReserved()) {
            // 상태 전이: RESERVED → USED
            userCoupon.use();  // use() 메서드가 is_used = true와 status = USED로 변경
            userCouponRepository.save(userCoupon);
            log.info("✅ 쿠폰 사용 완료 - userCouponId: {}, status: {}", 
                    userCoupon.getId(), userCoupon.getStatus());
        }
    }

    //쿠폰롤백 처리 (RESERVED → AVAILABLE)
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public void processCouponRollback(UserCoupon userCoupon) {
        if (userCoupon != null && userCoupon.isReserved()) {
            userCoupon.release();
            userCouponRepository.save(userCoupon);
            log.info("✅ 결제 실패로 쿠폰 롤백 완료 - userCouponId: {}, status: AVAILABLE", userCoupon.getId());
        }
    }

    //쿠폰처리 결과 DTO
    public static class CouponProcessingResult {
        private final UserCoupon userCoupon;
        private final Coupon appliedCoupon;

        public CouponProcessingResult(UserCoupon userCoupon, Coupon appliedCoupon) {
            this.userCoupon = userCoupon;
            this.appliedCoupon = appliedCoupon;
        }

        public UserCoupon getUserCoupon() {
            return userCoupon;
        }

        public Coupon getAppliedCoupon() {
            return appliedCoupon;
        }
    }
}
