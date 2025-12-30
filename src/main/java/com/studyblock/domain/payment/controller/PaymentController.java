package com.studyblock.domain.payment.controller;

import com.studyblock.domain.payment.dto.OrderResponse;
import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.PaymentConfirmResponse;
import com.studyblock.domain.payment.dto.PaymentFailureLogRequest;
import com.studyblock.domain.payment.dto.PaymentPageResponse;
import com.studyblock.domain.payment.service.OrderService;
import com.studyblock.domain.payment.service.PaymentPageService;
import com.studyblock.domain.payment.service.PaymentService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import com.studyblock.global.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    // 실제 결제 처리 컨트롤러

    private final PaymentService paymentService;
    private final PaymentPageService paymentPageService;
    private final OrderService orderService;
    private final AuthenticationUtils authenticationUtils;

    // 결제 페이지 정보 조회 API (단일 강의)
    @GetMapping("/page/{courseId}")
    public ResponseEntity<PaymentPageResponse> getPaymentPageInfo(
            @PathVariable Long courseId,
            Authentication authentication) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.error("인증되지 않은 사용자가 결제 페이지 정보를 조회하려고 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // JWT 토큰에서 사용자 정보 추출
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);

            PaymentPageResponse response = paymentPageService.getPaymentPageInfo(courseId, userId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 강의를 찾을 수 없음
            log.error("결제 페이지 정보 조회 실패 - courseId: {}, error: {}", courseId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            // 예상치 못한 에러
            log.error("결제 페이지 정보 조회 중 오류 발생 - courseId: {}", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 결제 페이지 정보 조회 API (여러 강의)
    @GetMapping("/page/multiple")
    public ResponseEntity<PaymentPageResponse> getPaymentPageInfoForMultipleCourses(
            @RequestParam("courseIds") List<Long> courseIds,
            Authentication authentication) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.error("인증되지 않은 사용자가 결제 페이지 정보를 조회하려고 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // JWT 토큰에서 사용자 정보 추출
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);

            log.info("여러 강의 결제 페이지 정보 조회 요청 - courseIds: {}, userId: {}", courseIds, userId);

            PaymentPageResponse response = paymentPageService.getPaymentPageInfoForMultipleCourses(courseIds, userId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 강의를 찾을 수 없음 또는 잘못된 요청
            log.error("결제 페이지 정보 조회 실패 (여러 강의) - courseIds: {}, error: {}", courseIds, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            // 예상치 못한 에러
            log.error("결제 페이지 정보 조회 중 오류 발생 (여러 강의) - courseIds: {}", courseIds, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 결제 승인 API
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @RequestBody PaymentConfirmRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.error("인증되지 않은 사용자가 결제를 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(PaymentConfirmResponse.fail("인증이 필요합니다"));
        }

        try {
            // JWT 토큰에서 사용자 정보 추출
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);

            // IP 주소 및 User-Agent 추출
            String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
            String userAgent = RequestUtils.getUserAgent(httpRequest);
            String paymentSource = RequestUtils.determinePaymentSource(httpRequest);

            PaymentConfirmResponse response = paymentService.confirmPayment(
                    request, userId, ipAddress, userAgent, paymentSource);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 잘못된 요청 (사용자 없음, 금액 불일치 등)
            PaymentConfirmResponse errorResponse = PaymentConfirmResponse.fail(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (IllegalStateException e) {
            // 토스 승인 실패 (결제 처리 중 에러)
            PaymentConfirmResponse errorResponse = PaymentConfirmResponse.fail(e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);

        } catch (Exception e) {
            // 예상치 못한 에러
            PaymentConfirmResponse errorResponse = PaymentConfirmResponse.fail(
                "결제 처리 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 결제 위젯 단계 실패 로그
    @PostMapping("/failure-log")
    public ResponseEntity<CommonResponse<Void>> logPaymentFailure(
            @RequestBody PaymentFailureLogRequest request) {
        
        try {
            // 로그 기록
            paymentService.logPaymentFailure(request);
            
            return ResponseEntity.ok(CommonResponse.success("실패 로그가 기록되었습니다"));
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("로그 기록 실패: " + e.getMessage()));
        }
    }
    
    // 내 주문 목록 조회
    @GetMapping("/my-orders")
    public ResponseEntity<CommonResponse<List<OrderResponse>>> getMyOrders(
            Authentication authentication) {

        // 인증 확인
        if (!authenticationUtils.isAuthenticated(authentication)) {
            log.error("인증되지 않은 사용자가 주문 목록을 조회하려고 시도했습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다"));
        }

        try {
            // JWT 토큰에서 사용자 정보 추출
            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);

            List<OrderResponse> orders = orderService.getUserOrders(userId);

            log.info("주문 목록 조회 완료 - userId: {}, count: {}", userId, orders.size());
            return ResponseEntity.ok(CommonResponse.success("주문 목록을 조회했습니다", orders));

        } catch (Exception e) {
            log.error("주문 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("주문 목록 조회 실패: " + e.getMessage()));
        }
    }

}

