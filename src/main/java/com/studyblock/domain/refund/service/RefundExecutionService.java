package com.studyblock.domain.refund.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.course.service.LectureOwnershipService;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.payment.client.TossPaymentClient;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.enums.RefundStatus;
import com.studyblock.domain.refund.repository.RefundRepository;
import com.studyblock.domain.payment.service.DailyLimitService;
import com.studyblock.domain.settlement.service.SettlementService;
import com.studyblock.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundExecutionService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TossPaymentClient tossPaymentClient;
    private final WalletService walletService;
    private final DailyLimitService dailyLimitService;
    private final SettlementService settlementService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LectureOwnershipService lectureOwnershipService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processRefund(Refund refund, List<Long> orderItemIds) {
        // Refund 엔티티를 직접 받아서 처리
        // 같은 트랜잭션에서 실행되므로 엔티티를 다시 조회할 필요 없음
        // orderItemIds: 부분 환불 시 선택된 OrderItem IDs, 전체 환불 시 null
        Long refundId = refund.getId();

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("대기 중인 환불만 처리할 수 있습니다");
        }

        Order order = refund.getOrder();
        Payment payment = refund.getPayment();

        boolean cashRefunded = false;
        boolean cookieRefunded = false;

        try {
            if (refund.getRefundAmountCash() != null && refund.getRefundAmountCash() > 0) {
                // 토스 환불 API 호출 (원본 JSON 포함)
                TossPaymentClient.RefundResult refundResult = tossPaymentClient.refundWithRawJson(
                        payment.getPaymentKey(),
                        refund.getRefundAmountCash(),
                        refund.getReason()
                );
                
                // 원본 JSON 저장
                refund.setTossRefundResponse(refundResult.getRawJson());
                
                // 환불 응답에서 핵심 정보 추출 (cancels 배열의 첫 번째 항목)
                extractRefundInfoFromResponse(refund, refundResult.getRawJson());
                
                cashRefunded = true;
                log.info("토스페이먼츠 환불 완료 - refundId: {}, amount: {}",
                        refundId, refund.getRefundAmountCash());
            }

            if (refund.getRefundAmountCookie() != null && refund.getRefundAmountCookie() > 0) {
                walletService.refundCookies(
                        refund.getUser().getId(),
                        refund.getRefundAmountCookie(),
                        order,
                        "주문 환불: " + refund.getReason()
                );
                cookieRefunded = true;
                log.info("쿠키 환불 완료 - refundId: {}, amount: {}",
                        refundId, refund.getRefundAmountCookie());
            }

            boolean isFullRefund = determineFullRefund(order, refund);
            if (isFullRefund) {
                order.refund();
                orderRepository.save(order);
            } else {
                orderRepository.save(order);
            }

            refund.process(null);
            refundRepository.save(refund);

            // 환불된 OrderItem들의 status를 REFUNDED로 변경
            // orderItemIds가 null이면 전체 환불 (모든 OrderItem), 아니면 부분 환불 (선택된 OrderItem만)
            updateRefundedOrderItems(refund, orderItemIds);

            // 수강 취소 및 섹션 소유권 취소 (실패해도 환불은 완료)
            try {
                revokeEnrollmentsAndOwnerships(refund, orderItemIds);
            } catch (Exception e) {
                log.error("수강 취소/섹션 소유권 취소 실패 - refundId: {}, error: {}",
                        refundId, e.getMessage(), e);
                // 수강 취소 실패는 환불 처리에 영향을 주지 않음 (수동 처리 가능)
            }

            // 일일 사용량 차감 (실패해도 환불은 완료)
            try {
                dailyLimitService.refundDailyLimit(
                        refund.getUser().getId(),
                        refund.getRefundAmountCash(),
                        refund.getRefundAmountCookie(),
                        order.getCreatedAt().toLocalDate()
                );
            } catch (Exception e) {
                log.error("일일 사용량 차감 실패 - refundId: {}, error: {}", refundId, e.getMessage(), e);
                // 일일 사용량 차감 실패는 환불 처리에 영향을 주지 않음
            }

            // 정산 불가 처리 (실패해도 환불은 완료)
            try {
                settlementService.markSettlementIneligible(order.getId());
            } catch (Exception e) {
                log.error("정산 불가 처리 실패 - refundId: {}, orderId: {}, error: {}",
                        refundId, order.getId(), e.getMessage(), e);
                // 정산 불가 처리 실패는 환불 처리에 영향을 주지 않음
            }

            log.info("환불 처리 완료 - refundId: {}, orderId: {}", refundId, order.getId());
        } catch (Exception e) {
            log.error("환불 처리 실패 - refundId: {}, error: {}", refundId, e.getMessage(), e);
            if (cashRefunded) {
                log.error("현금 환불 완료 후 후속 실패 - refundId: {}, 수동 확인 필요", refundId);
            }
            if (cookieRefunded) {
                log.error("쿠키 환불 완료 후 후속 실패 - refundId: {}, 수동 확인 필요", refundId);
            }
            try {
                refund.reject(null, "환불 처리 중 오류 발생: " + e.getMessage());
                refundRepository.save(refund);
            } catch (Exception saveEx) {
                log.error("환불 상태 업데이트 실패 - refundId: {}, error: {}", 
                        refundId, saveEx.getMessage(), saveEx);
                // 상태 업데이트 실패는 무시 (이미 예외 발생)
            }
            throw new IllegalStateException("환불 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private boolean determineFullRefund(Order order, Refund refund) {
        if (order.getPaymentType() == PaymentType.CASH) {
            return refund.getRefundAmountCash() != null
                    && order.getTotalAmount() != null
                    && refund.getRefundAmountCash() >= order.getTotalAmount().intValue();
        } else if (order.getPaymentType() == PaymentType.COOKIE) {
            return refund.getRefundAmountCookie() != null
                    && order.getCookieSpent() != null
                    && refund.getRefundAmountCookie() >= order.getCookieSpent();
        } else if (order.getPaymentType() == PaymentType.MIXED) {
            boolean isFullCashRefund = refund.getRefundAmountCash() != null
                    && order.getTotalAmount() != null
                    && refund.getRefundAmountCash() >= (order.getTotalAmount().intValue() - (order.getCookieSpent() != null ? order.getCookieSpent() : 0));
            boolean isFullCookieRefund = refund.getRefundAmountCookie() != null
                    && order.getCookieSpent() != null
                    && refund.getRefundAmountCookie() >= order.getCookieSpent();
            return isFullCashRefund && isFullCookieRefund;
        }
        return false;
    }

    // 토스 환불 응답에서 핵심 정보 추출 (cancels 배열의 첫 번째 항목)
    private void extractRefundInfoFromResponse(Refund refund, String rawJson) {
        try {
            if (rawJson == null || rawJson.isBlank()) {
                log.warn("토스 환불 응답 JSON이 비어있음 - refundId: {}", refund.getId());
                return;
            }

            JsonNode rootNode = objectMapper.readTree(rawJson);
            JsonNode cancelsNode = rootNode.get("cancels");

            // cancels 배열이 있고 첫 번째 항목이 있으면 추출
            if (cancelsNode != null && cancelsNode.isArray() && cancelsNode.size() > 0) {
                JsonNode firstCancel = cancelsNode.get(0);

                // transactionKey를 refundKey에 저장
                if (firstCancel.has("transactionKey")) {
                    String transactionKey = firstCancel.get("transactionKey").asText();
                    if (refund.getRefundKey() == null || refund.getRefundKey().isBlank()) {
                        refund.setRefundKey(transactionKey);
                        log.info("토스 환불 transactionKey 저장 - refundId: {}, transactionKey: {}",
                                refund.getId(), transactionKey);
                    }
                }

                // canceledAt을 processedAt에 저장
                if (firstCancel.has("canceledAt")) {
                    try {
                        String canceledAtStr = firstCancel.get("canceledAt").asText();
                        OffsetDateTime canceledAt = OffsetDateTime.parse(canceledAtStr);
                        refund.setProcessedAt(canceledAt.toLocalDateTime());
                        log.info("토스 환불 canceledAt 추출 - refundId: {}, canceledAt: {}",
                                refund.getId(), canceledAt);
                    } catch (Exception e) {
                        log.warn("토스 환불 canceledAt 파싱 실패 - refundId: {}, error: {}",
                                refund.getId(), e.getMessage());
                    }
                }
            } else {
                log.warn("토스 환불 응답에 cancels 배열이 없음 - refundId: {}", refund.getId());
            }
        } catch (Exception e) {
            log.error("토스 환불 응답 정보 추출 실패 - refundId: {}, error: {}",
                    refund.getId(), e.getMessage(), e);
            // JSON 저장은 완료되었으므로 추출 실패는 무시 (원본 JSON은 보존됨)
        }
    }

    /**
     * 수강 취소 및 섹션 소유권 취소
     * COURSE 타입: CourseEnrollment 취소
     * SECTION 타입: LectureOwnership 취소
     *
     * @param refund 환불 엔티티
     * @param orderItemIds 부분 환불 시 선택된 OrderItem IDs, 전체 환불 시 null
     */
    private void revokeEnrollmentsAndOwnerships(Refund refund, List<Long> orderItemIds) {
        Order order = refund.getOrder();

        // 주문에 연결된 CourseEnrollment 조회 및 취소
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByOrderId(order.getId());
        int courseEnrollmentCancelled = 0;

        if (!enrollments.isEmpty()) {
            for (CourseEnrollment enrollment : enrollments) {
                try {
                    // 부분 환불의 경우 해당 Course가 환불 대상인지 확인
                    if (orderItemIds != null && !orderItemIds.isEmpty()) {
                        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
                        List<Long> refundCourseIds = orderItems.stream()
                                .filter(oi -> orderItemIds.contains(oi.getId()) &&
                                             oi.getItemType() == ItemType.COURSE &&
                                             oi.getCourse() != null)
                                .map(oi -> oi.getCourse().getId())
                                .collect(Collectors.toList());

                        if (!refundCourseIds.contains(enrollment.getCourse().getId())) {
                            log.debug("부분 환불 대상이 아님 - enrollmentId: {}, courseId: {}",
                                    enrollment.getId(), enrollment.getCourse().getId());
                            continue;
                        }
                    }

                    // 환불 가능 여부 확인 (이미 취소된 경우 스킵)
                    if (enrollment.getStatus().name().equals("REVOKED")) {
                        log.debug("이미 취소된 수강신청 - enrollmentId: {}", enrollment.getId());
                        continue;
                    }

                    enrollment.revoke();
                    courseEnrollmentRepository.save(enrollment);
                    courseEnrollmentCancelled++;

                    log.info("CourseEnrollment 취소 완료 - enrollmentId: {}, courseId: {}, userId: {}",
                            enrollment.getId(),
                            enrollment.getCourse().getId(),
                            enrollment.getUser().getId());
                } catch (Exception e) {
                    log.error("CourseEnrollment 취소 실패 - enrollmentId: {}, error: {}",
                            enrollment.getId(), e.getMessage());
                    // 개별 실패는 계속 진행
                }
            }
        }

        // SECTION 타입 OrderItem 확인 및 LectureOwnership 취소
        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
        boolean hasSectionItems = orderItems.stream()
                .anyMatch(oi -> oi.getItemType() == ItemType.SECTION && oi.getSection() != null);

        int lectureOwnershipCancelled = 0;
        if (hasSectionItems) {
            try {
                lectureOwnershipService.revokeOwnershipsByOrder(order.getId());

                // 취소된 섹션 수 계산
                lectureOwnershipCancelled = (int) orderItems.stream()
                        .filter(oi -> oi.getItemType() == ItemType.SECTION && oi.getSection() != null)
                        .filter(oi -> orderItemIds == null || orderItemIds.isEmpty() || orderItemIds.contains(oi.getId()))
                        .count();

                log.info("LectureOwnership 일괄 취소 완료 - orderId: {}, 대상 섹션 수: {}",
                        order.getId(), lectureOwnershipCancelled);
            } catch (Exception e) {
                log.error("LectureOwnership 일괄 취소 실패 - orderId: {}, error: {}",
                        order.getId(), e.getMessage());
            }
        }

        log.info("수강 취소 및 섹션 소유권 취소 완료 - refundId: {}, CourseEnrollment: {}, LectureOwnership: {}",
                refund.getId(), courseEnrollmentCancelled, lectureOwnershipCancelled);
    }

    // 환불된 OrderItem들의 status를 REFUNDED로 변경
    // orderItemIds가 null이면 전체 환불 (Order의 모든 OrderItem), 아니면 부분 환불 (선택된 OrderItem만)
    private void updateRefundedOrderItems(Refund refund, List<Long> orderItemIds) {
        try {
            Order order = refund.getOrder();
            List<OrderItem> orderItems;

            if (orderItemIds == null || orderItemIds.isEmpty()) {
                // 전체 환불: Order의 모든 OrderItem 조회
                orderItems = orderItemRepository.findByOrder_Id(order.getId());
                log.info("전체 환불 - Order의 모든 OrderItem 조회: orderId={}, 개수={}", 
                        order.getId(), orderItems.size());
            } else {
                // 부분 환불: 선택된 OrderItem만 조회
                orderItems = orderItemRepository.findAllById(orderItemIds);
                log.info("부분 환불 - 선택된 OrderItem 조회: refundId={}, orderItemIds={}, 개수={}", 
                        refund.getId(), orderItemIds, orderItems.size());
            }

            if (orderItems.isEmpty()) {
                log.warn("환불 대상 OrderItem을 찾을 수 없음 - refundId: {}, orderId: {}, orderItemIds: {}", 
                        refund.getId(), order.getId(), orderItemIds);
                return;
            }

            // 각 OrderItem의 status를 REFUNDED로 변경
            int updatedCount = 0;
            for (OrderItem orderItem : orderItems) {
                // OrderItem이 해당 Order에 속하는지 확인 (보안)
                if (!orderItem.getOrder().getId().equals(order.getId())) {
                    log.warn("OrderItem이 Order에 속하지 않음 - orderItemId: {}, orderId: {}", 
                            orderItem.getId(), order.getId());
                    continue;
                }

                // 이미 환불된 항목은 건너뛰기 (중복 방지)
                if (orderItem.isRefunded()) {
                    log.debug("이미 환불된 OrderItem - orderItemId: {}", orderItem.getId());
                    continue;
                }

                orderItem.markAsRefunded();
                orderItemRepository.save(orderItem);
                // 변경사항 즉시 반영 (트랜잭션 내에서)
                orderItemRepository.flush();
                updatedCount++;

                log.info("OrderItem 환불 상태 변경 - orderItemId: {}, status: REFUNDED, courseId: {}", 
                        orderItem.getId(), 
                        orderItem.getCourse() != null ? orderItem.getCourse().getId() : 
                        (orderItem.getSection() != null && orderItem.getSection().getCourse() != null ? 
                         orderItem.getSection().getCourse().getId() : null));
            }

            log.info("환불된 OrderItem 상태 변경 완료 - refundId: {}, 대상={}, 변경={}", 
                    refund.getId(), orderItems.size(), updatedCount);

        } catch (Exception e) {
            log.error("환불된 OrderItem 상태 변경 실패 - refundId: {}, error: {}", 
                    refund.getId(), e.getMessage(), e);
            // OrderItem 상태 변경 실패는 환불 처리에 영향을 주지 않음 (로그만 남김)
        }
    }
}


