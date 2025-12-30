package com.studyblock.domain.auth.repository;

import com.studyblock.domain.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * UserRole Repository
 * - 사용자-역할 관계 관리
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
}