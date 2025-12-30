package com.studyblock.domain.wallet.controller;

import com.studyblock.domain.wallet.dto.CookieChargeRequest;
import com.studyblock.domain.wallet.dto.CookieChargeResponse;
import com.studyblock.domain.wallet.service.CookieChargeService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//쿠키 충전 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CookieChargeController {

    private final CookieChargeService cookieChargeService;
    private final com.studyblock.domain.wallet.service.CookieChargeQueryService cookieChargeQueryService;
    private final AuthenticationUtils authenticationUtils;

    // 쿠키 충전 요청
    // POST /api/cookie-charge/request
    // request 충전 요청 정보 (쿠키 번들 ID, 수량)
    // authentication 현재 인증된 사용자 정보
    // return 충전 결과
    @PostMapping("/cookie-charge/request")
    public ResponseEntity<CommonResponse<CookieChargeResponse>> requestCharge(
            @RequestBody CookieChargeRequest request,
            Authentication authentication) {

        try {
            // 사용자 ID 추출 (principal 안전 처리)
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);

            log.info("쿠키 충전 요청 - userId: {}, bundleId: {}, quantity: {}",
                    userId, request.getBundleId(), request.getQuantity());

            // 유효성 검증
            if (request.getBundleId() == null) {
                log.warn("쿠키 충전 요청 실패 - 번들 ID 없음: userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("쿠키 번들 ID를 선택해주세요"));
            }

            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                log.warn("쿠키 충전 요청 실패 - 수량 없음: userId: {}, quantity: {}", userId, request.getQuantity());
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("충전 수량을 입력해주세요 (1개 이상)"));
            }

            if (request.getPaymentKey() == null || request.getPaymentKey().isBlank()) {
                log.warn("쿠키 충전 요청 실패 - paymentKey 없음: userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("결제 키(paymentKey)가 필요합니다"));
            }

            if (request.getOrderId() == null || request.getOrderId().isBlank()) {
                log.warn("쿠키 충전 요청 실패 - orderId 없음: userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("주문 ID(orderId)가 필요합니다"));
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                log.warn("쿠키 충전 요청 실패 - amount 없음: userId: {}, amount: {}", userId, request.getAmount());
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("결제 금액(amount)이 필요합니다"));
            }

            // 토스 연동 쿠키 충전 처리
            log.info("🎯 쿠키 충전 시작 - userId: {}, bundleId: {}, quantity: {}, paymentKey: {}, orderId: {}, amount: {}", 
                    userId, request.getBundleId(), request.getQuantity(), request.getPaymentKey(), request.getOrderId(), request.getAmount());
            
            CookieChargeResponse response = cookieChargeService.processChargeWithToss(
                userId, request, request.getPaymentKey(), request.getOrderId(), request.getAmount());

            log.info("🎯 쿠키 충전 완료 - response: {}", response);

            return ResponseEntity.ok(
                CommonResponse.success("쿠키 충전이 완료되었습니다", response)
            );

        } catch (IllegalArgumentException e) {
            log.warn("쿠키 충전 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("쿠키 충전 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠키 충전 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 쿠키 번들 목록 조회
    // GET /api/cookie-charge/bundles
    // return 사용 가능한 쿠키 번들 목록
    @GetMapping("/cookie-charge/bundles")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getBundles() {
        try {
            log.info("쿠키 번들 목록 조회 요청");
            Map<String, Object> bundles = cookieChargeQueryService.getAvailableBundles();
            
            // 에러 필드 확인
            if (bundles.containsKey("error")) {
                log.warn("쿠키 번들 목록 조회 중 경고: {}", bundles.get("error"));
            }
            
            log.info("쿠키 번들 목록 조회 성공 - totalCount: {}", bundles.get("totalCount"));
            return ResponseEntity.ok(
                CommonResponse.success("쿠키 번들 목록을 조회했습니다", bundles)
            );
        } catch (Exception e) {
            log.error("쿠키 번들 목록 조회 중 예상치 못한 오류 발생", e);
            // 빈 결과 반환
            Map<String, Object> emptyResult = new java.util.HashMap<>();
            emptyResult.put("bundles", new java.util.ArrayList<>());
            emptyResult.put("totalCount", 0);
            return ResponseEntity.ok(
                CommonResponse.success("쿠키 번들 목록을 조회했습니다", emptyResult)
            );
        }
    }

    // 쿠키 패키지 목록 조회 (프론트엔드 호환용)
    // GET /api/cookie-packages
    // return 사용 가능한 쿠키 패키지 목록
    @GetMapping("/cookie-packages")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getCookiePackages() {
        try {
            log.info("쿠키 패키지 목록 조회 요청");
            Map<String, Object> bundles = cookieChargeQueryService.getAvailableBundles();
            
            // 에러 필드 확인
            if (bundles.containsKey("error")) {
                log.warn("쿠키 패키지 목록 조회 중 경고: {}", bundles.get("error"));
                // 에러가 있어도 빈 리스트를 반환하도록 처리
            }
            
            log.info("쿠키 패키지 목록 조회 성공 - totalCount: {}", bundles.get("totalCount"));
            return ResponseEntity.ok(
                CommonResponse.success("쿠키 패키지 목록을 조회했습니다", bundles)
            );
        } catch (Exception e) {
            log.error("쿠키 패키지 목록 조회 중 예상치 못한 오류 발생", e);
            // 빈 결과 반환
            Map<String, Object> emptyResult = new java.util.HashMap<>();
            emptyResult.put("bundles", new java.util.ArrayList<>());
            emptyResult.put("totalCount", 0);
            return ResponseEntity.ok(
                CommonResponse.success("쿠키 패키지 목록을 조회했습니다", emptyResult)
            );
        }
    }

    // 테스트용 간단한 엔드포인트
    @GetMapping("/cookie-charge/test")
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Map.of(
            "message", "테스트 성공",
            "timestamp", System.currentTimeMillis()
        ));
    }

    // 사용자 쿠키 잔액 조회
    // GET /api/wallet/balance
    @GetMapping("/wallet/balance")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getBalance(
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return createBalanceResponse(0L, 0L);
            }

            Long amount = getCookieBalanceSafely(userId);
            return createBalanceResponse(userId, amount);

        } catch (Exception e) {
            log.error("쿠키 잔액 조회 중 예상치 못한 오류 발생", e);
            return createBalanceResponse(0L, 0L);
        }
    }

    private Long getCookieBalanceSafely(Long userId) {
        try {
            Long amount = cookieChargeQueryService.getCookieBalance(userId);
            return amount != null ? amount : 0L;
        } catch (IllegalStateException ex) {
            log.warn("지갑 잔액 정보가 없어 0으로 응답합니다 - userId: {} | msg: {}", userId, ex.getMessage());
            return 0L;
        } catch (Exception e) {
            log.error("쿠키 잔액 조회 중 오류 발생 - userId: {}", userId, e);
            return 0L;
        }
    }

    private ResponseEntity<CommonResponse<Map<String, Object>>> createBalanceResponse(Long userId, Long amount) {
        return ResponseEntity.ok(
            CommonResponse.success("쿠키 잔액을 조회했습니다", Map.of(
                "userId", userId != null ? userId : 0L,
                "amount", amount
            ))
        );
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

    private com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse createEmptyCookieChargeHistoryPage() {
        return com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse.builder()
                .balanceAmount(0L)
                .totalCharged(0)
                .totalUsed(0)
                .items(new java.util.ArrayList<>())
                .build();
    }

    private ResponseEntity<CommonResponse<com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse>> createHistoryPageResponse() {
        return ResponseEntity.ok(
                CommonResponse.success("쿠키 충전 내역을 조회했습니다", createEmptyCookieChargeHistoryPage())
        );
    }

    // 나의 쿠키 충전 내역 (요약 + 목록)
    // GET /api/cookie-charge/my-history
    @GetMapping("/cookie-charge/my-history")
    public ResponseEntity<CommonResponse<com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse>> myHistory(
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return createHistoryPageResponse();
            }

            var page = getChargeHistoryPageSafely(userId);
            log.debug("쿠키 충전 내역 조회 완료 - userId: {}, itemsCount: {}", 
                    userId, page.getItems() != null ? page.getItems().size() : 0);
            
            return ResponseEntity.ok(
                    CommonResponse.success("쿠키 충전 내역을 조회했습니다", page)
            );
            
        } catch (Exception e) {
            log.error("쿠키 충전 내역 조회 중 예상치 못한 오류 발생", e);
            return createHistoryPageResponse();
        }
    }

    private com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse getChargeHistoryPageSafely(Long userId) {
        try {
            var page = cookieChargeQueryService.getMyChargeHistoryPage(userId);
            if (page == null) {
                return createEmptyCookieChargeHistoryPage();
            }
            
            if (page.getItems() == null) {
                return com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse.builder()
                        .balanceAmount(page.getBalanceAmount() != null ? page.getBalanceAmount() : 0L)
                        .totalCharged(page.getTotalCharged() != null ? page.getTotalCharged() : 0)
                        .totalUsed(page.getTotalUsed() != null ? page.getTotalUsed() : 0)
                        .items(new java.util.ArrayList<>())
                        .build();
            }
            
            return page;
        } catch (IllegalArgumentException e) {
            log.warn("쿠키 충전 내역 조회 실패 (사용자 없음) - userId: {}, error: {}", userId, e.getMessage());
            return createEmptyCookieChargeHistoryPage();
        } catch (Exception e) {
            log.error("쿠키 충전 내역 조회 중 서비스 레벨 오류 - userId: {}", userId, e);
            return createEmptyCookieChargeHistoryPage();
        }
    }

    // 나의 쿠키 충전 내역 (주문 단위 그룹)
    // GET /api/cookie-charge/my-history-grouped
    @GetMapping("/cookie-charge/my-history-grouped")
    public ResponseEntity<CommonResponse<java.util.List<com.studyblock.domain.wallet.dto.CookieChargeGroupedResponse>>> myHistoryGrouped(
            Authentication authentication) {
        try {
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            var list = cookieChargeQueryService.getMyChargeHistoryGrouped(userId);
            return ResponseEntity.ok(
                    CommonResponse.success("쿠키 충전 내역(그룹)을 조회했습니다", list)
            );
        } catch (Exception e) {
            log.error("쿠키 충전 내역(그룹) 조회 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠키 충전 내역(그룹) 조회 중 오류가 발생했습니다"));
        }
    }

    // 나의 쿠키 사용 내역
    // GET /api/cookie-usage/my-history
    @GetMapping("/cookie-usage/my-history")
    public ResponseEntity<CommonResponse<java.util.List<com.studyblock.domain.wallet.dto.CookieUsageHistoryResponse>>> myUsageHistory(
            Authentication authentication) {
        try {
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            var list = cookieChargeQueryService.getMyUsageHistory(userId);
            return ResponseEntity.ok(
                    CommonResponse.success("쿠키 사용 내역을 조회했습니다", list)
            );
        } catch (Exception e) {
            log.error("쿠키 사용 내역 조회 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("쿠키 사용 내역 조회 중 오류가 발생했습니다"));
        }
    }
}
