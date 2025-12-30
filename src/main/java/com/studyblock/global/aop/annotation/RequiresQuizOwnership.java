package com.studyblock.global.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 퀴즈 소유권 검증 어노테이션
 *
 * 이 어노테이션이 적용된 메서드는 실행 전에:
 * 1. 사용자가 INSTRUCTOR 역할을 가지고 있는지 검증
 * 2. 해당 퀴즈가 속한 코스의 소유자인지 검증
 *
 * 메서드 파라미터에 "quizId"라는 이름의 Long 타입 파라미터가 있어야 합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @RequiresQuizOwnership
 * public void updateQuiz(Long quizId, QuizUpdateRequest request) {
 *     // 퀴즈 수정 로직
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresQuizOwnership {
    /**
     * 에러 메시지 커스터마이징 (선택 사항)
     */
    String message() default "해당 퀴즈에 대한 권한이 없습니다.";
}