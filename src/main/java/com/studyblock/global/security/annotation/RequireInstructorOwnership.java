package com.studyblock.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 강의 소유자 권한 검증 어노테이션
 * 
 * - 메서드 파라미터에서 lectureId 또는 Lecture 엔티티를 추출하여 권한 검증
 * - 강사(Instructor) 권한 + 강의 소유자인지 확인
 * - AOP(Aspect)를 통해 자동 권한 검증 수행
 * 
 * 사용 예시:
 * <pre>
 * {@code
 * @RequireInstructorOwnership
 * public VideoUploadResponse uploadVideo(Long lectureId, ..., User currentUser) {
 *     // 이 메서드 실행 전에 자동으로 권한 검증
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireInstructorOwnership {
    
    /**
     * lectureId 파라미터명 (기본값: "lectureId")
     * 메서드 파라미터 중에서 lectureId를 찾을 때 사용
     */
    String lectureIdParam() default "lectureId";
    
    /**
     * 에러 메시지 (선택사항)
     */
    String message() default "해당 강의의 소유자만 접근할 수 있습니다.";
}

