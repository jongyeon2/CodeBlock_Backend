package com.studyblock.domain.coupon.controller;

import com.studyblock.domain.coupon.dto.AvailableCouponResponse;
import com.studyblock.domain.coupon.dto.CouponValidationRequest;
import com.studyblock.domain.coupon.dto.CouponValidationResponse;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.domain.coupon.service.CouponValidationService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

//쿠폰 관련 API Controller

@Slf4j
@RestController
@RequestMapping("/api/user/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final UserCouponRepository userCouponRepository;
    private final AuthenticationUtils authenticationUtils;
    private final CouponValidationService couponValidationService;

    //사용 가능한 쿠폰 목록 조회
    // GET /api/user/coupons/available 요청시 사용 가능한 쿠폰 목록 조회
    // return 사용 가능한 쿠폰 목록
    @GetMapping("/available")
    public ResponseEntity<CommonResponse<List<AvailableCouponResponse>>> getAvailableCoupons(
            Authentication authentication) {

        if (!authenticationUtils.isAuthenticated(authentication)) {
            return handleUnauthorized("쿠폰 목록 조회");
        }

        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return successWithEmptyList();
            }

            List<AvailableCouponResponse> couponResponses = findAvailableCoupons(userId);

            log.info("사용 가능한 쿠폰 조회 완료 - userId: {}, count: {}", userId, couponResponses.size());
            return ResponseEntity.ok(
                CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", couponResponses)
            );

        } catch (Exception e) {
            log.error("쿠폰 목록 조회 중 예상치 못한 오류 발생", e);
            return successWithEmptyList();
        }
    }

    private List<AvailableCouponResponse> findAvailableCoupons(Long userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<UserCoupon> availableCoupons = userCouponRepository.findAvailableCouponsByUserId(userId, now);
            
            if (availableCoupons == null) {
                return new java.util.ArrayList<>();
            }

            return availableCoupons.stream()
                    .filter(uc -> uc != null && uc.getCoupon() != null)
                    .map(AvailableCouponResponse::from)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("쿠폰 조회 중 오류 발생 - userId: {}", userId, e);
            return new java.util.ArrayList<>();
        }
    }

    //내 쿠폰 전체 목록 조회 (페이징 지원)
    // GET /api/user/coupons/my 요청시 내 쿠폰 전체 목록 조회
    // return 내 쿠폰 전체 목록
    @GetMapping("/my")
    public ResponseEntity<CommonResponse<Page<UserCoupon>>> getMyCoupons(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        if (!authenticationUtils.isAuthenticated(authentication)) {
            return handleUnauthorized("내 쿠폰 목록 조회");
        }

        try {
            Long userId = extractUserId(authentication);

            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<UserCoupon> myCoupons = userCouponRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

            log.info("내 쿠폰 목록 조회 완료 - userId: {}, count: {}, totalPages: {}", 
                    userId, myCoupons.getNumberOfElements(), myCoupons.getTotalPages());
            return ResponseEntity.ok(
                CommonResponse.success("내 쿠폰 목록을 조회했습니다", myCoupons)
            );

        } catch (Exception e) {
            log.error("내 쿠폰 목록 조회 중 오류 발생", e);
            return handleInternalError("내 쿠폰 목록 조회 중 오류가 발생했습니다");
        }
    }

    // 쿠폰 검증 API
    @PostMapping("/validate")
    public ResponseEntity<CommonResponse<CouponValidationResponse>> validateCoupon(
            @RequestBody CouponValidationRequest request,
            Authentication authentication) {

        if (!authenticationUtils.isAuthenticated(authentication)) {
            return handleUnauthorized("쿠폰 검증");
        }

        try {
            Long userId = extractUserId(authentication);
            
            // 쿠폰 검증 로직 (userCouponId로 검증)
            boolean isValid = validateCoupon(userId, request);
            
            CouponValidationResponse response = CouponValidationResponse.builder()
                    .valid(isValid)
                    .message(isValid ? "쿠폰을 사용할 수 있습니다" : "사용할 수 없는 쿠폰입니다")
                    .build();
            
            log.info("쿠폰 검증 완료 - userId: {}, couponCode: {}, valid: {}", 
                    userId, request.getCouponCode(), isValid);
            return ResponseEntity.ok(
                CommonResponse.success("쿠폰 검증이 완료되었습니다", response)
            );

        } catch (Exception e) {
            log.error("쿠폰 검증 중 오류 발생", e);
            return handleInternalError("쿠폰 검증 중 오류가 발생했습니다");
        }
    }

    private boolean validateCoupon(Long userId, CouponValidationRequest request) {
        try {
            Long userCouponId = Long.parseLong(request.getCouponCode());
            return couponValidationService.validateCouponUsage(userId, userCouponId, request.getTotalAmount());
        } catch (NumberFormatException e) {
            log.warn("couponCode 형식으로 검증 요청 - {}", request.getCouponCode());
            return false;
        }
    }

    // 쿠폰 사용 취소 API (RESERVED → AVAILABLE)
    @PostMapping("/release/{userCouponId}")
    public ResponseEntity<CommonResponse<String>> releaseCoupon(
            @PathVariable Long userCouponId,
            Authentication authentication) {

        if (!authenticationUtils.isAuthenticated(authentication)) {
            return handleUnauthorized("쿠폰 취소");
        }

        try {
            Long userId = extractUserId(authentication);
            log.info("🔓 쿠폰 사용 취소 요청 - userId: {}, userCouponId: {}", userId, userCouponId);
            
            UserCoupon userCoupon = userCouponRepository.findByIdWithCoupon(userCouponId)
                    .orElse(null);
            
            if (userCoupon == null) {
                return handleNotFound("쿠폰을 찾을 수 없습니다");
            }

            if (!userCoupon.getUser().getId().equals(userId)) {
                return handleForbidden("본인의 쿠폰이 아닙니다");
            }

            if (!userCoupon.isReserved()) {
                return handleBadRequest("취소할 수 없는 쿠폰입니다 (상태: " + userCoupon.getStatus() + ")");
            }
            
            userCoupon.release();
            userCouponRepository.save(userCoupon);
            
            log.info("✅ 쿠폰 사용 취소 완료 - userCouponId: {}, status: {}", 
                    userCouponId, userCoupon.getStatus());
            
            return ResponseEntity.ok(CommonResponse.success("쿠폰 사용이 취소되었습니다"));
            
        } catch (Exception e) {
            log.error("쿠폰 취소 중 오류 발생", e);
            return handleInternalError("쿠폰 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    private Long extractUserId(Authentication authentication) {
        try {
            return authenticationUtils.extractAuthenticatedUserId(authentication);
        } catch (IllegalStateException e) {
            log.warn("인증 정보 추출 실패: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("인증 정보 추출 중 오류 발생", e);
            return null;
        }
    }

    private ResponseEntity<CommonResponse<List<AvailableCouponResponse>>> successWithEmptyList() {
        return ResponseEntity.ok(
            CommonResponse.success("사용 가능한 쿠폰 목록을 조회했습니다", new java.util.ArrayList<>())
        );
    }

    private <T> ResponseEntity<CommonResponse<T>> handleUnauthorized(String action) {
        log.warn("인증되지 않은 사용자가 {}를 시도했습니다", action);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.error("인증이 필요합니다"));
    }

    private <T> ResponseEntity<CommonResponse<T>> handleNotFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(message));
    }

    private <T> ResponseEntity<CommonResponse<T>> handleForbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CommonResponse.error(message));
    }

    private <T> ResponseEntity<CommonResponse<T>> handleBadRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(message));
    }

    private <T> ResponseEntity<CommonResponse<T>> handleInternalError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.error(message));
    }
}

