package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    /**
     * 코스 ID로 섹션 목록 조회 (sequence 순서대로)
     */
    List<Section> findByCourseIdOrderBySequenceAsc(Long courseId);

    /**
     * 코스 ID로 섹션 목록 조회 (Lecture와 함께 fetch join)
     */
    @Query("SELECT DISTINCT s FROM Section s " +
           "LEFT JOIN FETCH s.lectures l " +
           "WHERE s.course.id = :courseId " +
           "ORDER BY s.sequence ASC, l.sequence ASC")
    List<Section> findByCourseIdWithLectures(@Param("courseId") Long courseId);

    /**
     * 섹션 ID로 섹션 조회 (Lecture와 함께 fetch join)
     */
    @Query("SELECT s FROM Section s " +
           "LEFT JOIN FETCH s.lectures l " +
           "WHERE s.id = :sectionId " +
           "ORDER BY l.sequence ASC")
    Section findByIdWithLectures(@Param("sectionId") Long sectionId);

    /**
     * 코스 ID로 섹션 개수 조회
     */
    long countByCourseId(Long courseId);

    /**
     * 코스와 sequence로 섹션 존재 여부 확인
     */
    boolean existsByCourseIdAndSequence(Long courseId, Integer sequence);
}

