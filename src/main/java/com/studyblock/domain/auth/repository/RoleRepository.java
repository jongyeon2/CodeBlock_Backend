package com.studyblock.domain.auth.repository;

import com.studyblock.domain.auth.entity.Role;
import com.studyblock.domain.auth.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Role Repository
 * - 역할 정보 조회
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * RoleCode로 역할 조회
     * @param code 역할 코드 (ADMIN, USER, INSTRUCTOR)
     * @return Role
     */
    Optional<Role> findByCode(RoleCode code);
}