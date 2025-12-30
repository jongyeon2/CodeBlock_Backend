package com.studyblock.global.aop.aspect;

import com.studyblock.domain.auth.enums.RoleCode;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.aop.annotation.RequiresInstructorRole;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 강사 역할 검증 AOP
 *
 * @RequiresInstructorRole 어노테이션이 적용된 메서드 실행 전에
 * 현재 사용자가 INSTRUCTOR 역할을 가지고 있는지 검증합니다.
 */
@Aspect
@Component
@Slf4j
public class InstructorRoleAspect {

    /**
     * @RequiresInstructorRole이 적용된 메서드 실행 전 역할 검증
     */
    @Before("@annotation(com.studyblock.global.aop.annotation.RequiresInstructorRole)")
    public void checkInstructorRole(JoinPoint joinPoint) {
        // 1. Authentication 객체 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 강사 전용 기능 접근 시도");
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 2. Principal에서 User 객체 추출
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.warn("잘못된 인증 정보로 강사 전용 기능 접근 시도 - principal type: {}",
                    principal != null ? principal.getClass().getName() : "null");
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
        }

        User user = (User) principal;

        // 3. INSTRUCTOR 역할 보유 여부 확인
        boolean hasInstructorRole = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getCode() == RoleCode.INSTRUCTOR);

        // 4. 역할이 없으면 예외 발생
        if (!hasInstructorRole) {
            // 어노테이션에서 커스텀 메시지 추출
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            RequiresInstructorRole annotation = method.getAnnotation(RequiresInstructorRole.class);
            String message = annotation != null ? annotation.message() : "강사 권한이 필요합니다.";

            log.warn("INSTRUCTOR 역할이 없는 사용자의 접근 시도 - userId: {}, memberId: {}",
                    user.getId(), user.getMemberId());
            throw new IllegalArgumentException(message);
        }

        log.debug("INSTRUCTOR 역할 검증 성공 - userId: {}, method: {}",
                user.getId(), joinPoint.getSignature().toShortString());
    }
}