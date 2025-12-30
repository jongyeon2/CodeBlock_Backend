package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.entity.PaymentWebhookEvent;

import com.studyblock.domain.payment.repository.PaymentRepository;
import com.studyblock.domain.payment.repository.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;

    private final PaymentRepository paymentRepository;

    @Transactional
    public void handle(String eventType, String payloadJson, String paymentKey, LocalDateTime createdAt) {
        // 웹훅이벤트 저장
        PaymentWebhookEvent saved = paymentWebhookEventRepository.save(
                PaymentWebhookEvent.builder()
                        .eventType(eventType)
                        .webhookPayload(payloadJson)
                        .receivedAt(createdAt != null ? createdAt : LocalDateTime.now())
                        .build()
        );

        // paymentKey로 결제 조회 후 상태 동기화(간단 버전)
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElse(null);
        if (payment == null) {
            log.warn("웹훅 처리: paymentKey={} 해당 결제를 찾지 못했습니다", paymentKey);
            return;
        }

        switch (eventType) {
            case "PAYMENT_STATUS_CHANGED":
            case "PAYMENT_APPROVED":
                payment.capture();
                break;
            case "PAYMENT_CANCELLED":
            case "CANCEL_STATUS_CHANGED":
                payment.cancel("Webhook cancel");
                break;
            case "PAYMENT_PARTIAL_CANCELLED":
                // 부분 취소 시: Payment 내부 환불가능금액/상태를 동기화하도록 보강 필요(여기서는 로그로 남김)
                log.info("부분 취소 웹훅 수신 - paymentKey={}", paymentKey);
                break;
            default:
                log.info("웹훅 처리: 미지원 이벤트={}, paymentKey={}", eventType, paymentKey);
        }

        paymentRepository.save(payment);
        log.info("웹훅 처리 완료: eventId={}, eventType={}, paymentId={}", saved.getId(), eventType, payment.getId());
    }
}


