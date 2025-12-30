package com.studyblock.domain.coupon.service;

import com.studyblock.domain.coupon.dto.CouponAdminResponse;
import com.studyblock.domain.coupon.dto.CouponCreateRequest;
import com.studyblock.domain.coupon.dto.CouponUpdateRequest;
import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.repository.CouponRepository;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponAdminService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;

    //쿠폰 생성
    //@param request 쿠폰 생성 요청
    //@param creatorId 생성자 ID
    //@return 생성된 쿠폰

    @Transactional
    public Coupon createCoupon(CouponCreateRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElse(null);

        Coupon coupon = Coupon.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .discountValue(request.getDiscountValue())
                .minimumAmount(request.getMinimumAmount())
                .maximumDiscount(request.getMaximumDiscount())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .usageLimit(request.getUsageLimit())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(creator)
                .build();

        couponRepository.save(coupon);

        log.info("쿠폰 생성 완료 - couponId: {}, name: {}, creatorId: {}",
                coupon.getId(), coupon.getName(), creatorId);

        return coupon;
    }

    //쿠폰 수정
    //@param couponId 쿠폰 ID
    //@param request 쿠폰 수정 요청
    //@return 수정된 쿠폰

    @Transactional
    public Coupon updateCoupon(Long couponId, CouponUpdateRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));

        // 수정 가능한 필드만 업데이트 (Reflection 대신 빌더 패턴 사용 불가능하므로 수동 업데이트)
        // Entity에 update 메서드 추가 필요 (나중에 리팩토링)

        couponRepository.save(coupon);

        log.info("쿠폰 수정 완료 - couponId: {}, name: {}", couponId, coupon.getName());

        return coupon;
    }

    //쿠폰 삭제
    //@param couponId 쿠폰 ID

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));

        couponRepository.delete(coupon);

        log.info("쿠폰 삭제 완료 - couponId: {}, name: {}", couponId, coupon.getName());
    }

    //쿠폰 활성화/비활성화 토글
    //@param couponId 쿠폰 ID
    //@return 업데이트된 쿠폰

    @Transactional
    public Coupon toggleCouponActive(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));

        // Entity에 toggle 메서드 추가 필요 (임시로 Reflection 사용)
        try {
            java.lang.reflect.Field field = Coupon.class.getDeclaredField("isActive");
            field.setAccessible(true);
            Boolean currentValue = (Boolean) field.get(coupon);
            field.set(coupon, !currentValue);
            field.setAccessible(false);
        } catch (Exception e) {
            log.error("쿠폰 활성화 상태 변경 실패", e);
            throw new IllegalStateException("쿠폰 활성화 상태 변경에 실패했습니다");
        }

        couponRepository.save(coupon);

        log.info("쿠폰 활성화 상태 변경 완료 - couponId: {}, isActive: {}", couponId, coupon.getIsActive());

        return coupon;
    }

    //쿠폰 상세 조회
    //@param couponId 쿠폰 ID
    //@return 쿠폰

    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));
    }

    //쿠폰 목록 조회 (전체) - DTO 변환까지 완료
    //@return 쿠폰 목록 (CouponAdminResponse)

    @Transactional(readOnly = true)
    public List<CouponAdminResponse> getAllCoupons() {
        // createdBy fetch join으로 조회하여 LazyInitializationException 방지
        List<Coupon> coupons = couponRepository.findAllWithCreator();
        
        // 트랜잭션 내에서 DTO 변환까지 완료
        return coupons.stream()
                .map(coupon -> {
                    try {
                        // user_coupons 테이블에서 집계
                        Long issuedCount = userCouponRepository.countIssuedCouponsByCouponId(coupon.getId());
                        Long usedCountFromUserCoupons = userCouponRepository.countUsedCouponsByCouponId(coupon.getId());
                        
                        // null 체크
                        if (issuedCount == null) {
                            issuedCount = 0L;
                        }
                        if (usedCountFromUserCoupons == null) {
                            usedCountFromUserCoupons = 0L;
                        }
                        
                        return CouponAdminResponse.from(coupon, issuedCount, usedCountFromUserCoupons);
                    } catch (Exception e) {
                        log.warn("쿠폰 집계 정보 조회 실패 - couponId: {}, error: {}", coupon.getId(), e.getMessage());
                        // 집계 실패 시 기본값으로 반환
                        return CouponAdminResponse.from(coupon, 0L, 0L);
                    }
                })
                .collect(Collectors.toList());
    }

    //활성화된 쿠폰 목록 조회 - DTO 변환까지 완료
    //@return 활성화된 쿠폰 목록 (CouponAdminResponse)

    @Transactional(readOnly = true)
    public List<CouponAdminResponse> getActiveCoupons() {
        List<Coupon> coupons = couponRepository.findByIsActive(true);
        
        // 트랜잭션 내에서 DTO 변환까지 완료
        return convertToCouponAdminResponse(coupons);
    }

    //유효한 쿠폰 목록 조회 (현재 시점 기준) - DTO 변환까지 완료
    //@return 유효한 쿠폰 목록 (CouponAdminResponse)

    @Transactional(readOnly = true)
    public List<CouponAdminResponse> getValidCoupons() {
        List<Coupon> coupons = couponRepository.findValidCoupons(LocalDateTime.now());
        
        // 트랜잭션 내에서 DTO 변환까지 완료
        return convertToCouponAdminResponse(coupons);
    }

    //사용 가능한 쿠폰 목록 조회 (유효 + 한도 미달) - DTO 변환까지 완료
    //@return 사용 가능한 쿠폰 목록 (CouponAdminResponse)

    @Transactional(readOnly = true)
    public List<CouponAdminResponse> getAvailableCoupons() {
        List<Coupon> coupons = couponRepository.findAvailableCoupons(LocalDateTime.now());
        
        // 트랜잭션 내에서 DTO 변환까지 완료
        return convertToCouponAdminResponse(coupons);
    }

    // Coupon 리스트를 CouponAdminResponse 리스트로 변환 (트랜잭션 내에서 실행)
    private List<CouponAdminResponse> convertToCouponAdminResponse(List<Coupon> coupons) {
        return coupons.stream()
                .map(coupon -> {
                    try {
                        // user_coupons 테이블에서 집계
                        Long issuedCount = userCouponRepository.countIssuedCouponsByCouponId(coupon.getId());
                        Long usedCountFromUserCoupons = userCouponRepository.countUsedCouponsByCouponId(coupon.getId());
                        
                        // null 체크
                        if (issuedCount == null) {
                            issuedCount = 0L;
                        }
                        if (usedCountFromUserCoupons == null) {
                            usedCountFromUserCoupons = 0L;
                        }
                        
                        return CouponAdminResponse.from(coupon, issuedCount, usedCountFromUserCoupons);
                    } catch (Exception e) {
                        log.warn("쿠폰 집계 정보 조회 실패 - couponId: {}, error: {}", coupon.getId(), e.getMessage());
                        // 집계 실패 시 기본값으로 반환
                        return CouponAdminResponse.from(coupon, 0L, 0L);
                    }
                })
                .collect(Collectors.toList());
    }

    //쿠폰 통계
    //@return 활성화된 쿠폰 수

    @Transactional(readOnly = true)
    public Long countActiveCoupons() {
        return couponRepository.countActiveCoupons();
    }

    //총 사용 횟수
    //@return 총 사용 횟수

    @Transactional(readOnly = true)
    public Long getTotalUsedCount() {
        Long count = couponRepository.sumTotalUsedCount();
        return count != null ? count : 0L;
    }
}
