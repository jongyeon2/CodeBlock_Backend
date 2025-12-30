package com.studyblock.global.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 강사 프로필 생성 후처리 어노테이션
 *
 * 이 어노테이션이 적용된 메서드가 InstructorProfile을 반환하면
 * 자동으로 다음과 같은 후처리를 수행합니다:
 * 1. channelStatus를 ACTIVE로 변경
 * 2. 로그 기록
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @InstructorProfileCreated
 * public InstructorProfile createProfile(Long userId, InstructorProfileRequest req) {
 *     // 프로필 생성 로직
 *     return instructorProfile;
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstructorProfileCreated {
    /**
     * 프로필 생성 시 channelStatus를 자동으로 ACTIVE로 변경할지 여부
     * 기본값: true
     */
    boolean activateChannel() default true;
}