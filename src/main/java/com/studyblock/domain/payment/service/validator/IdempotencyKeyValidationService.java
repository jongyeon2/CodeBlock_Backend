package com.studyblock.domain.payment.service.validator;

import com.studyblock.domain.idempotency.service.IdempotencyKeyService;
import com.studyblock.domain.payment.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 멱등성 키 검증 전담 서비스
// 단일 책임: 중복 결제 방지를 위한 멱등성 키 검증
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyKeyValidationService {

    private final IdempotencyKeyService idempotencyKeyService;
    private final OrderRepository orderRepository;

    // 멱등성 키 검증 (중복 결제 방지)
    public void validateIdempotencyKey(Long userId, String idempotencyKey) {
        // 멱등성 키가 없으면 에러
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키가 필요합니다.");
        }

        // IdempotencyKeyService를 사용하여 검증
        try {
            idempotencyKeyService.validateIdempotencyKey(userId, idempotencyKey);
        } catch (IllegalStateException e) {
            // 이미 처리된 요청인 경우 예외 재던지기
            throw e;
        } catch (Exception e) {
            log.warn("멱등성 키 검증 중 오류 - idempotencyKey: {}, error: {}", idempotencyKey, e.getMessage());
            // 다른 오류는 기존 방식으로 폴백
            boolean exists = orderRepository.findByIdempotencyKey(idempotencyKey)
                    .isPresent();

            if (exists) {
                throw new IllegalStateException(
                    "이미 처리된 요청입니다. 중복 결제를 방지합니다."
                );
            }
        }
    }
}

