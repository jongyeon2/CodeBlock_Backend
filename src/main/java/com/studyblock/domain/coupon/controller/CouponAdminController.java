package com.studyblock.domain.coupon.controller;

import com.studyblock.domain.coupon.dto.*;
import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.coupon.entity.UserCoupon;
import com.studyblock.domain.coupon.repository.UserCouponRepository;
import com.studyblock.domain.coupon.service.CouponAdminService;
import com.studyblock.domain.coupon.service.CouponIssueService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//쿠폰 관리자 API Controller

@Slf4j
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CouponAdminController {

    private final CouponAdminService couponAdminService;
    private final CouponIssueService couponIssueService;
    private final UserCouponRepository userCouponRepository;
    private final AuthenticationUtils authenticationUtils;

    //쿠폰 생성
    @PostMapping
    public ResponseEntity<CommonResponse<CouponAdminResponse>> createCoupon(
            @RequestBody CouponCreateRequest request,
            Authentication authentication) {

        try {
            Long creatorId = authenticationUtils.extractAuthenticatedUserId(authentication);
            Coupon coupon = couponAdminService.createCoupon(request, creatorId);

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰이 생성되었습니다",
                    CouponAdminResponse.from(coupon)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("쿠폰 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 생성 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<Map<String, Object>>> getAllCoupons(
            @RequestParam(required = false) String filter) {

        try {
            List<CouponAdminResponse> responses;

            if ("active".equals(filter)) {
                responses = couponAdminService.getActiveCoupons();
            } else if ("valid".equals(filter)) {
                responses = couponAdminService.getValidCoupons();
            } else if ("available".equals(filter)) {
                responses = couponAdminService.getAvailableCoupons();
            } else {
                responses = couponAdminService.getAllCoupons();
            }

            // 응답 생성 (count 포함)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("data", responses);
            responseData.put("count", responses.size());

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰 목록을 조회했습니다",
                    responseData
            ));

        } catch (Exception e) {
            log.error("쿠폰 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 목록 조회 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 상세 조회
    @GetMapping("/{couponId}")
    public ResponseEntity<CommonResponse<CouponAdminResponse>> getCoupon(
            @PathVariable Long couponId) {

        try {
            Coupon coupon = couponAdminService.getCoupon(couponId);

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰을 조회했습니다",
                    CouponAdminResponse.from(coupon)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("쿠폰 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 조회 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 수정
    @PutMapping("/{couponId}")
    public ResponseEntity<CommonResponse<CouponAdminResponse>> updateCoupon(
            @PathVariable Long couponId,
            @RequestBody CouponUpdateRequest request) {

        try {
            Coupon coupon = couponAdminService.updateCoupon(couponId, request);

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰이 수정되었습니다",
                    CouponAdminResponse.from(coupon)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("쿠폰 수정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 수정 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 삭제
    @DeleteMapping("/{couponId}")
    public ResponseEntity<CommonResponse<Void>> deleteCoupon(
            @PathVariable Long couponId) {

        try {
            couponAdminService.deleteCoupon(couponId);

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰이 삭제되었습니다"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("쿠폰 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 삭제 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 활성화/비활성화
    @PatchMapping("/{couponId}/toggle")
    public ResponseEntity<CommonResponse<CouponAdminResponse>> toggleCouponActive(
            @PathVariable Long couponId) {

        try {
            Coupon coupon = couponAdminService.toggleCouponActive(couponId);

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰 활성화 상태가 변경되었습니다",
                    CouponAdminResponse.from(coupon)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("쿠폰 활성화 상태 변경 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 활성화 상태 변경 중 오류가 발생했습니다"));
        }
    }

    //특정 사용자에게 쿠폰 발급
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CommonResponse<CouponIssueResponse>> issueCoupon(
            @PathVariable Long couponId,
            @RequestBody CouponIssueRequest request) {

        try {
            if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error("사용자 ID를 입력해주세요"));
            }

            // 단일 사용자 발급
            if (request.getUserIds().size() == 1) {
                Long userId = request.getUserIds().get(0);
                UserCoupon userCoupon = couponIssueService.issueCouponToUser(couponId, userId);

                CouponIssueResponse response = CouponIssueResponse.builder()
                        .couponId(couponId)
                        .couponName(userCoupon.getCoupon().getName())
                        .totalIssued(1)
                        .successCount(1)
                        .failCount(0)
                        .message("쿠폰이 발급되었습니다")
                        .build();

                return ResponseEntity.ok(CommonResponse.success(
                        "쿠폰이 발급되었습니다",
                        response
                ));
            }

            // 여러 사용자 발급
            CouponIssueService.IssueBulkResult result = couponIssueService.issueCouponBulk(
                    couponId, request.getUserIds());

            Coupon coupon = couponAdminService.getCoupon(couponId);

            CouponIssueResponse response = CouponIssueResponse.builder()
                    .couponId(couponId)
                    .couponName(coupon.getName())
                    .totalIssued(request.getUserIds().size())
                    .successCount(result.getSuccessCount())
                    .failCount(result.getFailCount())
                    .failedUserIds(result.getFailedUserIds())
                    .message(String.format("쿠폰 발급 완료 (성공: %d, 실패: %d)",
                            result.getSuccessCount(), result.getFailCount()))
                    .build();

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰이 발급되었습니다",
                    response
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("쿠폰 발급 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 발급 중 오류가 발생했습니다"));
        }
    }

    //전체 사용자에게 쿠폰 발급
    @PostMapping("/{couponId}/issue-all")
    public ResponseEntity<CommonResponse<CouponIssueResponse>> issueCouponToAll(
            @PathVariable Long couponId) {

        try {
            log.info("전체 사용자 쿠폰 발급 요청 - couponId: {}", couponId);

            // 쿠폰 존재 여부 확인
            Coupon coupon = couponAdminService.getCoupon(couponId);

            // 쿠폰 발급
            CouponIssueService.IssueBulkResult result = couponIssueService.issueCouponToAll(couponId);

            CouponIssueResponse response = CouponIssueResponse.builder()
                    .couponId(couponId)
                    .couponName(coupon.getName())
                    .totalIssued(result.getSuccessCount() + result.getFailCount())
                    .successCount(result.getSuccessCount())
                    .failCount(result.getFailCount())
                    .failedUserIds(result.getFailedUserIds())
                    .message(String.format("전체 사용자 쿠폰 발급 완료 (성공: %d, 실패: %d)",
                            result.getSuccessCount(), result.getFailCount()))
                    .build();

            log.info("전체 사용자 쿠폰 발급 완료 - couponId: {}, success: {}, fail: {}", 
                    couponId, result.getSuccessCount(), result.getFailCount());

            return ResponseEntity.ok(CommonResponse.success(
                    "전체 사용자에게 쿠폰이 발급되었습니다",
                    response
            ));

        } catch (IllegalArgumentException e) {
            log.warn("쿠폰 발급 실패 (잘못된 요청) - couponId: {}, error: {}", couponId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("쿠폰 발급 실패 (상태 오류) - couponId: {}, error: {}", couponId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("전체 사용자 쿠폰 발급 중 오류 발생 - couponId: {}", couponId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("전체 사용자 쿠폰 발급 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    //쿠폰 발급 받은 사용자 목록
    @GetMapping("/{couponId}/issued-users")
    public ResponseEntity<CommonResponse<List<UserCouponResponseDTO>>> getIssuedUsers(
            @PathVariable Long couponId) {

        try {
            // User를 fetch join하여 LazyInitializationException 방지
            List<UserCoupon> userCoupons = userCouponRepository.findByCoupon_IdWithUser(couponId);

            List<UserCouponResponseDTO> responses = userCoupons.stream()
                    .map(UserCouponResponseDTO::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰 발급 사용자 목록을 조회했습니다",
                    responses
            ));

        } catch (Exception e) {
            log.error("쿠폰 발급 사용자 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 발급 사용자 목록 조회 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 통계
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<CouponStatsResponse>> getCouponStats() {

        try {
            Long activeCoupons = couponAdminService.countActiveCoupons();
            Long totalUsedCount = couponAdminService.getTotalUsedCount();

            CouponStatsResponse response = new CouponStatsResponse(activeCoupons, totalUsedCount);

            return ResponseEntity.ok(CommonResponse.success(
                    "쿠폰 통계를 조회했습니다",
                    response
            ));

        } catch (Exception e) {
            log.error("쿠폰 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠폰 통계 조회 중 오류가 발생했습니다"));
        }
    }

    //쿠폰 통계 응답 DTO
    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class CouponStatsResponse {
        private Long activeCoupons;
        private Long totalUsedCount;
    }
}
