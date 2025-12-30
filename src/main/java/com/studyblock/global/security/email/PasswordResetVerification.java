package com.studyblock.global.security.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * 비밀번호 찾기 인증 코드를 Redis에 저장 (TTL 5분)
 * - 회원가입용 EmailVerification과 구분하기 위해 별도 엔티티 생성
 * - Key: "password_reset:{email}"
 * - Value: 6자리 인증 코드
 */
@Getter
@AllArgsConstructor
@RedisHash("password_reset_verification")
public class PasswordResetVerification {

    @Id
    private String email; // 이메일 주소 (키)

    private String code; // 6자리 인증 코드

    @TimeToLive
    private Long ttl = 300L; // 5분(300초) 후 자동 삭제
}

