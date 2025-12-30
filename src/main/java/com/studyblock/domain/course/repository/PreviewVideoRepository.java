package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.PreviewVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 맛보기 비디오 Repository
 * - Lecture와 1:1 관계이므로 lectureId로 조회
 * - QueryDSL을 사용한 커스텀 쿼리는 PreviewVideoRepositoryCustom 참조
 */
@Repository
public interface PreviewVideoRepository extends JpaRepository<PreviewVideo, Long>, PreviewVideoRepositoryCustom {
    
    /**
     * 강의 ID로 맛보기 비디오 조회
     * @param lectureId 강의 ID
     * @return 맛보기 비디오 Optional
     */
    Optional<PreviewVideo> findByLectureId(Long lectureId);
    
    /**
     * 강의 ID로 맛보기 비디오 존재 여부 확인
     * @param lectureId 강의 ID
     * @return 존재 여부
     */
    boolean existsByLectureId(Long lectureId);
    
    /**
     * 강의 ID로 맛보기 비디오 삭제
     * @param lectureId 강의 ID
     */
    void deleteByLectureId(Long lectureId);
    
    /**
     * 여러 강의 ID로 맛보기 비디오 목록 조회
     * - N+1 문제 방지를 위해 IN 절 사용
     * - 일괄 조회로 성능 최적화
     * @param lectureIds 강의 ID 목록
     * @return 맛보기 비디오 목록
     */
    List<PreviewVideo> findByLectureIdIn(List<Long> lectureIds);
}
