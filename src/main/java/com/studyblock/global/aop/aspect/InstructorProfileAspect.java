package com.studyblock.global.aop.aspect;

import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.repository.InstructorProfileRepository;
import com.studyblock.global.aop.annotation.InstructorProfileCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 강사 프로필 생성 후처리 AOP
 *
 * @InstructorProfileCreated 어노테이션이 적용된 메서드가 실행된 후
 * InstructorProfile의 channelStatus를 자동으로 ACTIVE로 변경합니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class InstructorProfileAspect {

    private final InstructorProfileRepository instructorProfileRepository;

    /**
     * @InstructorProfileCreated가 적용된 메서드 실행 후 프로필 활성화
     */
    @AfterReturning(
            pointcut = "@annotation(com.studyblock.global.aop.annotation.InstructorProfileCreated)",
            returning = "result"
    )
    @Transactional
    public void activateInstructorProfile(JoinPoint joinPoint, Object result) {
        // 1. 반환값이 InstructorProfile인지 확인
        if (!(result instanceof InstructorProfile)) {
            log.warn("@InstructorProfileCreated가 적용된 메서드의 반환 타입이 InstructorProfile이 아닙니다. " +
                    "실제 타입: {}", result != null ? result.getClass().getName() : "null");
            return;
        }

        InstructorProfile profile = (InstructorProfile) result;

        // 2. 어노테이션에서 activateChannel 속성 확인
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        InstructorProfileCreated annotation = method.getAnnotation(InstructorProfileCreated.class);

        if (annotation == null || !annotation.activateChannel()) {
            log.debug("@InstructorProfileCreated.activateChannel=false이므로 자동 활성화를 건너뜁니다.");
            return;
        }

        // 3. 프로필이 새로 생성된 경우에만 활성화 (기존에 INACTIVE였던 경우)
        // 이미 ACTIVE인 경우 불필요한 업데이트 방지
        if (!profile.isChannelActive()) {
            profile.activateChannel();
            instructorProfileRepository.save(profile);

            log.info("강사 프로필 채널이 자동으로 활성화되었습니다. - profileId: {}, userId: {}, channelName: {}",
                    profile.getId(),
                    profile.getUser() != null ? profile.getUser().getId() : null,
                    profile.getChannelName());
        } else {
            log.debug("강사 프로필 채널이 이미 활성화되어 있습니다. - profileId: {}", profile.getId());
        }
    }
}