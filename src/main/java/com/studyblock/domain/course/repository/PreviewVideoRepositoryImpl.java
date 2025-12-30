package com.studyblock.domain.course.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyblock.domain.course.entity.PreviewVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.studyblock.domain.course.entity.QPreviewVideo.previewVideo;
import static com.studyblock.domain.course.entity.QLecture.lecture;
import static com.studyblock.domain.user.entity.QInstructorProfile.instructorProfile;

/**
 * PreviewVideoRepositoryCustom 구현체
 * - QueryDSL을 사용한 효율적인 맛보기 비디오 조회
 * - N+1 문제 방지를 위한 Fetch Join 활용
 */
@Repository
@RequiredArgsConstructor
public class PreviewVideoRepositoryImpl implements PreviewVideoRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * PreviewVideo ID로 조회 (Lecture, Instructor Fetch Join)
     * - 단 1번의 쿼리로 PreviewVideo + Lecture + Instructor 모두 조회
     * - N+1 문제 완전 해결 (5개 쿼리 → 1개 쿼리)
     *
     * 실행되는 SQL:
     * SELECT pv.*, l.*, i.*
     * FROM preview_video pv
     * LEFT JOIN lecture l ON pv.lecture_id = l.id
     * LEFT JOIN instructor_profile i ON l.instructor_id = i.id
     * WHERE pv.id = ?
     */
    @Override
    public Optional<PreviewVideo> findByIdWithLectureAndInstructor(Long previewVideoId) {
        PreviewVideo result = queryFactory
                .selectFrom(previewVideo)
                .leftJoin(previewVideo.lecture, lecture).fetchJoin()  // Lecture Fetch Join
                .leftJoin(lecture.instructor, instructorProfile).fetchJoin()  // Instructor Fetch Join
                .where(previewVideo.id.eq(previewVideoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * Lecture ID로 PreviewVideo 조회 (Lecture, Instructor Fetch Join)
     * - 강의별 맛보기 비디오 조회 시 N+1 문제 방지
     * - 단 1번의 쿼리로 PreviewVideo + Lecture + Instructor 모두 조회
     *
     * 실행되는 SQL:
     * SELECT pv.*, l.*, i.*
     * FROM preview_video pv
     * LEFT JOIN lecture l ON pv.lecture_id = l.id
     * LEFT JOIN instructor_profile i ON l.instructor_id = i.id
     * WHERE pv.lecture_id = ?
     */
    @Override
    public Optional<PreviewVideo> findByLectureIdWithLectureAndInstructor(Long lectureId) {
        PreviewVideo result = queryFactory
                .selectFrom(previewVideo)
                .leftJoin(previewVideo.lecture, lecture).fetchJoin()  // Lecture Fetch Join
                .leftJoin(lecture.instructor, instructorProfile).fetchJoin()  // Instructor Fetch Join
                .where(previewVideo.lecture.id.eq(lectureId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}

