package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.dto.PaymentConfirmRequest;
import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.entity.PaymentMethodDetails;
import com.studyblock.domain.payment.enums.PaymentMethod;
import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.payment.repository.PaymentMetadataRepository;
import com.studyblock.domain.payment.repository.PaymentMethodDetailsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.payment.service.support.PaymentMethodMapper;
import com.studyblock.domain.payment.service.support.TossMethodDetailsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//결제생성 전담 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final PaymentRepository paymentRepository;
    private final PaymentMetadataRepository paymentMetadataRepository;
    private final PaymentMethodDetailsRepository paymentMethodDetailsRepository;
    private final ObjectMapper objectMapper;

    //결제생성 및 저장
    // @Transactional 제거 - PaymentService.confirmPayment()의 트랜잭션에 참여
    public Payment createPayment(Order order, PaymentConfirmRequest request, 
                            TossPaymentResponse tossResponse, String failureReason,
                            String ipAddress, String userAgent, String paymentSource) {

        final Long resolvedAmount = (tossResponse != null && tossResponse.getTotalAmount() != null)
                ? tossResponse.getTotalAmount().longValue()
                : (request.getAmount() != null ? request.getAmount().longValue() : 0L);

        Payment payment = Payment.builder()
                .order(order)
                .method(tossResponse != null ? PaymentMethodMapper.fromTossMethod(tossResponse.getMethod()) : PaymentMethod.CARD)
                .provider("toss")
                .amount(resolvedAmount)
                .paymentKey(request.getPaymentKey())
                .merchantUid(request.getOrderId())
                .idempotencyKey(request.getPaymentKey())
                .build();

        if (failureReason == null) {
            // 성공: 승인 완료 상태로 변경
            payment.capture();

            // 주의: Order 상태 변경은 이미 OrderReuseService 또는 OrderCreationService에서 처리됨
            // PaymentCreationService는 Payment 생성 전담이므로 Order는 건드리지 않음

            // 승인 시각 설정
            if (tossResponse != null && tossResponse.getApprovedAt() != null) {
                try {
                    payment.setApprovedAt(java.time.OffsetDateTime.parse(tossResponse.getApprovedAt()).toLocalDateTime());
                } catch (Exception ignore) {
                    // 형식 이슈 시 무시
                }
            }

            // 결제 수단별 상세 정보는 JSON으로 저장 (NOT NULL 보장)
            String methodDetailsJson = TossMethodDetailsConverter.toJson(tossResponse);
            if (methodDetailsJson == null || methodDetailsJson.isBlank()) {
                methodDetailsJson = "{}"; // 최소 구조로 채움
            }
           
            // 토스 원본 응답 JSON 저장 (NULL 방지)
            payment.setTossResponse(safeWriteJson(tossResponse));
        } else {
            // 실패: 실패 상태로 저장
            payment.fail(failureReason);
        }

        // Payment를 먼저 저장 (ID 생성 필요)
        payment = paymentRepository.save(payment);
        
        // Payment 저장 후 연관 엔티티 저장
        if (failureReason == null) {
            // PaymentMetadata 저장
            savePaymentMetadata(payment, tossResponse, ipAddress, userAgent, paymentSource);
            // PaymentMethodDetails 저장
            savePaymentMethodDetails(payment, tossResponse);
        }
        
        return payment;
    }

    private String safeWriteJson(TossPaymentResponse tossResponse) {
        try {
            if (tossResponse == null) return "{}";
            return "{\"version\":\"" + (tossResponse.getVersion() != null ? tossResponse.getVersion() : "") + "\"}";
        } catch (Exception e) {
            log.warn("토스 원본 응답 JSON 저장 실패: {}", e.getMessage());
            return "{}";
        }
    }

    private void savePaymentMetadata(Payment payment, TossPaymentResponse tossResponse, 
                                    String ipAddress, String userAgent, String paymentSource) {
        try {
            // orderName을 100자로 제한 (DB 컬럼 크기 제한)
            String orderName = null;
            if (tossResponse != null && tossResponse.getOrderName() != null) {
                orderName = tossResponse.getOrderName();
                if (orderName.length() > 100) {
                    log.warn("orderName이 100자를 초과하여 잘립니다 - 원본 길이: {}, 원본: {}", 
                            orderName.length(), orderName);
                    orderName = orderName.substring(0, 100);
                }
            }
            
            com.studyblock.domain.payment.entity.PaymentMetadata metadata = 
                com.studyblock.domain.payment.entity.PaymentMetadata.builder()
                    .payment(payment)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .paymentSource(paymentSource)
                    .orderName(orderName)
                    .country(tossResponse != null ? tossResponse.getCountry() : null)
                    .receiptUrl(tossResponse != null && tossResponse.getReceipt() != null 
                        ? tossResponse.getReceipt().getUrl() : null)
                    .tossApiVersion(tossResponse != null ? tossResponse.getVersion() : null)
                    .build();
            paymentMetadataRepository.save(metadata);
            log.info("PaymentMetadata 저장 완료 - paymentId: {}", payment.getId());
        } catch (Exception e) {
            log.error("PaymentMetadata 저장 중 오류 - paymentId: {}, error: {}", 
                    payment.getId(), e.getMessage(), e);
            // 예외 발생 시 재throw하여 트랜잭션 롤백 보장
            throw new IllegalStateException("PaymentMetadata 저장 실패: " + e.getMessage(), e);
        }
    }

    private void savePaymentMethodDetails(Payment payment, TossPaymentResponse tossResponse) {
        try {
            if (tossResponse == null) {
                log.warn("TossPaymentResponse가 null입니다. PaymentMethodDetails 저장 건너뜀");
                return;
            }

            String methodDetailsJson = TossMethodDetailsConverter.toJson(tossResponse);
            if (methodDetailsJson == null || methodDetailsJson.isBlank()) {
                methodDetailsJson = "{}";
            }

            // 결제 수단 타입 결정
            PaymentMethodDetails.MethodType methodType = PaymentMethodDetails.MethodType.CARD;
            if (tossResponse.getMethod() != null) {
                String method = tossResponse.getMethod().toUpperCase();
                if (method.contains("VIRTUAL") || method.contains("가상계좌")) {
                    methodType = PaymentMethodDetails.MethodType.VIRTUAL_ACCOUNT;
                } else if (method.contains("TRANSFER") || method.contains("계좌이체")) {
                    methodType = PaymentMethodDetails.MethodType.TRANSFER;
                } else if (method.contains("MOBILE") || method.contains("휴대폰")) {
                    methodType = PaymentMethodDetails.MethodType.MOBILE;
                } else if (method.contains("EASY") || method.contains("간편")) {
                    methodType = PaymentMethodDetails.MethodType.EASYPAY;
                } else if (method.contains("GIFT") || method.contains("상품권")) {
                    methodType = PaymentMethodDetails.MethodType.GIFT_CERTIFICATE;
                }
            }

            PaymentMethodDetails methodDetails = PaymentMethodDetails.builder()
                    .payment(payment)
                    .methodType(methodType)
                    .methodData(methodDetailsJson)
                    .build();
            paymentMethodDetailsRepository.save(methodDetails);
            log.info("PaymentMethodDetails 저장 완료 - paymentId: {}, methodType: {}", 
                    payment.getId(), methodType);
        } catch (Exception e) {
            log.error("PaymentMethodDetails 저장 중 오류 - paymentId: {}, error: {}", 
                    payment.getId(), e.getMessage(), e);
            // 예외 발생 시 재throw하여 트랜잭션 롤백 보장
            throw new IllegalStateException("PaymentMethodDetails 저장 실패: " + e.getMessage(), e);
        }
    }
}
