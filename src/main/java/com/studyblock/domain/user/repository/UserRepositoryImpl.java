package com.studyblock.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.studyblock.domain.auth.entity.QRole.role;
import static com.studyblock.domain.auth.entity.QUserRole.userRole;
import static com.studyblock.domain.user.entity.QUser.user;

/**
 * UserRepositoryCustom 구현체
 * - QueryDSL을 사용한 복잡한 쿼리 구현
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * memberId로 User 조회 (UserRole, Role Fetch Join)
     * - 단 1번의 쿼리로 User + UserRole + Role 모두 조회
     * - N+1 문제 완전 해결
     *
     * 실행되는 SQL:
     * SELECT u.*, ur.*, r.*
     * FROM user u
     * LEFT JOIN user_role ur ON u.id = ur.user_id
     * LEFT JOIN role r ON ur.role_id = r.id
     * WHERE u.member_id = ?
     */
    @Override
    public Optional<User> findByMemberIdWithRoles(String memberId) {
        User result = queryFactory
                .selectFrom(user)
                .leftJoin(user.userRoles, userRole).fetchJoin()  // UserRole Fetch Join
                .leftJoin(userRole.role, role).fetchJoin()       // Role Fetch Join
                .where(user.memberId.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * userId로 User 조회 (UserRole, Role Fetch Join)
     * - 단 1번의 쿼리로 User + UserRole + Role 모두 조회
     * - N+1 문제 완전 해결
     *
     * 실행되는 SQL:
     * SELECT u.*, ur.*, r.*
     * FROM user u
     * LEFT JOIN user_role ur ON u.id = ur.user_id
     * LEFT JOIN role r ON ur.role_id = r.id
     * WHERE u.id = ?
     */
    @Override
    public Optional<User> findByIdWithRoles(Long id) {
        User result = queryFactory
                .selectFrom(user)
                .leftJoin(user.userRoles, userRole).fetchJoin()  // UserRole Fetch Join
                .leftJoin(userRole.role, role).fetchJoin()       // Role Fetch Join
                .where(user.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}