package com.studyblock.domain.user.repository;

import com.studyblock.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 * - JpaRepository: 기본 CRUD 메서드 제공
 * - UserRepositoryCustom: QueryDSL을 사용한 커스텀 쿼리 메서드 제공
 */
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    /**
     * 이메일로 사용자 조회
     * - 이메일 통합 정책의 핵심: 같은 이메일이면 하나의 계정으로 처리
     * - OAuth2 로그인 시 이 메서드로 먼저 조회 → 있으면 기존 계정 사용
     */
    Optional<User> findByEmail(String email);

    /**
     * OAuth2 제공자 정보로 사용자 조회 (기존 방식)
     * - 가입 경로(jointype) + OAuth ID로 조회
     * - 이메일 통합 정책에서는 보조적으로 사용 (fallback)
     */
    Optional<User> findByJointypeAndOauthProviderId(Integer jointype, String oauthProviderId);

    Optional<User> findByMemberId(String memberId);

    boolean existsByEmail(String email);

    // 아이디 존재 여부 확인
    boolean existsByMemberId(String memberId);

    // 전화번호 존재 여부 확인
    boolean existsByPhone(String phone);

    // 모든 사용자 ID만 조회 (전체 엔티티를 로드하지 않음)
    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u")
    List<Long> findAllUserIds();

}
