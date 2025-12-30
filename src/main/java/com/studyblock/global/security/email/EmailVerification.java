package com.studyblock.global.security.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/*
    이메일 인증 코드를 Redis에 저장 (TTL 5분)
 */
@Getter
@AllArgsConstructor
@RedisHash("email_verification")
public class EmailVerification {

    @Id
    private String email; // 이메일 주소 (키)

    private String code; // 6자리 인증 코드

    @TimeToLive
    private Long ttl = 300L; // 5분(300초) 후 자동 삭제

}
