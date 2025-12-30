package com.studyblock.domain.idempotency.service;

import com.studyblock.domain.idempotency.entity.IdempotencyKey;
import com.studyblock.domain.idempotency.repository.IdempotencyKeyRepository;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 멱등성 키 관리 서비스
 * 중복 요청 방지 및 응답 캐싱을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyKeyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    
    // 멱등성 키 기본 만료 시간: 24시간
    private static final int DEFAULT_EXPIRATION_HOURS = 24;

    /**
     * 멱등성 키 생성 및 저장
     * @param user 사용자
     * @param idempotencyKey 멱등성 키 (UUID 등)
     * @param requestHash 요청 내용의 해시값 (선택적)
     * @return 생성된 IdempotencyKey 엔티티
     */
    @Transactional
    public IdempotencyKey createIdempotencyKey(User user, 
                                               String idempotencyKey, 
                                               String requestHash) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키가 필요합니다.");
        }

        // 이미 존재하는지 확인
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            
            // 같은 사용자의 키인지 확인
            if (!key.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException(
                    "다른 사용자의 멱등성 키입니다. 중복 요청을 방지합니다."
                );
            }
            
            // 이미 완료된 키인지 확인
            if ("COMPLETED".equals(key.getStatus())) {
                throw new IllegalStateException(
                    "이미 처리된 요청입니다. 중복 요청을 방지합니다."
                );
            }
            
            // 만료된 키인지 확인
            if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException(
                    "만료된 멱등성 키입니다. 새로운 키를 생성해주세요."
                );
            }
            
            // 이미 존재하면 기존 키 반환 (PENDING 상태인 경우)
            log.info("기존 멱등성 키 발견 - key: {}, status: {}", idempotencyKey, key.getStatus());
            
            // requestHash가 다르면 업데이트
            if (requestHash != null && !requestHash.equals(key.getRequestHash())) {
                key.setRequestHash(requestHash);
                idempotencyKeyRepository.save(key);
            }
            
            return key;
        }

        // 새로운 키 생성
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(DEFAULT_EXPIRATION_HOURS);
        
        IdempotencyKey newKey = IdempotencyKey.builder()
                .user(user)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status("PENDING")
                .expiresAt(expiresAt)
                .build();

        IdempotencyKey saved = idempotencyKeyRepository.save(newKey);
        log.info("멱등성 키 생성 완료 - key: {}, userId: {}", idempotencyKey, user.getId());
        
        return saved;
    }

    /**
     * 멱등성 키 검증 (중복 요청 확인)
     * @param userId 사용자 ID
     * @param idempotencyKey 멱등성 키
     * @return 이미 처리된 키인 경우 true, 그렇지 않으면 false
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        Optional<IdempotencyKey> optional = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (optional.isEmpty()) {
            return false;
        }

        IdempotencyKey key = optional.get();
        
        // 다른 사용자의 키인지 확인
        if (!key.getUser().getId().equals(userId)) {
            throw new IllegalStateException(
                "다른 사용자의 멱등성 키입니다."
            );
        }

        // 만료된 키인지 확인
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("만료된 멱등성 키 - key: {}", idempotencyKey);
            return false;
        }

        // 이미 완료된 키인지 확인
        return "COMPLETED".equals(key.getStatus());
    }

    /**
     * 멱등성 키 검증 (중복 요청이면 예외 발생)
     * @param userId 사용자 ID
     * @param idempotencyKey 멱등성 키
     * @throws IllegalStateException 이미 처리된 요청인 경우
     */
    @Transactional(readOnly = true)
    public void validateIdempotencyKey(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키가 필요합니다.");
        }

        Optional<IdempotencyKey> optional = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (optional.isEmpty()) {
            return; // 새로운 키이므로 통과
        }

        IdempotencyKey key = optional.get();
        
        // 다른 사용자의 키인지 확인
        if (!key.getUser().getId().equals(userId)) {
            throw new IllegalStateException(
                "다른 사용자의 멱등성 키입니다. 중복 요청을 방지합니다."
            );
        }

        // 만료된 키인지 확인
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("만료된 멱등성 키 - key: {}", idempotencyKey);
            return; // 만료된 키는 새 요청으로 간주
        }

        // 이미 완료된 키인지 확인
        if ("COMPLETED".equals(key.getStatus())) {
            throw new IllegalStateException(
                "이미 처리된 요청입니다. 중복 요청을 방지합니다."
            );
        }
    }

    /**
     * 멱등성 키를 사용 완료 상태로 변경 및 응답 캐싱
     * @param idempotencyKey 멱등성 키
     * @param responseSnapshot 응답 내용 (JSON 형태)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsUsed(String idempotencyKey, String responseSnapshot) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        Optional<IdempotencyKey> optional = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (optional.isEmpty()) {
            log.warn("멱등성 키를 찾을 수 없습니다 - key: {}", idempotencyKey);
            return;
        }

        IdempotencyKey key = optional.get();
        key.markAsUsed(responseSnapshot);
        idempotencyKeyRepository.save(key);
        log.info("멱등성 키 사용 완료 처리 - key: {}, responseSnapshot: {}", 
                idempotencyKey, responseSnapshot != null ? "있음" : "없음");
    }

    /**
     * 멱등성 키로 캐시된 응답 조회
     * @param idempotencyKey 멱등성 키
     * @return 캐시된 응답 (JSON 형태), 없으면 null
     */
    @Transactional(readOnly = true)
    public String getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        Optional<IdempotencyKey> optional = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (optional.isEmpty()) {
            return null;
        }

        IdempotencyKey key = optional.get();
        
        // 만료된 키인지 확인
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        // 완료된 키이고 응답이 캐시되어 있는 경우
        if ("COMPLETED".equals(key.getStatus()) && key.getResponseSnapshot() != null) {
            return key.getResponseSnapshot();
        }

        return null;
    }

    /**
     * 멱등성 키를 실패 상태로 변경 및 에러 캐싱
     * @param idempotencyKey 멱등성 키
     * @param errorSnapshot 에러 내용 (JSON 형태)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(String idempotencyKey, String errorSnapshot) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        Optional<IdempotencyKey> optional = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (optional.isEmpty()) {
            log.warn("멱등성 키를 찾을 수 없습니다 - key: {}", idempotencyKey);
            return;
        }

        IdempotencyKey key = optional.get();
        key.markAsFailed(errorSnapshot);
        idempotencyKeyRepository.save(key);
        log.info("멱등성 키 실패 처리 - key: {}, errorSnapshot: {}", 
                idempotencyKey, errorSnapshot != null ? "있음" : "없음");
    }
}

