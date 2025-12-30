package com.studyblock.domain.payment.client;

import com.studyblock.domain.payment.dto.TossPaymentResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

// 토스페이먼츠 API 호출 클라이언트
@Slf4j
@Component
public class TossPaymentClient {
    
    private final RestTemplate restTemplate;
    private final String tossApiUrl;
    private final String tossSecretKey;
    private final ObjectMapper objectMapper;
    
    public TossPaymentClient(
            RestTemplate restTemplate,
            @Value("${toss.payments.api-url}") String tossApiUrl,
            @Value("${toss.payments.secret-key}") String tossSecretKey,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.tossApiUrl = tossApiUrl;
        this.tossSecretKey = tossSecretKey;
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    // 결제 승인 API 호출
    public TossPaymentResponse confirm(String paymentKey, String orderId, Integer amount) {
        // 1. 요청 URL
        String url = tossApiUrl + "/v1/payments/confirm";
        
        // 2. 요청 헤더 생성 (Basic 인증)
        HttpHeaders headers = createHeaders();
        
        // 3. 요청 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentKey", paymentKey);
        requestBody.put("orderId", orderId);
        requestBody.put("amount", amount);
        
        log.info("토스페이먼츠 결제 승인 요청 - URL: {}, paymentKey: {}, orderId: {}, amount: {}", 
                url, paymentKey, orderId, amount);
        
        // 4. HTTP 요청 생성
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // 5. API 호출
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            String body = response.getBody();
            log.info("토스페이먼츠 응답 수신 - URL: {}, Status: {}, Body: {}", 
                    url, response.getStatusCode(), body);
            
            try {
                TossPaymentResponse parsedResponse = objectMapper.readValue(body, TossPaymentResponse.class);
                log.info("토스페이먼츠 응답 파싱 성공 - paymentKey: {}, status: {}, method: {}", 
                        parsedResponse.getPaymentKey(), parsedResponse.getStatus(), parsedResponse.getMethod());
                return parsedResponse;
            } catch (Exception parseEx) {
                log.error("토스페이먼츠 응답 파싱 실패 - Body: {}, Error: {}", body, parseEx.getMessage(), parseEx);
                throw new IllegalStateException("토스 응답 파싱 실패: " + parseEx.getMessage());
            }
            
        } catch (HttpClientErrorException e) {
            // 토스 API 에러 처리 - 상세 오류 메시지 포함
            String errorMessage = extractErrorMessage(e);
            log.error("토스페이먼츠 API 오류 - 상태: {}, 응답: {}", e.getStatusCode(), errorMessage);
            throw new IllegalStateException(
                "토스페이먼츠 승인 실패: " + errorMessage
            );
        }
    }
    
    // 에러 메시지 추출
    private String extractErrorMessage(HttpClientErrorException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            log.debug("토스페이먼츠 오류 응답: {}", responseBody);
            
            // JSON 파싱해서 code와 message 추출
            if (responseBody != null && !responseBody.isEmpty()) {
                // 간단한 파싱 (실제로는 ObjectMapper 사용 권장)
                if (responseBody.contains("\"code\"")) {
                    return responseBody; // 전체 응답 반환 (디버깅용)
                }
            }
            return "HTTP " + e.getStatusCode().value() + ": " + e.getStatusText();
        } catch (Exception ex) {
            return "HTTP " + e.getStatusCode().value() + ": " + e.getStatusText();
        }
    }
    
    // 에러 코드만 추출 (민감 정보 제외)
    private String extractErrorCode(HttpClientErrorException e) {
        try {
            // 실제로는 JSON 파싱해서 code만 추출해야 함
            // 간단한 구현: HTTP 상태 코드만 반환
            return String.valueOf(e.getStatusCode().value());
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }
    
    // 환불 API 호출 (원본 JSON 응답도 함께 반환)
    public static class RefundResult {
        private final TossPaymentResponse response;
        private final String rawJson;
        
        public RefundResult(TossPaymentResponse response, String rawJson) {
            this.response = response;
            this.rawJson = rawJson;
        }
        
        public TossPaymentResponse getResponse() {
            return response;
        }
        
        public String getRawJson() {
            return rawJson;
        }
    }
    
    // 환불 API 호출
    public TossPaymentResponse refund(String paymentKey, Integer cancelAmount, String cancelReason) {
        return refundWithRawJson(paymentKey, cancelAmount, cancelReason).getResponse();
    }
    
    // 환불 API 호출 (원본 JSON 포함)
    public RefundResult refundWithRawJson(String paymentKey, Integer cancelAmount, String cancelReason) {
        // 1. 요청 URL
        String url = tossApiUrl + "/v1/payments/" + paymentKey + "/cancel";
        
        // 2. 요청 헤더 생성
        HttpHeaders headers = createHeaders();
        
        // 3. 요청 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cancelAmount", cancelAmount);
        requestBody.put("cancelReason", cancelReason);
        
        // 4. HTTP 요청 생성
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // 5. API 호출 (원본 JSON 먼저 받기)
        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            String rawJson = rawResponse.getBody();
            log.info("토스페이먼츠 환불 응답 수신 - URL: {}, Status: {}, Body: {}", 
                    url, rawResponse.getStatusCode(), rawJson);
            
            // JSON 파싱
            TossPaymentResponse parsedResponse = objectMapper.readValue(rawJson, TossPaymentResponse.class);
            log.info("토스페이먼츠 환불 응답 파싱 성공 - paymentKey: {}, status: {}", 
                    parsedResponse.getPaymentKey(), parsedResponse.getStatus());
            
            return new RefundResult(parsedResponse, rawJson);
            
        } catch (HttpClientErrorException e) {
            String errorCode = extractErrorCode(e);
            throw new IllegalStateException(
                "토스페이먼츠 환불 실패 (코드: " + errorCode + ")"
            );
        } catch (Exception e) {
            log.error("토스페이먼츠 환불 응답 파싱 실패 - Error: {}", e.getMessage(), e);
            throw new IllegalStateException("토스 환불 응답 파싱 실패: " + e.getMessage());
        }
    }

    // Basic 인증 헤더 생성
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Secret Key를 Base64로 인코딩 (토스 요구사항: "secretKey:" 형식)
        String auth = tossSecretKey + ":";
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }
}

