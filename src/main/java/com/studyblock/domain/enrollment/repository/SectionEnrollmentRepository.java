package com.studyblock.domain.enrollment.repository;

import com.studyblock.domain.enrollment.entity.SectionEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionEnrollmentRepository extends JpaRepository<SectionEnrollment, Long> {

    Optional<SectionEnrollment> findByUser_IdAndSection_Id(Long userId, Long sectionId);

    List<SectionEnrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    long countByUser_IdAndCourse_Id(Long userId, Long courseId);

    /**
     * 사용자의 모든 섹션 수강 정보 조회
     * @param userId 사용자 ID
     * @return 섹션 수강 정보 리스트
     */
    List<SectionEnrollment> findByUser_Id(Long userId);
}

