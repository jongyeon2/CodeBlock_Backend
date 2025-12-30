package com.studyblock.domain.user.repository;

import com.studyblock.domain.user.entity.InstructorProfile;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorRepository extends JpaRepository<InstructorProfile, Long> {

    // 강사 프로필 가져오기
    @Query("select ip from InstructorProfile ip join fetch ip.user where ip.user.id = :userId")
    Optional<InstructorProfile> findByUserIdWithUser(@Param("userId") Long userId);
}
