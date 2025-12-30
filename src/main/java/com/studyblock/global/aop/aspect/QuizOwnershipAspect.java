package com.studyblock.global.aop.aspect;

import com.studyblock.domain.auth.enums.RoleCode;
import com.studyblock.domain.course.entity.Quiz;
import com.studyblock.domain.course.repository.QuizRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.aop.annotation.RequiresQuizOwnership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 퀴즈 소유권 검증 AOP
 *
 * @RequiresQuizOwnership 어노테이션이 적용된 메서드 실행 전에
 * 현재 사용자가 INSTRUCTOR 역할을 가지고 있고,
 * 해당 퀴즈가 속한 코스의 소유자인지 검증합니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class QuizOwnershipAspect {

    private final QuizRepository quizRepository;

    /**
     * @RequiresQuizOwnership이 적용된 메서드 실행 전 소유권 검증
     */
    @Before("@annotation(com.studyblock.global.aop.annotation.RequiresQuizOwnership)")
    public void checkQuizOwnership(JoinPoint joinPoint) {
        // 1. Authentication 객체 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 퀴즈 관리 기능 접근 시도");
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 2. Principal에서 User 객체 추출
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.warn("잘못된 인증 정보로 퀴즈 관리 기능 접근 시도 - principal type: {}",
                    principal != null ? principal.getClass().getName() : "null");
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
        }

        User user = (User) principal;

        // 3. INSTRUCTOR 역할 보유 여부 확인
        boolean hasInstructorRole = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getCode() == RoleCode.INSTRUCTOR);

        if (!hasInstructorRole) {
            log.warn("INSTRUCTOR 역할이 없는 사용자의 퀴즈 관리 접근 시도 - userId: {}", user.getId());
            throw new IllegalArgumentException("강사 권한이 필요합니다.");
        }

        // 4. 메서드 파라미터에서 quizId 추출
        Long quizId = extractQuizId(joinPoint);
        if (quizId == null) {
            log.error("메서드 파라미터에서 quizId를 찾을 수 없습니다 - method: {}",
                    joinPoint.getSignature().toShortString());
            throw new IllegalArgumentException("퀴즈 ID가 필요합니다.");
        }

        // 5. 퀴즈 조회 (Course와 Instructor를 함께 fetch하여 LazyInitializationException 방지)
        Quiz quiz = quizRepository.findByIdWithCourseAndInstructor(quizId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 퀴즈 접근 시도 - quizId: {}, userId: {}", quizId, user.getId());
                    return new IllegalArgumentException("퀴즈를 찾을 수 없습니다. ID=" + quizId);
                });

        // 6. 코스 소유권 확인
        Long courseOwnerId = quiz.getCourse().getInstructor().getId();
        Long instructorProfileId = user.getInstructorProfile() != null ?
                user.getInstructorProfile().getId() : null;

        if (instructorProfileId == null || !instructorProfileId.equals(courseOwnerId)) {
            // 어노테이션에서 커스텀 메시지 추출
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            RequiresQuizOwnership annotation = method.getAnnotation(RequiresQuizOwnership.class);
            String message = annotation != null ? annotation.message() : "해당 퀴즈에 대한 권한이 없습니다.";

            log.warn("권한 없는 퀴즈 접근 시도 - quizId: {}, courseOwnerId: {}, instructorProfileId: {}",
                    quizId, courseOwnerId, instructorProfileId);
            throw new IllegalArgumentException(message);
        }

        log.debug("퀴즈 소유권 검증 성공 - quizId: {}, instructorProfileId: {}, method: {}",
                quizId, instructorProfileId, joinPoint.getSignature().toShortString());
    }

    /**
     * 메서드 파라미터에서 quizId 추출
     */
    private Long extractQuizId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            // "quizId"라는 이름의 Long 타입 파라미터 찾기
            if (parameter.getName().equals("quizId") && parameter.getType() == Long.class) {
                return (Long) args[i];
            }
        }

        return null;
    }
}