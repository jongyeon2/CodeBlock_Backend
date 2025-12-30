package com.studyblock.domain.coupon.controller;

import com.studyblock.domain.coupon.dto.AvailableCouponResponse;
import com.studyblock.domain.coupon.dto.CouponValidationRequest;
import com.studyblock.domain.coupon.dto.CouponValidationResponse;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 쿠폰 API 호환성 컨트롤러
 * 프론트엔드에서 사용하는 /api/coupons 경로를 지원
 */
@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponCompatibilityController {

    private final UserCouponRepository userCouponRepository;
    private final AuthenticationUtils authenticationUtils;

    //사용 가능한 쿠폰 목록 조회 (프론트엔드 호환용)
    // GET /api/coupons/available 요청시 사용 가능한 쿠폰 목록 조회
    // return 사용 가능한 쿠폰 목록
    @GetMapping("/available")
    public ResponseEntity<CommonResponse<List<AvailableCouponResponse>>> getAvailableCoupons(
            Authentication authentication) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.warn("인증되지 않은 사용자가 쿠폰 목록을 조회하려고 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다"));
        }

        Long userId = null;
        try {
            // JWT 토큰에서 사용자 정보 추출
            try {
                userId = authenticationUtils.extractAuthenticatedUserId(authentication);
                log.debug("사용 가능한 쿠폰 조회 요청 - userId: {}", userId);
            } catch (IllegalStateException e) {
                log.warn("인증 정보 추출 실패: {}", e.getMessage());
                return ResponseEntity.ok(
                    CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", new java.util.ArrayList<>())
                );
            } catch (Exception e) {
                log.error("인증 정보 추출 중 오류 발생", e);
                return ResponseEntity.ok(
                    CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", new java.util.ArrayList<>())
                );
            }
            
            // 사용 가능한 쿠폰 조회 (사용 안 함 + 만료 안 됨)
            List<UserCoupon> availableCoupons;
            try {
                LocalDateTime now = LocalDateTime.now();
                availableCoupons = userCouponRepository.findAvailableCouponsByUserId(userId, now);
                
                // null 체크
                if (availableCoupons == null) {
                    availableCoupons = new java.util.ArrayList<>();
                }
            } catch (Exception e) {
                log.error("쿠폰 조회 중 오류 발생 - userId: {}", userId, e);
                return ResponseEntity.ok(
                    CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", new java.util.ArrayList<>())
                );
            }

            // DTO로 변환 (null 체크 포함)
            List<AvailableCouponResponse> couponResponses;
            try {
                couponResponses = availableCoupons.stream()
                        .filter(uc -> uc != null && uc.getCoupon() != null) // null 체크
                        .map(AvailableCouponResponse::from)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("쿠폰 DTO 변환 중 오류 발생 - userId: {}", userId, e);
                // 변환 실패 시 빈 리스트 반환
                couponResponses = new java.util.ArrayList<>();
            }

            log.info("사용 가능한 쿠폰 조회 완료 - userId: {}, count: {}", userId, couponResponses.size());
            return ResponseEntity.ok(
                CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", couponResponses)
            );

        } catch (Exception e) {
            log.error("쿠폰 목록 조회 중 예상치 못한 오류 발생 - userId: {}", userId, e);
            // 모든 예외를 잡아서 빈 리스트 반환
            return ResponseEntity.ok(
                CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", new java.util.ArrayList<>())
            );
        }
    }

    // 쿠폰 검증 API (프론트엔드 호환용)
    // POST /api/coupons/validate 요청시 쿠폰 검증
    // return 쿠폰 검증 결과
    @PostMapping("/validate")
    public ResponseEntity<CommonResponse<CouponValidationResponse>> validateCoupon(
            @RequestBody CouponValidationRequest request,
            Authentication authentication) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.warn("인증되지 않은 사용자가 쿠폰을 검증하려고 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다"));
        }

        Long userId = null;
        try {
            // 인증 정보 추출
            try {
                userId = authenticationUtils.extractAuthenticatedUserId(authentication);
                log.debug("쿠폰 검증 요청 - userId: {}", userId);
            } catch (IllegalStateException e) {
                log.warn("인증 정보 추출 실패: {}", e.getMessage());
                CouponValidationResponse response = CouponValidationResponse.builder()
                        .valid(false)
                        .message("인증 정보를 가져올 수 없습니다")
                        .build();
                return ResponseEntity.ok(
                    CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
                );
            } catch (Exception e) {
                log.error("인증 정보 추출 중 오류 발생", e);
                CouponValidationResponse response = CouponValidationResponse.builder()
                        .valid(false)
                        .message("인증 처리 중 오류가 발생했습니다")
                        .build();
                return ResponseEntity.ok(
                    CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
                );
            }
            
            // 요청 데이터 검증
            if (request == null || request.getCouponCode() == null || request.getCouponCode().isBlank()) {
                log.warn("쿠폰 코드가 없습니다 - userId: {}", userId);
                CouponValidationResponse response = CouponValidationResponse.builder()
                        .valid(false)
                        .message("쿠폰 코드를 입력해주세요")
                        .build();
                return ResponseEntity.ok(
                    CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
                );
            }

            // 쿠폰 검증 로직 (userCouponId로 검증)
            boolean isValid = false;
            try {
                // couponCode가 문자열이면 userCouponId로 변환
                Long userCouponId;
                try {
                    userCouponId = Long.parseLong(request.getCouponCode());
                } catch (NumberFormatException e) {
                    log.warn("쿠폰 코드 형식이 올바르지 않습니다 - couponCode: {}", request.getCouponCode());
                    CouponValidationResponse response = CouponValidationResponse.builder()
                            .valid(false)
                            .message("쿠폰 코드 형식이 올바르지 않습니다")
                            .build();
                    return ResponseEntity.ok(
                        CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
                    );
                }
                
                isValid = validateCouponUsage(userId, userCouponId, request.getTotalAmount());
                
            } catch (Exception e) {
                log.error("쿠폰 검증 중 오류 발생 - userId: {}, couponCode: {}", userId, request.getCouponCode(), e);
                CouponValidationResponse response = CouponValidationResponse.builder()
                        .valid(false)
                        .message("쿠폰 검증 중 오류가 발생했습니다")
                        .build();
                return ResponseEntity.ok(
                    CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
                );
            }
            
            CouponValidationResponse response = CouponValidationResponse.builder()
                    .valid(isValid)
                    .message(isValid ? "쿠폰을 사용할 수 있습니다" : "사용할 수 없는 쿠폰입니다")
                    .build();
            
            log.info("쿠폰 검증 완료 - userId: {}, couponCode: {}, valid: {}", userId, request.getCouponCode(), isValid);
            return ResponseEntity.ok(
                CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
            );

        } catch (Exception e) {
            log.error("쿠폰 검증 중 예상치 못한 오류 발생 - userId: {}", userId, e);
            CouponValidationResponse response = CouponValidationResponse.builder()
                    .valid(false)
                    .message("쿠폰 검증 중 오류가 발생했습니다")
                    .build();
            // 모든 예외를 잡아서 valid=false로 응답
            return ResponseEntity.ok(
                CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
            );
        }
    }

    // 쿠폰 사용 가능 여부 검증
    private boolean validateCouponUsage(Long userId, Long userCouponId, Integer totalAmount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            log.info("🔍 쿠폰 검증 시작 - userId: {}, userCouponId: {}, totalAmount: {}", userId, userCouponId, totalAmount);
            
            // 1. 사용자 쿠폰 조회 (Coupon fetch join으로 LazyInitializationException 방지)
            UserCoupon userCoupon = userCouponRepository.findByIdWithCoupon(userCouponId)
                    .orElse(null);
            
            if (userCoupon == null) {
                log.warn("❌ 쿠폰을 찾을 수 없습니다 - userCouponId: {}", userCouponId);
                return false;
            }
            
            // ✅ Coupon 엔티티 fetch join으로 즉시 로딩됨
            var coupon = userCoupon.getCoupon();
            if (coupon == null) {
                log.warn("❌ 쿠폰 정보를 찾을 수 없습니다 - userCouponId: {}", userCouponId);
                return false;
            }
            
            log.info("✅ 쿠폰 조회 성공 - userCouponId: {}, status: {}, isUsed: {}, couponId: {}, couponName: {}", 
                    userCouponId, userCoupon.getStatus(), userCoupon.getIsUsed(), coupon.getId(), coupon.getName());
            
            // 2. 본인의 쿠폰인지 확인
            if (userCoupon.getUser() == null || !userCoupon.getUser().getId().equals(userId)) {
                log.warn("❌ 본인의 쿠폰이 아닙니다 - userId: {}, couponUserId: {}", 
                        userId, userCoupon.getUser() != null ? userCoupon.getUser().getId() : null);
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
                log.warn("❌ 만료된 쿠폰입니다 - userCouponId: {}, expiresAt: {}", userCouponId, userCoupon.getExpiresAt());
                return false;
            }
            log.info("✅ 만료되지 않은 쿠폰 확인 완료");
            
            // 5. 최소 주문 금액 확인 (쿠폰 적용 전 원래 금액으로 검증)
            // 할인 전 금액이 최소 주문금액을 충족해야 함 (할인 후 금액은 검증하지 않음)
            if (totalAmount == null) {
                totalAmount = 0;
            }
            log.info("💰 쿠폰 정보 - minimumAmount: {}, totalAmount(할인전): {}", coupon.getMinimumAmount(), totalAmount);
            if (coupon.getMinimumAmount() != null && totalAmount < coupon.getMinimumAmount()) {
                log.warn("❌ 최소 주문 금액 미달 - minimum: {}, total(할인전): {}", coupon.getMinimumAmount(), totalAmount);
                return false;
            }
            log.info("✅ 최소 주문 금액 충족 (할인 전 금액 기준)");
            
            // ✅ 쿠폰 검증만 수행 (상태 전이 없음)
            // 상태 전이는 결제 버튼 클릭 시 (/api/payment/confirm) 수행
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

    // 쿠폰 사용 취소 API (프론트엔드 호환용)
    // POST /api/coupons/release/{userCouponId} 요청시 쿠폰 사용 취소
    @PostMapping("/release/{userCouponId}")
    public ResponseEntity<CommonResponse<String>> releaseCoupon(
            @PathVariable Long userCouponId,
            Authentication authentication) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.warn("인증되지 않은 사용자가 쿠폰 취소를 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다"));
        }

        Long userId = null;
        try {
            // 인증 정보 추출
            try {
                userId = authenticationUtils.extractAuthenticatedUserId(authentication);
                log.info("🔓 쿠폰 사용 취소 요청 - userId: {}, userCouponId: {}", userId, userCouponId);
            } catch (IllegalStateException e) {
                log.warn("인증 정보 추출 실패: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증 정보를 가져올 수 없습니다"));
            } catch (Exception e) {
                log.error("인증 정보 추출 중 오류 발생", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(CommonResponse.error("인증 처리 중 오류가 발생했습니다"));
            }

            // UserCoupon 조회 (Coupon fetch join으로 LazyInitializationException 방지)
            UserCoupon userCoupon;
            try {
                userCoupon = userCouponRepository.findByIdWithCoupon(userCouponId)
                        .orElse(null);
            } catch (Exception e) {
                log.error("쿠폰 조회 중 오류 발생 - userCouponId: {}", userCouponId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(CommonResponse.error("쿠폰 조회 중 오류가 발생했습니다"));
            }

            if (userCoupon == null) {
                log.warn("❌ 쿠폰을 찾을 수 없습니다 - userCouponId: {}", userCouponId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResponse.error("쿠폰을 찾을 수 없습니다"));
            }

            // 본인의 쿠폰인지 확인
            if (userCoupon.getUser() == null || !userCoupon.getUser().getId().equals(userId)) {
                log.warn("❌ 본인의 쿠폰이 아닙니다 - userId: {}, couponUserId: {}", 
                        userId, userCoupon.getUser() != null ? userCoupon.getUser().getId() : null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.error("본인의 쿠폰이 아닙니다"));
            }

            // RESERVED 상태인지 확인
            if (!userCoupon.isReserved()) {
                log.warn("❌ 취소할 수 없는 쿠폰입니다 - userCouponId: {}, status: {}", 
                        userCouponId, userCoupon.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error("취소할 수 없는 쿠폰입니다 (상태: " + userCoupon.getStatus() + ")"));
            }

            // 쿠폰 상태 전이: RESERVED → AVAILABLE
            try {
                userCoupon.release();
                userCouponRepository.save(userCoupon);
            } catch (Exception e) {
                log.error("쿠폰 취소 처리 중 오류 발생 - userCouponId: {}", userCouponId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(CommonResponse.error("쿠폰 취소 처리 중 오류가 발생했습니다"));
            }

            log.info("✅ 쿠폰 사용 취소 완료 - userCouponId: {}, status: {}", 
                    userCouponId, userCoupon.getStatus());
            return ResponseEntity.ok(CommonResponse.success("쿠폰 사용이 취소되었습니다"));

        } catch (Exception e) {
            log.error("쿠폰 취소 중 예상치 못한 오류 발생 - userId: {}, userCouponId: {}", userId, userCouponId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

