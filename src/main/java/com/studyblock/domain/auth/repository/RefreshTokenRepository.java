package com.studyblock.domain.auth.repository;

import com.studyblock.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * RefreshToken Redis Repository
 * - JpaRepository 대신 CrudRepository 사용
 * - Redis는 간단한 CRUD만 지원
 */
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    /**
     * userId로 RefreshToken 조회
     * - @Indexed 덕분에 userId로 검색 가능
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * userId로 RefreshToken 삭제 (로그아웃 시 사용)
     */
    void deleteByUserId(Long userId);
}