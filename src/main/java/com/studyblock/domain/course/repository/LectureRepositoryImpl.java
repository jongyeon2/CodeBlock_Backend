package com.studyblock.domain.course.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyblock.domain.course.entity.Lecture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.studyblock.domain.course.entity.QLecture.lecture;
import static com.studyblock.domain.course.entity.QVideo.video;
import static com.studyblock.domain.course.entity.QPreviewVideo.previewVideo;
import static com.studyblock.domain.course.entity.QSection.section;

/**
 * LectureRepositoryCustom 구현체
 * - QueryDSL을 사용한 효율적인 강의 조회
 * - N+1 문제 방지를 위한 Fetch Join 활용
 */
@Repository
@RequiredArgsConstructor
public class LectureRepositoryImpl implements LectureRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 코스의 모든 강의 조회 (Video Fetch Join)
     * - N+1 문제 방지를 위해 Video를 함께 조회
     * - DISTINCT 사용으로 카테시안 곱 방지
     * 
     * 실행되는 SQL:
     * SELECT DISTINCT l.*, v.*
     * FROM lecture l
     * LEFT JOIN video v ON l.id = v.lecture_id
     * WHERE l.course_id = ?
     * ORDER BY l.sequence ASC
     *
     * 효과:
     * - 기존: 1 (Lecture 조회) + N (Video 조회) = 1 + N 쿼리
     * - 개선: 1개 쿼리로 모든 데이터 조회
     */
    @Override
    public List<Lecture> findByCourseIdWithVideoOrderBySequenceAsc(Long courseId) {
        return queryFactory
                .selectFrom(lecture)
                .distinct()
                .leftJoin(lecture.video, video).fetchJoin()
                .where(lecture.course.id.eq(courseId))
                .orderBy(lecture.sequence.asc())
                .fetch();
    }

    /**
     * 특정 코스의 모든 강의 조회 (PreviewVideo Fetch Join)
     * - N+1 문제 방지를 위해 PreviewVideo를 함께 조회
     * - DISTINCT 사용으로 카테시안 곱 방지
     * 
     * 실행되는 SQL:
     * SELECT DISTINCT l.*, pv.*
     * FROM lecture l
     * LEFT JOIN preview_video pv ON l.id = pv.lecture_id
     * WHERE l.course_id = ?
     * ORDER BY l.sequence ASC
     *
     * 효과:
     * - 기존: 1 (Lecture 조회) + N (PreviewVideo 조회) = 1 + N 쿼리
     * - 개선: 1개 쿼리로 모든 데이터 조회
     */
    @Override
    public List<Lecture> findByCourseIdWithPreviewVideoOrderBySequenceAsc(Long courseId) {
        return queryFactory
                .selectFrom(lecture)
                .distinct()
                .leftJoin(lecture.previewVideo, previewVideo).fetchJoin()
                .where(lecture.course.id.eq(courseId))
                .orderBy(lecture.sequence.asc())
                .fetch();
    }

    /**
     * 특정 코스의 모든 강의 조회 (Video + Section Fetch Join)
     * - N+1 문제 방지를 위해 Video와 Section을 함께 조회
     * - DISTINCT 사용으로 카테시안 곱 방지
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
     */
    @Override
    public List<Lecture> findByCourseIdWithVideoAndSectionOrderBySequenceAsc(Long courseId) {
        return queryFactory
                .selectFrom(lecture)
                .distinct()
                .leftJoin(lecture.video, video).fetchJoin()
                .leftJoin(lecture.section, section).fetchJoin()  // Section Fetch Join 추가
                .where(lecture.course.id.eq(courseId))
                .orderBy(lecture.sequence.asc())
                .fetch();
    }
}

