package com.studyblock.domain.user.repository;

import com.studyblock.domain.user.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
    
    /**
     * User ID로 AdminProfile 조회
     */
    Optional<AdminProfile> findByUserId(Long userId);
    
    /**
     * User ID로 AdminProfile 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}

