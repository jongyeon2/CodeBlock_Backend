package com.studyblock.domain.payment.controller;

import com.studyblock.domain.payment.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks/toss")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestHeader Map<String, String> headers,
                                    @RequestBody String payload) {
        // 간략 파싱(실무에선 서명 검증 및 정확한 JSON 파싱 필요)
        String eventType = headers.getOrDefault("eventType", "PAYMENT_STATUS_CHANGED");
        String paymentKey = null; // payload에서 추출하도록 확장 필요
        webhookService.handle(eventType, payload, paymentKey, LocalDateTime.now());
        return ResponseEntity.ok().build();
    }
}


