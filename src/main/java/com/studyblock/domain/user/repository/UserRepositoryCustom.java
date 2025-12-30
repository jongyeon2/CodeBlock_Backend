package com.studyblock.domain.user.repository;

import com.studyblock.domain.user.entity.User;

import java.util.Optional;

/**
 * User 커스텀 Repository 인터페이스
 * - QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 */
public interface UserRepositoryCustom {

    /**
     * memberId로 User 조회 (UserRole, Role Fetch Join)
     * - N+1 문제 방지를 위해 Fetch Join 사용
     * - 로그인 시 역할 정보를 함께 조회
     *
     * @param memberId 회원 아이디
     * @return User (UserRole, Role 포함)
     */
    Optional<User> findByMemberIdWithRoles(String memberId);

    /**
     * userId로 User 조회 (UserRole, Role Fetch Join)
     * - N+1 문제 방지를 위해 Fetch Join 사용
     * - 현재 사용자 정보 조회 시 역할 정보를 함께 조회
     *
     * @param id 회원 ID
     * @return User (UserRole, Role 포함)
     */
    Optional<User> findByIdWithRoles(Long id);
}