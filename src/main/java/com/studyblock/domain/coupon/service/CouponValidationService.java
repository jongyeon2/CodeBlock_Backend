package com.studyblock.domain.coupon.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// 쿠폰 검증 전담 서비스 (사용자용)
// 단일 책임: 쿠폰 사용 가능 여부 검증 (상태 전이 없음)
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponValidationService {

    private final UserCouponRepository userCouponRepository;

    // 쿠폰 사용 가능 여부 검증 (상태 전이 없음)
    // 검증만 수행하고 상태 전이는 하지 않음
    public boolean validateCouponUsage(Long userId, Long userCouponId, Integer totalAmount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            log.info("🔍 쿠폰 검증 시작 - userId: {}, userCouponId: {}, totalAmount: {}", 
                    userId, userCouponId, totalAmount);
            
            // 1. 사용자 쿠폰 조회 (Coupon fetch join으로 LazyInitializationException 방지)
            UserCoupon userCoupon = userCouponRepository.findByIdWithCoupon(userCouponId)
                    .orElse(null);
            
            if (userCoupon == null) {
                log.warn("❌ 쿠폰을 찾을 수 없습니다 - userCouponId: {}", userCouponId);
                return false;
            }
            
            // ✅ Coupon 엔티티 fetch join으로 즉시 로딩됨
            Coupon coupon = userCoupon.getCoupon();
            log.info("✅ 쿠폰 조회 성공 - userCouponId: {}, status: {}, isUsed: {}, couponId: {}, couponName: {}", 
                    userCouponId, userCoupon.getStatus(), userCoupon.getIsUsed(), 
                    coupon.getId(), coupon.getName());
            
            // 2. 본인의 쿠폰인지 확인
            if (!userCoupon.getUser().getId().equals(userId)) {
                log.warn("❌ 본인의 쿠폰이 아닙니다 - userId: {}, couponUserId: {}", 
                        userId, userCoupon.getUser().getId());
                return false;
            }
            log.info("✅ 본인 쿠폰 확인 완료");
            
            // 3. 이미 사용했는지 확인
            if (userCoupon.getIsUsed()) {
                log.warn("❌ 이미 사용된 쿠폰입니다 - userCouponId: {}", userCouponId);
                return false;
            }
            log.info("✅ 사용되지 않은 쿠폰 확인 완료");
            
            // 4. 만료되었는지 확인
            if (userCoupon.getExpiresAt() != null && now.isAfter(userCoupon.getExpiresAt())) {
                log.warn("❌ 만료된 쿠폰입니다 - userCouponId: {}, expiresAt: {}", 
                        userCouponId, userCoupon.getExpiresAt());
                return false;
            }
            log.info("✅ 만료되지 않은 쿠폰 확인 완료");
            
            // 5. 최소 주문 금액 확인 (쿠폰 적용 전 원래 금액으로 검증)
            log.info("💰 쿠폰 정보 - minimumAmount: {}, totalAmount(할인전): {}", 
                    coupon.getMinimumAmount(), totalAmount);
            if (coupon.getMinimumAmount() != null && totalAmount < coupon.getMinimumAmount()) {
                log.warn("❌ 최소 주문 금액 미달 - minimum: {}, total(할인전): {}", 
                        coupon.getMinimumAmount(), totalAmount);
                return false;
            }
            log.info("✅ 최소 주문 금액 충족 (할인 전 금액 기준)");
            
            // ✅ 쿠폰 검증만 수행 (상태 전이 없음)
            log.info("✅ 쿠폰 검증 성공 - userCouponId: {}, userId: {}, status: {}",
                    userCouponId, userId, userCoupon.getStatus());
            return true;

        } catch (IllegalStateException e) {
            log.error("❌ 쿠폰 예약 불가 - {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ 쿠폰 검증 중 오류 발생", e);
            return false;
        }
    }
}

