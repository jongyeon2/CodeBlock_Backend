package com.studyblock.domain.auth.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

/**
 * RefreshToken Redis 저장용 엔티티
 * - Key: "refresh_token:{userId}"
 * - TTL: 7일 (자동 만료)
 */
@RedisHash(value = "refresh_token", timeToLive = 604800) // 7일 (초 단위)
@Getter
public class RefreshToken {

    @Id
    private String id; // Redis의 Key가 됨 (예: "refresh_token:1")

    @Indexed // userId로 검색 가능하도록 인덱스 생성
    private Long userId;

    private String token;

    @TimeToLive // TTL 동적 설정 가능 (선택사항)
    private Long expiration; // 초 단위

    // 기본 생성자 (Redis 역직렬화용)
    protected RefreshToken() {
    }

    @Builder
    public RefreshToken(Long userId, String token, Long expiration) {
        this.id = String.valueOf(userId); // userId를 Key로 사용
        this.userId = userId;
        this.token = token;
        this.expiration = expiration != null ? expiration : 604800L; // 기본 7일
    }

    /**
     * RefreshToken 갱신
     */
    public void updateToken(String newToken) {
        this.token = newToken;
    }
}