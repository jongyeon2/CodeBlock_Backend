package com.studyblock.domain.coupon.service;

import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.CouponRepository;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    //단일 사용자에게 쿠폰 발급
    //@param couponId 쿠폰 ID
    //@param userId 사용자 ID
    //@return 발급된 UserCoupon

    @Transactional
    public UserCoupon issueCouponToUser(Long couponId, Long userId) {
        // 1. Coupon 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));

        // 2. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 3. 중복 발급 확인
        if (userCouponRepository.existsByUser_IdAndCoupon_Id(userId, couponId)) {
            log.warn("이미 발급된 쿠폰입니다 - userId: {}, couponId: {}", userId, couponId);
            throw new IllegalStateException("이미 발급된 쿠폰입니다");
        }

        // 4. 쿠폰 활성화 확인
        if (!coupon.getIsActive()) {
            throw new IllegalStateException("비활성화된 쿠폰은 발급할 수 없습니다");
        }

        // 5. 사용 한도 확인
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalStateException("쿠폰 발급 한도를 초과했습니다");
        }

        // 6. 쿠폰 코드 생성 (COUPON-{랜덤6자}-{타임스탬프})
        String couponCode = generateCouponCode();

        // 7. 만료일 설정 (쿠폰의 validUntil 사용)
        LocalDateTime expiresAt = coupon.getValidUntil();

        // 8. UserCoupon 생성
        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .couponCode(couponCode)
                .expiresAt(expiresAt)
                .build();

        userCouponRepository.save(userCoupon);

        // 9. Coupon의 usedCount 증가 (리플렉션 사용)
        incrementUsedCount(coupon);

        log.info("쿠폰 발급 완료 - userId: {}, couponId: {}, couponCode: {}",
                userId, couponId, couponCode);

        return userCoupon;
    }

    //여러 사용자에게 쿠폰 일괄 발급
    //@param couponId 쿠폰 ID
    //@param userIds 사용자 ID 목록
    //@return 발급 성공/실패 통계

    @Transactional
    public IssueBulkResult issueCouponBulk(Long couponId, List<Long> userIds) {
        int successCount = 0;
        int failCount = 0;
        List<String> failedUserIds = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                issueCouponToUser(couponId, userId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failedUserIds.add(String.valueOf(userId));
                log.warn("쿠폰 발급 실패 - userId: {}, error: {}", userId, e.getMessage());
            }
        }

        log.info("쿠폰 일괄 발급 완료 - couponId: {}, total: {}, success: {}, fail: {}",
                couponId, userIds.size(), successCount, failCount);

        return new IssueBulkResult(successCount, failCount, failedUserIds);
    }

    //전체 사용자에게 쿠폰 발급
    //@param couponId 쿠폰 ID
    //@return 발급 성공/실패 통계

    @Transactional
    public IssueBulkResult issueCouponToAll(Long couponId) {
        // 1. 쿠폰 조회 및 검증
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));

        // 쿠폰 활성화 확인
        if (!coupon.getIsActive()) {
            throw new IllegalStateException("비활성화된 쿠폰은 발급할 수 없습니다");
        }

        // 사용 한도 확인
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalStateException("쿠폰 발급 한도를 초과했습니다");
        }

        // 2. 모든 사용자 ID 조회 (엔티티 전체를 로드하지 않음)
        List<Long> userIds = userRepository.findAllUserIds();
        
        if (userIds.isEmpty()) {
            log.warn("발급할 사용자가 없습니다 - couponId: {}", couponId);
            return new IssueBulkResult(0, 0, new ArrayList<>());
        }

        // 3. 이미 발급된 사용자 ID 조회 (중복 체크용 - ID만 조회하여 성능 최적화)
        List<Long> alreadyIssuedUserIds = userCouponRepository.findUserIdsByCouponId(couponId);

        // 4. 발급 대상 사용자 필터링 (이미 발급된 사용자 제외)
        List<Long> targetUserIds = userIds.stream()
                .filter(userId -> !alreadyIssuedUserIds.contains(userId))
                .toList();

        if (targetUserIds.isEmpty()) {
            log.warn("모든 사용자에게 이미 쿠폰이 발급되었습니다 - couponId: {}", couponId);
            return new IssueBulkResult(0, userIds.size(), userIds.stream()
                    .map(String::valueOf)
                    .toList());
        }

        // 5. 배치로 UserCoupon 생성
        List<UserCoupon> userCouponsToSave = new ArrayList<>();
        LocalDateTime expiresAt = coupon.getValidUntil();
        List<Long> failedUserIds = new ArrayList<>();

        for (Long userId : targetUserIds) {
            try {
                // User 엔티티 조회 (필요한 경우에만)
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

                String couponCode = generateCouponCode();
                UserCoupon userCoupon = UserCoupon.builder()
                        .user(user)
                        .coupon(coupon)
                        .couponCode(couponCode)
                        .expiresAt(expiresAt)
                        .build();

                userCouponsToSave.add(userCoupon);
            } catch (Exception e) {
                failedUserIds.add(userId);
                log.warn("쿠폰 발급 대상 사용자 처리 실패 - userId: {}, error: {}", userId, e.getMessage());
            }
        }

        // 6. 배치 저장
        if (!userCouponsToSave.isEmpty()) {
            userCouponRepository.saveAll(userCouponsToSave);
        }

        // 7. Coupon의 usedCount 증가 (한 번에 처리)
        int successCount = userCouponsToSave.size();
        incrementUsedCountByAmount(coupon, successCount);

        int failCount = failedUserIds.size() + (userIds.size() - targetUserIds.size());
        List<String> failedUserIdsStr = new ArrayList<>();
        failedUserIdsStr.addAll(failedUserIds.stream().map(String::valueOf).toList());
        failedUserIdsStr.addAll(alreadyIssuedUserIds.stream().map(String::valueOf).toList());

        log.info("전체 사용자 쿠폰 발급 완료 - couponId: {}, total: {}, success: {}, fail: {}",
                couponId, userIds.size(), successCount, failCount);

        return new IssueBulkResult(successCount, failCount, failedUserIdsStr);
    }

    //쿠폰 코드 생성 (COUPON-{랜덤6자}-{타임스탬프})
    //@return 쿠폰 코드

    private String generateCouponCode() {
        String randomPart = generateRandomString(6);
        long timestamp = System.currentTimeMillis() / 1000; // 초 단위
        return String.format("COUPON-%s-%d", randomPart, timestamp);
    }

    //랜덤 문자열 생성 (영문 대문자 + 숫자)
    //@param length 길이
    //@return 랜덤 문자열

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    //Coupon의 usedCount 증가 (리플렉션 사용)

    private void incrementUsedCount(Coupon coupon) {
        incrementUsedCountByAmount(coupon, 1);
    }

    //Coupon의 usedCount를 지정된 수만큼 증가 (리플렉션 사용)
    //@param coupon 쿠폰 엔티티
    //@param amount 증가할 수량

    private void incrementUsedCountByAmount(Coupon coupon, int amount) {
        try {
            java.lang.reflect.Field field = Coupon.class.getDeclaredField("usedCount");
            field.setAccessible(true);
            Integer currentCount = (Integer) field.get(coupon);
            field.set(coupon, currentCount + amount);
            field.setAccessible(false);
            couponRepository.save(coupon);
        } catch (Exception e) {
            log.error("usedCount 증가 실패", e);
        }
    }

    //일괄 발급 결과 클래스

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class IssueBulkResult {
        private int successCount;
        private int failCount;
        private List<String> failedUserIds;
    }
}
