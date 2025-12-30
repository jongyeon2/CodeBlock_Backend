package com.studyblock.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *
 * <p>JPA Auditing이란?
 * - Entity의 생성 시간(createdAt), 수정 시간(updatedAt) 등을 자동으로 관리해주는 기능
 * - @CreatedDate, @LastModifiedDate 어노테이션이 붙은 필드를 자동으로 채워줌
 *
 * <p>왜 별도의 Config 클래스로 분리했나?
 * 1. 관심사 분리: Application 클래스는 애플리케이션 실행만 담당
 * 2. 확장성: 추가 설정(예: AuditorAware 빈) 필요 시 이 클래스에서 관리
 * 3. 명확성: JPA 관련 설정을 한 곳에서 관리
 * 4. 테스트: 필요 시 JPA Auditing을 선택적으로 비활성화 가능
 *
 * <p>어떻게 작동하는가?
 * - BaseTimeEntity를 상속받는 모든 Entity(User, UserProfile 등)에 적용됨
 * - Entity 저장 시: createdAt, updatedAt 자동 설정
 * - Entity 수정 시: updatedAt만 자동 업데이트
 *
 * <p>관련 클래스:
 * - {@link com.studyblock.domain.common.BaseTimeEntity} - 시간 필드 정의
 * - {@link com.studyblock.domain.user.entity.User} - BaseTimeEntity 상속
 *
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 * @see com.studyblock.domain.common.BaseTimeEntity
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * 추후 "누가" 생성/수정했는지 추적하려면 아래와 같은 빈을 추가:
     *
     * @Bean
     * public AuditorAware<Long> auditorProvider() {
     *     return () -> {
     *         // Spring Security의 현재 로그인한 사용자 ID 반환
     *         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
     *         if (authentication == null || !authentication.isAuthenticated()) {
     *             return Optional.empty();
     *         }
     *         PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
     *         return Optional.of(principal.getUser().getId());
     *     };
     * }
     *
     * 그리고 BaseTimeEntity에 필드 추가:
     * @CreatedBy
     * @Column(name = "created_by")
     * private Long createdBy;
     *
     * @LastModifiedBy
     * @Column(name = "updated_by")
     * private Long updatedBy;
     */
}