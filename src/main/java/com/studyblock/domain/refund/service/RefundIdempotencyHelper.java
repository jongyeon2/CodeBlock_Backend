package com.studyblock.domain.refund.service;

import com.studyblock.domain.idempotency.service.IdempotencyKeyService;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.repository.RefundRepository;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 환불 멱등성 키 처리 헬퍼 클래스
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundIdempotencyHelper {

    private final IdempotencyKeyService idempotencyKeyService;
    private final RefundRepository refundRepository;

    // 멱등성 키로 중복 요청 확인
    public Refund checkDuplicateRequest(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        // 이미 처리된 멱등성 키인지 확인
        boolean alreadyProcessed = idempotencyKeyService.isAlreadyProcessed(userId, idempotencyKey);
        if (alreadyProcessed) {
            // 이미 처리된 요청이면 기존 환불 반환
            Refund existingRefund = refundRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                    "이미 처리된 환불 요청입니다. 중복 환불을 방지합니다."
                ));
            log.info("중복 환불 요청 차단 - idempotencyKey: {}, refundId: {}",
                    idempotencyKey, existingRefund.getId());
            return existingRefund;
        }

        // 멱등성 키로 이미 환불이 생성되어 있는지 확인
        refundRepository.findByIdempotencyKey(idempotencyKey)
            .ifPresent(existingRefund -> {
                throw new IllegalStateException(
                    "이미 처리된 환불 요청입니다. 중복 환불을 방지합니다."
                );
            });

        return null;
    }

    // 멱등성 키 생성
    public void createIdempotencyKey(User user, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            idempotencyKeyService.createIdempotencyKey(user, idempotencyKey, null);
        } catch (Exception e) {
            log.warn("멱등성 키 생성 실패 - idempotencyKey: {}, error: {}", 
                    idempotencyKey, e.getMessage());
            // 멱등성 키 생성 실패는 환불 처리에 영향을 주지 않음 (로그만 남김)
        }
    }

    // 멱등성 키 완료 처리
    public void markAsUsed(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            idempotencyKeyService.markAsUsed(idempotencyKey, null);
        } catch (Exception e) {
            log.error("멱등성 키 완료 처리 실패 - idempotencyKey: {}, error: {}", 
                    idempotencyKey, e.getMessage(), e);
            // 멱등성 키 처리 실패는 환불 성공 응답에 영향을 주지 않음
        }
    }

    // 멱등성 키 실패 처리
    public void markAsFailed(String idempotencyKey, String reason) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            idempotencyKeyService.markAsFailed(idempotencyKey, reason);
        } catch (Exception e) {
            log.error("멱등성 키 실패 처리 중 오류 - idempotencyKey: {}, error: {}", 
                    idempotencyKey, e.getMessage(), e);
            // 멱등성 키 처리 실패는 무시 (환불 실패가 더 중요)
        }
    }
}

