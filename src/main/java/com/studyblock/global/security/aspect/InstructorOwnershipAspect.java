package com.studyblock.global.security.aspect;

import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.security.annotation.RequireInstructorOwnership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * 강의 소유자 권한 검증 AOP Aspect
 *
 * - @RequireInstructorOwnership 어노테이션이 붙은 메서드 실행 전에 권한 검증
 * - 메서드 파라미터에서 lectureId 추출하여 강의 소유자 확인
 * - 권한이 없으면 IllegalArgumentException 발생
 *
 * AOP 장점:
 * - 비즈니스 로직과 권한 검증 로직 분리 (관심사 분리)
 * - 중복 코드 제거 (VideoService, PreviewVideoService에서 반복되는 권한 검증 로직)
 * - 선언적 보안 (어노테이션만으로 권한 검증 적용)
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class InstructorOwnershipAspect {

    private final LectureRepository lectureRepository;

    /**
     * @RequireInstructorOwnership 어노테이션이 붙은 메서드 실행 전에 권한 검증
     *
     * @param joinPoint AOP 조인 포인트 (메서드 실행 지점)
     * @param annotation RequireInstructorOwnership 어노테이션
     */
    @Before("@annotation(annotation)")
    public void checkInstructorOwnership(JoinPoint joinPoint, RequireInstructorOwnership annotation) {
        log.debug("강의 소유자 권한 검증 시작 - Method: {}", joinPoint.getSignature().getName());

        // 1. 현재 사용자 추출
        User currentUser = getCurrentUser();

        // 2. 메서드 파라미터에서 lectureId 추출
        Long lectureId = extractLectureId(joinPoint, annotation.lectureIdParam());

        // 3. 권한 검증
        verifyLectureOwnership(lectureId, currentUser, annotation.message());

        log.debug("강의 소유자 권한 검증 완료 - Lecture ID: {}, User ID: {}", lectureId, currentUser.getId());
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 추출
     *
     * @return 현재 로그인한 사용자
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User)) {
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
        }

        return (User) principal;
    }

    /**
     * 메서드 파라미터에서 lectureId 추출
     *
     * @param joinPoint AOP 조인 포인트
     * @param paramName 파라미터명 (기본값: "lectureId")
     * @return 추출된 lectureId
     */
    private Long extractLectureId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        throw new IllegalStateException(
                "메서드 파라미터에서 '" + paramName + "'을(를) 찾을 수 없습니다. " +
                        "Method: " + signature.getName());
    }

    /**
     * 강의 소유자 권한 검증
     *
     * @param lectureId 강의 ID
     * @param currentUser 현재 로그인한 사용자
     * @param errorMessage 에러 메시지
     */
    private void verifyLectureOwnership(Long lectureId, User currentUser, String errorMessage) {
        // 강의 조회
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID: " + lectureId));

        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 접근할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 강의의 강사와 현재 사용자의 강사 프로필이 일치하지 않으면 권한 없음
        if (!lecture.getInstructor().getId().equals(instructorProfile.getId())) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}

