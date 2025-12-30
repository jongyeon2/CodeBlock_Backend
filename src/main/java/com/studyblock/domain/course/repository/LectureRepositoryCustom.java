package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Lecture;

import java.util.List;

/**
 * Lecture 커스텀 Repository 인터페이스
 * - QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 * - N+1 문제 방지를 위한 Fetch Join 활용
 */
public interface LectureRepositoryCustom {

    /**
     * 특정 코스의 모든 강의 조회 (Video Fetch Join)
     * - N+1 문제 방지를 위해 Video를 함께 조회
     * - 강의 순서대로 정렬
     *
     * 실행되는 SQL:
     * SELECT DISTINCT l.*, v.*
     * FROM lecture l
     * LEFT JOIN video v ON l.id = v.lecture_id
     * WHERE l.course_id = ?
     * ORDER BY l.sequence ASC
     *
     * @param courseId 코스 ID
     * @return 강의 목록 (Video 포함)
     */
    List<Lecture> findByCourseIdWithVideoOrderBySequenceAsc(Long courseId);

    /**
     * 특정 코스의 모든 강의 조회 (PreviewVideo Fetch Join)
     * - N+1 문제 방지를 위해 PreviewVideo를 함께 조회
     * - 강의 순서대로 정렬
     *
     * 실행되는 SQL:
     * SELECT DISTINCT l.*, pv.*
     * FROM lecture l
     * LEFT JOIN preview_video pv ON l.id = pv.lecture_id
     * WHERE l.course_id = ?
     * ORDER BY l.sequence ASC
     *
     * @param courseId 코스 ID
     * @return 강의 목록 (PreviewVideo 포함)
     */
    List<Lecture> findByCourseIdWithPreviewVideoOrderBySequenceAsc(Long courseId);

    /**
     * 특정 코스의 모든 강의 조회 (Video + Section Fetch Join)
     * - N+1 문제 방지를 위해 Video와 Section을 함께 조회
     * - 강의 순서대로 정렬
     *
     * 실행되는 SQL:
     * SELECT DISTINCT l.*, v.*, s.*
     * FROM lecture l
     * LEFT JOIN video v ON l.id = v.lecture_id
     * LEFT JOIN section s ON l.section_id = s.id
     * WHERE l.course_id = ?
     * ORDER BY l.sequence ASC
     *
     * 효과:
     * - 기존: 1 (Lecture) + N (Video) + N (Section) = 1 + 2N 쿼리
     * - 개선: 1개 쿼리로 모든 데이터 조회
     *
     * @param courseId 코스 ID
     * @return 강의 목록 (Video, Section 포함)
     */
    List<Lecture> findByCourseIdWithVideoAndSectionOrderBySequenceAsc(Long courseId);
}

