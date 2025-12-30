package com.studyblock.global.aop.aspect;

import com.studyblock.global.aop.annotation.RateLimited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 재전송 제한 AOP Aspect
 *
 * @RateLimited 어노테이션이 적용된 메서드 실행 전에
 * Redis를 사용하여 재전송 제한을 검증합니다.
 * 설정된 시간 내에 동일한 키로 요청이 들어오면 예외를 발생시킵니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * @RateLimited가 적용된 메서드 실행 전 재전송 제한 검증
     */
    @Around("@annotation(com.studyblock.global.aop.annotation.RateLimited)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 어노테이션 정보 추출
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimited annotation = method.getAnnotation(RateLimited.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 2. Redis 키 생성 (파라미터에서 키 값 추출)
        String keyValue = extractKeyValue(joinPoint, annotation.key());
        String redisKey = annotation.prefix() + keyValue;

        // 3. Redis에서 기존 요청 확인
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            log.warn("재전송 제한 - Redis 키 존재: {}, duration: {}초", redisKey, annotation.duration());
            throw new IllegalArgumentException(annotation.message());
        }

        // 4. Redis에 요청 기록 저장 (TTL 설정)
        redisTemplate.opsForValue().set(
                redisKey,
                "locked",
                annotation.duration(),
                TimeUnit.SECONDS
        );

        log.debug("재전송 제한 키 설정 - key: {}, duration: {}초", redisKey, annotation.duration());

        try {
            // 5. 실제 메서드 실행
            return joinPoint.proceed();
        } catch (Exception e) {
            // 6. 예외 발생 시 Redis 키 삭제 (재시도 가능하도록)
            redisTemplate.delete(redisKey);
            log.debug("예외 발생으로 재전송 제한 키 삭제 - key: {}", redisKey);
            throw e;
        }
    }

    /**
     * 메서드 파라미터에서 키 값 추출
     * - annotation.key()가 "email"이면 첫 번째 String 파라미터를 키로 사용
     * - annotation.key()가 특정 파라미터 이름이면 해당 파라미터의 값을 키로 사용
     */
    private String extractKeyValue(ProceedingJoinPoint joinPoint, String keyName) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        // 1. 파라미터 이름으로 키 찾기
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(keyName) && args[i] instanceof String) {
                return (String) args[i];
            }
        }

        // 2. 기본값: "email"이면 첫 번째 String 파라미터 사용
        if ("email".equals(keyName)) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    return (String) arg;
                }
            }
        }

        // 3. 키를 찾지 못한 경우 파라미터 조합으로 생성
        log.warn("키 값을 찾을 수 없어 파라미터 조합으로 생성 - keyName: {}", keyName);
        return String.join(":", java.util.Arrays.toString(args));
    }
}

