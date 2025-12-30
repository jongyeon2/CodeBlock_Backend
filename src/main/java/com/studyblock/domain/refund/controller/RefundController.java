package com.studyblock.domain.refund.controller;

import com.studyblock.domain.refund.dto.RefundRequest;
import com.studyblock.domain.refund.dto.RefundRequestFrontend;
import com.studyblock.domain.refund.dto.RefundResponse;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.service.RefundService;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.util.AuthenticationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final AuthenticationUtils authenticationUtils;

    // 1. 환불 요청 (전체/부분 공용: orderItemIds 또는 partialAmount가 오면 부분환불로 처리)
    @PostMapping
    public ResponseEntity<CommonResponse<RefundResponse>> requestRefund(
            @RequestBody RefundRequest request,
            Authentication authentication) {

        try {
            // 사용자 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            User user = authenticationUtils.extractAuthenticatedUser(authentication);
            
            Refund refund;
            if ((request.getOrderItemIds() != null && !request.getOrderItemIds().isEmpty())
                || request.getPartialAmount() != null) {
                refund = refundService.requestPartialRefund(
                        user.getId(),
                        request.getOrderId(),
                        request.getOrderItemIds(),
                        request.getPartialAmount(),
                        request.getReason()
                );
            } else {
                // 전체 환불
                refund = refundService.requestRefund(
                        user.getId(),
                        request.getOrderId(),
                        request.getReason(),
                        request.getIdempotencyKey()
                );
            }

            RefundResponse response = RefundResponse.from(refund);
            
            return ResponseEntity.ok(CommonResponse.success("환불 요청이 완료되었습니다", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("환불 요청 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 요청 처리 중 오류가 발생했습니다"));
        }
    }

    // 프론트 호환 엔드포인트: paymentId, courseId 기반
    // POST /api/refunds/request
    @PostMapping("/request")
    public ResponseEntity<CommonResponse<RefundResponse>> requestRefundFromFrontend(
            @RequestBody RefundRequestFrontend request,
            Authentication authentication) {

        try {
            logRefundRequest(request);
            User user = validateAndGetUser(authentication);
            Order order = findOrderFromRequest(request);
            validateOrderOwnership(order, user);

            Refund refund = processRefundRequest(request, order, user);

            return ResponseEntity.ok(CommonResponse.success("환불 요청이 완료되었습니다",
                                    RefundResponse.from(refund)));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("환불 요청 실패(프론트 호환)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 요청 처리 중 오류가 발생했습니다"));
        }
    }

    // 2. 내 환불 내역 조회 (페이징 지원)
    @GetMapping("/my")
    public ResponseEntity<CommonResponse<Page<RefundResponse>>> getMyRefunds(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        try {
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);

            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Refund> refunds = refundService.getUserRefunds(userId, pageable);
            Page<RefundResponse> responses = refunds.map(RefundResponse::from);

            return ResponseEntity.ok(CommonResponse.success(responses));

        } catch (Exception e) {
            log.error("환불 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 내역 조회 중 오류가 발생했습니다"));
        }
    }

    // 3. 환불 상세 조회
    @GetMapping("/{refundId}")
    public ResponseEntity<CommonResponse<RefundResponse>> getRefund(
            @PathVariable Long refundId,
            Authentication authentication) {

        try {
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            Refund refund = refundService.getRefund(refundId);

            // 본인의 환불인지 확인
            if (!refund.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.error("접근 권한이 없습니다"));
            }

            RefundResponse response = RefundResponse.from(refund);

            return ResponseEntity.ok(CommonResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("환불 상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 상세 조회 중 오류가 발생했습니다"));
        }
    }

    // 4. 주문의 환불 내역 조회
    @GetMapping("/order/{orderId}")
    public ResponseEntity<CommonResponse<List<RefundResponse>>> getOrderRefunds(
            @PathVariable Long orderId,
            Authentication authentication) {

        try {
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            List<Refund> refunds = refundService.getOrderRefunds(orderId);
            List<RefundResponse> responses = refunds.stream()
                    .map(RefundResponse::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(CommonResponse.success(responses));

        } catch (Exception e) {
            log.error("주문 환불 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("주문 환불 내역 조회 중 오류가 발생했습니다"));
        }
    }

    // 5. 대기 중인 환불 목록 조회 (페이징 지원, 관리자용)
    @GetMapping("/admin/pending")
    public ResponseEntity<CommonResponse<Page<RefundResponse>>> getPendingRefunds(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        try {
            // TODO: 관리자 권한 확인 필요
            
            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Refund> refunds = refundService.getPendingRefunds(pageable);
            Page<RefundResponse> responses = refunds.map(RefundResponse::from);
            
            return ResponseEntity.ok(CommonResponse.success(responses));

        } catch (Exception e) {
            log.error("대기 중인 환불 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 목록 조회 중 오류가 발생했습니다"));
        }
    }

    // 6. 환불 승인 (관리자용)
    @PostMapping("/{refundId}/approve")
    public ResponseEntity<CommonResponse<Void>> approveRefund(
            @PathVariable Long refundId,
            Authentication authentication) {

        try {
            // TODO: 관리자 권한 확인 필요
            User admin = authenticationUtils.extractAuthenticatedUser(authentication);

            refundService.approveRefund(refundId, admin.getId());

            return ResponseEntity.ok(CommonResponse.success("환불이 승인되었습니다"));

        } catch (Exception e) {
            log.error("환불 승인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 승인 중 오류가 발생했습니다"));
        }
    }

    // 7. 환불 거부 (관리자용)
    @PostMapping("/{refundId}/reject")
    public ResponseEntity<CommonResponse<Void>> rejectRefund(
            @PathVariable Long refundId,
            @RequestParam String reason,
            Authentication authentication) {

        try {
            // TODO: 관리자 권한 확인 필요
            User admin = authenticationUtils.extractAuthenticatedUser(authentication);

            refundService.rejectRefund(refundId, admin.getId(), reason);

            return ResponseEntity.ok(CommonResponse.success("환불이 거부되었습니다"));

        } catch (Exception e) {
            log.error("환불 거부 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("환불 거부 중 오류가 발생했습니다"));
        }
    }

    // ===========================
    // Private Helper Methods(여기서만 사용해서 바깥으로 빼지 않음 아래는 순서대로 작성됨)
    // ===========================

    // 요청 데이터 로깅 (디버깅용)
    private void logRefundRequest(RefundRequestFrontend request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            log.info("환불 요청 수신 (전체 JSON): {}", requestJson);
        } catch (Exception e) {
            log.warn("요청 데이터 JSON 변환 실패: {}", e.getMessage());
        }

        String effectiveOrderNumber = request.getEffectiveOrderNumber();
        log.info("환불 요청 수신 - paymentId: {}, orderNumber: {}, orderNumberSnake: {}, orderId: {}, effectiveOrderNumber: {}, refundType: {}, reason: {}, courseId: {}",
                request.getPaymentId(), request.getOrderNumber(), request.getOrderNumberSnake(),
                request.getOrderId(), effectiveOrderNumber, request.getRefundType(),
                request.getReason(), request.getCourseId());
    }

    // 인증 확인 및 사용자 추출
    private User validateAndGetUser(Authentication authentication) {
        if (!authenticationUtils.isAuthenticated(authentication)) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
        return authenticationUtils.extractAuthenticatedUser(authentication);
    }

    // paymentId 또는 orderNumber로 Order 찾기
    private Order findOrderFromRequest(RefundRequestFrontend request) {
        String effectiveOrderNumber = request.getEffectiveOrderNumber();

        if (effectiveOrderNumber != null && !effectiveOrderNumber.isBlank()) {
            // orderNumber 우선 사용
            Order order = orderRepository.findByOrderNumberWithUser(effectiveOrderNumber)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "주문을 찾을 수 없습니다. orderNumber=" + effectiveOrderNumber));
            log.info("OrderNumber를 통해 Order 조회 - orderNumber: {}, orderId: {}",
                    effectiveOrderNumber, order.getId());
            return order;
        } else if (request.getPaymentId() != null) {
            // paymentId로 Order 찾기
            Payment payment = paymentRepository.findByIdWithOrderAndUser(request.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "결제 정보를 찾을 수 없습니다. paymentId=" + request.getPaymentId()));
            log.info("Payment를 통해 Order 조회 - paymentId: {}, orderId: {}",
                    request.getPaymentId(), payment.getOrder().getId());
            return payment.getOrder();
        } else {
            throw new IllegalArgumentException("paymentId 또는 orderNumber 중 하나는 필수입니다.");
        }
    }

    // Order 소유자 확인
    private void validateOrderOwnership(Order order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 주문만 환불할 수 있습니다.");
        }
    }

    // 전체/부분 환불 요청 처리
    private Refund processRefundRequest(RefundRequestFrontend request, Order order, User user) {
        String refundType = request.getRefundType();

        if ("PARTIAL".equalsIgnoreCase(refundType)) {
            // 부분 환불
            PartialRefundTarget target = resolvePartialRefundItems(request, order.getId());
            return refundService.requestPartialRefund(
                    user.getId(),
                    order.getId(),
                    target.getOrderItemIds(),
                    (int) target.getAmount(),
                    request.getReason()
            );
        } else {
            // 전체 환불
            return refundService.requestRefund(
                    user.getId(),
                    order.getId(),
                    request.getReason(),
                    request.getIdempotencyKey()
            );
        }
    }

    // 부분 환불 대상 OrderItem과 금액 계산
    private PartialRefundTarget resolvePartialRefundItems(RefundRequestFrontend request, Long orderId) {
        List<Long> targetOrderItemIds = new java.util.ArrayList<>();
        long targetAmount = 0L;

        // 1) orderItemIds가 직접 제공된 경우 (최우선)
        if (request.getOrderItemIds() != null && !request.getOrderItemIds().isEmpty()) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(orderId);
            for (Long orderItemId : request.getOrderItemIds()) {
                OrderItem item = orderItems.stream()
                        .filter(oi -> oi.getId().equals(orderItemId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            "주문에 속하지 않은 주문 항목입니다. orderItemId=" + orderItemId));
                targetOrderItemIds.add(item.getId());
                targetAmount += item.getTotalFinalAmount();
            }
            log.info("부분환불 - orderItemIds로 처리: 개수={}, 총액={}",
                    targetOrderItemIds.size(), targetAmount);
        }
        // 2) refundItemIds가 제공된 경우
        else if (request.getRefundItemIds() != null && !request.getRefundItemIds().isEmpty()) {
            for (Long courseId : request.getRefundItemIds()) {
                OrderItem orderItem = orderItemRepository
                        .findByOrder_IdAndCourse_Id(orderId, courseId)
                        .orElseThrow(() -> new IllegalArgumentException(
                            "주문 내 강의를 찾을 수 없습니다. orderId=" + orderId + ", courseId=" + courseId));
                targetOrderItemIds.add(orderItem.getId());
                targetAmount += orderItem.getTotalFinalAmount();
            }
            log.info("부분환불 - refundItemIds로 처리: 개수={}, 총액={}",
                    request.getRefundItemIds().size(), targetAmount);
        }
        // 3) courseIds가 제공된 경우
        else if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            for (Long courseId : request.getCourseIds()) {
                OrderItem orderItem = orderItemRepository
                        .findByOrder_IdAndCourse_Id(orderId, courseId)
                        .orElseThrow(() -> new IllegalArgumentException(
                            "주문 내 강의를 찾을 수 없습니다. orderId=" + orderId + ", courseId=" + courseId));
                targetOrderItemIds.add(orderItem.getId());
                targetAmount += orderItem.getTotalFinalAmount();
            }
            log.info("부분환불 - courseIds로 처리: 개수={}, 총액={}",
                    request.getCourseIds().size(), targetAmount);
        }
        // 4) courseId 단일 강의
        else if (request.getCourseId() != null) {
            OrderItem orderItem = orderItemRepository
                    .findByOrder_IdAndCourse_Id(orderId, request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "주문 내 강의를 찾을 수 없습니다. orderId=" + orderId + ", courseId=" + request.getCourseId()));
            targetOrderItemIds.add(orderItem.getId());

            // partialPercent 적용 가능
            Integer partialPercent = request.getPartialPercent();
            long orderItemTotal = orderItem.getTotalFinalAmount();

            if (partialPercent != null && partialPercent > 0 && partialPercent <= 100) {
                targetAmount = (long) Math.max(1, Math.round(orderItemTotal * (partialPercent / 100.0)));
                log.info("부분환불 - 단일 강의 + 비율: courseId={}, 비율={}%, 원래금액={}, 환불금액={}",
                        request.getCourseId(), partialPercent, orderItemTotal, targetAmount);
            } else {
                targetAmount = orderItemTotal;
                log.info("부분환불 - 단일 강의 전체: courseId={}, 환불금액={}",
                        request.getCourseId(), targetAmount);
            }
        } else {
            throw new IllegalArgumentException(
                "부분 환불에는 courseId, refundItemIds, courseIds 또는 orderItemIds 중 하나가 필요합니다.");
        }

        if (targetOrderItemIds.isEmpty()) {
            throw new IllegalArgumentException("부분환불 대상이 없습니다.");
        }

        if (targetAmount <= 0) {
            throw new IllegalArgumentException("부분환불 금액이 올바르지 않습니다.");
        }

        return new PartialRefundTarget(targetOrderItemIds, targetAmount);
    }

    // 부분 환불 대상을 담는 내부 클래스
    private static class PartialRefundTarget {
        private final List<Long> orderItemIds;
        private final long amount;

        public PartialRefundTarget(List<Long> orderItemIds, long amount) {
            this.orderItemIds = orderItemIds;
            this.amount = amount;
        }

        public List<Long> getOrderItemIds() {
            return orderItemIds;
        }

        public long getAmount() {
            return amount;
        }
    }
}

