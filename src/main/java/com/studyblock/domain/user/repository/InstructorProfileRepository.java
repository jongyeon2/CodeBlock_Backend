package com.studyblock.domain.user.repository;

import com.studyblock.domain.user.entity.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {

    /**
     * User ID로 InstructorProfile 조회
     */
    Optional<InstructorProfile> findByUserId(Long userId);

    /**
     * User ID로 InstructorProfile 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * Channel URL로 InstructorProfile 조회
     */
    Optional<InstructorProfile> findByChannelUrl(String channelUrl);

    /**
     * 활성화된 강사 프로필만 조회
     */
    @Query("SELECT ip FROM InstructorProfile ip WHERE ip.id = :id AND ip.isActive = true")
    Optional<InstructorProfile> findActiveProfileById(@Param("id") Long id);

    /**
     * 채널 상태가 ACTIVE인 강사 프로필 조회
     */
    @Query("SELECT ip FROM InstructorProfile ip WHERE ip.channelStatus = 'ACTIVE' AND ip.isActive = true")
    java.util.List<InstructorProfile> findAllActiveInstructors();
}