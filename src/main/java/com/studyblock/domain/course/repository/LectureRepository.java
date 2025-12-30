package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.enums.LectureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long>, LectureRepositoryCustom {

    /**
     * 특정 코스의 모든 강의 조회 (순서대로)
     */
    List<Lecture> findByCourseIdOrderBySequenceAsc(Long courseId);

    /**
     * 특정 코스의 공개된 강의만 조회
     */
    List<Lecture> findByCourseIdAndStatusOrderBySequenceAsc(Long courseId, LectureStatus status);

    /**
     * 특정 코스의 강의 개수
     */
    long countByCourseId(Long courseId);

    /**
     * 특정 코스의 특정 상태 강의 개수
     */
    long countByCourseIdAndStatus(Long courseId, LectureStatus status);

    /**
     * 강의 ID로 코스 ID 조회
     */
    @Query("SELECT l.course.id FROM Lecture l WHERE l.id = :lectureId")
    Optional<Long> findCourseIdByLectureId(@Param("lectureId") Long lectureId);

    /**
     * 코스에 연결된 첫 번째 강의의 강사 조회
     */
    Optional<Lecture> findFirstByCourseIdAndInstructorIsNotNullOrderBySequenceAsc(Long courseId);

    /**
     * 강사가 참여한 코스 ID 목록 조회
     */
    @Query("select distinct l.course.id from Lecture l where l.instructor.id = :instructorId")
    List<Long> findCourseIdsByInstructorId(@Param("instructorId") Long instructorId);

}
