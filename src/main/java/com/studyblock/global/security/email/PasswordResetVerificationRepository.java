package com.studyblock.global.security.email;

import org.springframework.data.repository.CrudRepository;

/**
 * 비밀번호 찾기 인증 코드 Redis 저장소
 */
public interface PasswordResetVerificationRepository extends CrudRepository<PasswordResetVerification, String> {
}

