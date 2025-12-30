package com.studyblock.domain.payment.controller;

import com.studyblock.domain.payment.dto.PaymentValidationRequest;
import com.studyblock.domain.payment.dto.PaymentValidationResponse;
import com.studyblock.domain.payment.service.PaymentValidationService;
import com.studyblock.global.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//결제 검증 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentValidationController {

    private final PaymentValidationService paymentValidationService;
    private final com.studyblock.domain.payment.service.PendingOrderService pendingOrderService;

    // 결제 검증
    @PostMapping("/validate")
    public ResponseEntity<CommonResponse<PaymentValidationResponse>> validatePayment(
            @RequestBody PaymentValidationRequest request,
            Authentication authentication) {
        try {
            Long authUserId = extractUserId(authentication);
            if (request.getUserId() != null && !request.getUserId().equals(authUserId)) {
                log.warn("결제 검증 요청의 userId({})가 인증 사용자({})와 달라 무시합니다.", request.getUserId(), authUserId);
            }

            // 원본 request 로깅 (프론트에서 보낸 데이터 확인)
            log.info("결제 검증 요청 (원본) - itemsCount: {}, totalAmount: {}, userCouponId: {}", 
                    request.getItems() != null ? request.getItems().size() : 0,
                    request.getTotalAmount(),
                    request.getUserCouponId());
            
            if (request.getItems() != null) {
                for (int i = 0; i < request.getItems().size(); i++) {
                    var item = request.getItems().get(i);
                    log.info("결제 검증 요청 (원본) Item[{}] - courseId: {}, quantity: {}, unitPrice: {}", 
                            i, item.getCourseId(), item.getQuantity(), item.getUnitPrice());
                }
            }

            // 프론트의 userId는 무시하고 인증 사용자 ID를 주입하여 서비스 호출
            PaymentValidationRequest effectiveRequest = PaymentValidationRequest.builder()
                    .userId(authUserId)
                    .items(request.getItems())
                    .userCouponId(request.getUserCouponId())
                    .cookieAmount(request.getCookieAmount())
                    .totalAmount(request.getTotalAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            log.info("결제 검증 요청 (처리용) - authUserId: {}, itemsCount: {}", authUserId, 
                    effectiveRequest.getItems() != null ? effectiveRequest.getItems().size() : 0);
            
            // 각 item 상세 정보 로깅
            if (effectiveRequest.getItems() != null) {
                for (int i = 0; i < effectiveRequest.getItems().size(); i++) {
                    var item = effectiveRequest.getItems().get(i);
                    log.info("결제 검증 요청 (처리용) Item[{}] - courseId: {}, quantity: {}, unitPrice: {}", 
                            i, item.getCourseId(), item.getQuantity(), item.getUnitPrice());
                }
            }

            paymentValidationService.validatePayment(effectiveRequest);

            // PENDING 주문/아이템 생성 및 주문번호 반환 (B안)
            String orderNumber = pendingOrderService.createPendingOrder(authUserId, effectiveRequest);

            PaymentValidationResponse response = PaymentValidationResponse.builder()
                    .valid(true)
                    .message("결제 검증이 완료되었습니다")
                    .orderNumber(orderNumber)
                    .build();
            
            return ResponseEntity.ok(CommonResponse.success("결제 검증 성공", response));
        } catch (IllegalArgumentException e) {
            log.warn("결제 검증 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("결제 검증 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("결제 검증 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(CommonResponse.error("결제 검증 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 인증 정보에서 사용자 ID 추출
    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("인증 정보가 없습니다");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof com.studyblock.domain.user.entity.User u) {
            return u.getId();
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            String username = ud.getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException ignore) {
                throw new IllegalStateException("UserDetails.username에서 사용자 ID를 추출할 수 없습니다");
            }
        }
        if (principal instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignore) {
                throw new IllegalStateException("String principal에서 사용자 ID를 추출할 수 없습니다");
            }
        }
        throw new IllegalStateException("인증 정보에서 사용자 ID를 추출할 수 없습니다");
    }

    // 쿠키 충전 검증 (프론트엔드 호환용)
    @PostMapping("/validate-cookie-charge")
    public ResponseEntity<CommonResponse<PaymentValidationResponse>> validateCookieCharge(
            @RequestBody PaymentValidationRequest request) {
        try {
            log.info("쿠키 충전 검증 요청 - userId: {}", request.getUserId());
            
            // 쿠키 충전은 별도 API로 처리하도록 리다이렉트
            PaymentValidationResponse response = PaymentValidationResponse.builder()
                    .valid(true)
                    .message("쿠키 충전은 /cookie-charge/request API를 사용해주세요")
                    .redirectUrl("/api/cookie-charge/request")
                    .build();
            
            return ResponseEntity.ok(CommonResponse.success("쿠키 충전 검증 성공", response));
        } catch (Exception e) {
            log.error("쿠키 충전 검증 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(CommonResponse.error("쿠키 충전 검증 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}