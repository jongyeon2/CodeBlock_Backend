package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.PreviewVideo;

import java.util.Optional;

/**
 * PreviewVideo 커스텀 Repository 인터페이스
 * - QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 * - N+1 문제 방지를 위한 Fetch Join 활용
 */
public interface PreviewVideoRepositoryCustom {

    /**
     * PreviewVideo ID로 조회 (Lecture, Instructor Fetch Join)
     * - N+1 문제 방지를 위해 Fetch Join 사용
     * - 권한 검증 시 필요한 Lecture와 Instructor 정보를 한 번에 조회
     *
     * 실행되는 SQL:
     * SELECT pv.*, l.*, i.*
     * FROM preview_video pv
     * LEFT JOIN lecture l ON pv.lecture_id = l.id
     * LEFT JOIN instructor_profile i ON l.instructor_id = i.id
     * WHERE pv.id = ?
     *
     * @param previewVideoId 맛보기 비디오 ID
     * @return PreviewVideo (Lecture, Instructor 포함)
     */
    Optional<PreviewVideo> findByIdWithLectureAndInstructor(Long previewVideoId);

    /**
     * Lecture ID로 PreviewVideo 조회 (Lecture, Instructor Fetch Join)
     * - 강의별 맛보기 비디오 조회 시 N+1 문제 방지
     *
     * @param lectureId 강의 ID
     * @return PreviewVideo (Lecture, Instructor 포함)
     */
    Optional<PreviewVideo> findByLectureIdWithLectureAndInstructor(Long lectureId);
}

