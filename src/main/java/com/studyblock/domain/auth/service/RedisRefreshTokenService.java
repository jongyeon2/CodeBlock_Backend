package com.studyblock.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 RefreshToken 관리 서비스
 * - Key: "refresh:userId:{userId}"
 * - Value: JWT RefreshToken
 * - TTL: 7일 (자동 만료)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "refresh:userId:";
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 7;

    /**
     * RefreshToken 저장 (TTL 7일)
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        String key = KEY_PREFIX + userId;

        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                REFRESH_TOKEN_EXPIRE_DAYS,
                TimeUnit.DAYS
        );

        log.info("Redis에 RefreshToken 저장 - userId: {}, TTL: {}일", userId, REFRESH_TOKEN_EXPIRE_DAYS);
    }

    /**
     * userId로 RefreshToken 조회
     */
    public Optional<String> getRefreshToken(Long userId) {
        String key = KEY_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);

        return Optional.ofNullable(token)
                .map(Object::toString);
    }

    /**
     * RefreshToken으로 userId 조회 (전체 스캔 필요 - 비효율적)
     * 대신 JWT에서 userId를 직접 추출하는 방식 권장
     */
    @Deprecated
    public Optional<Long> getUserIdByToken(String refreshToken) {
        // Redis는 Value로 Key를 조회하는 기능이 없음
        // JWT 자체에서 userId를 추출하는 방식을 사용하세요
        throw new UnsupportedOperationException("JWT에서 직접 userId를 추출하세요");
    }

    /**
     * RefreshToken 삭제 (로그아웃)
     */
    public void deleteRefreshToken(Long userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);

        log.info("Redis에서 RefreshToken 삭제 - userId: {}", userId);
    }

    /**
     * RefreshToken 존재 여부 확인
     */
    public boolean existsRefreshToken(Long userId) {
        String key = KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * RefreshToken 남은 만료 시간 조회 (초 단위)
     */
    public Long getExpireTime(Long userId) {
        String key = KEY_PREFIX + userId;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}