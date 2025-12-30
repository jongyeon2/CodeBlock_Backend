package com.studyblock.global.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 재전송 제한 어노테이션
 *
 * 이 어노테이션이 적용된 메서드는 일정 시간 동안 재전송을 제한합니다.
 * Redis를 사용하여 전송 이력을 캐싱하고, 설정된 시간 내에는 동일한 요청을 차단합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @RateLimited(key = "email", duration = 60)  // 1분(60초) 동안 재전송 제한
 * public void sendEmail(String email) {
 *     // 이메일 전송 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /**
     * Redis 키 생성에 사용할 파라미터 이름
     * 기본값: "email" (첫 번째 String 파라미터를 키로 사용)
     */
    String key() default "email";

    /**
     * 제한 시간 (초 단위)
     * 기본값: 60초 (1분)
     */
    long duration() default 60;

    /**
     * Redis 키 접두사
     * 기본값: "rate_limit:"
     */
    String prefix() default "rate_limit:";

    /**
     * 에러 메시지 (재전송 제한 시)
     */
    String message() default "잠시 후 다시 시도해주세요.";
}

