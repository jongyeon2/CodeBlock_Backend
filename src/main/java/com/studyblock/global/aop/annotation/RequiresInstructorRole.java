package com.studyblock.global.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 강사 역할(INSTRUCTOR) 검증 어노테이션
 *
 * 이 어노테이션이 적용된 메서드는 실행 전에 사용자가 INSTRUCTOR 역할을 가지고 있는지 검증합니다.
 * INSTRUCTOR 역할이 없는 경우 IllegalArgumentException이 발생합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @RequiresInstructorRole
 * public void createLecture() {
 *     // 강사 전용 기능
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresInstructorRole {
    /**
     * 에러 메시지 커스터마이징 (선택 사항)
     */
    String message() default "강사 권한이 필요합니다.";
}