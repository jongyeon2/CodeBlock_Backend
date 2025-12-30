package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.LectureResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LectureResource 리포지토리
 * 섹션 자료 관리를 위한 데이터 접근 계층
 */
@Repository
public interface LectureResourceRepository extends JpaRepository<LectureResource, Long> {

    /**
     * 섹션별 자료 목록 조회 (순서 기준 정렬)
     * @param sectionId 섹션 ID
     * @return 자료 목록
     */
    List<LectureResource> findBySectionIdOrderBySequenceAsc(Long sectionId);

    /**
     * 섹션별 자료 목록 조회 (업로드 시간 역순 정렬)
     * @param sectionId 섹션 ID
     * @return 자료 목록
     */
    List<LectureResource> findBySectionIdOrderByUploadAtDesc(Long sectionId);

    /**
     * 강의별 자료 목록 조회
     * @param lectureId 강의 ID
     * @return 자료 목록
     */
    List<LectureResource> findByLectureId(Long lectureId);

    /**
     * 섹션의 자료 개수 조회
     * @param sectionId 섹션 ID
     * @return 자료 개수
     */
    long countBySectionId(Long sectionId);
}