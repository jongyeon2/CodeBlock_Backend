package com.studyblock.domain.enrollment.repository;

import com.studyblock.domain.enrollment.entity.LectureCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 강의 완료 Repository
 */
@Repository
public interface LectureCompletionRepository extends JpaRepository<LectureCompletion, Long> {

    /**
     * 사용자 ID와 강의 ID로 완료 정보 조회
     */
    Optional<LectureCompletion> findByUserIdAndLectureId(Long userId, Long lectureId);

    /**
     * 사용자 ID와 강의 ID로 완료 정보 존재 여부 확인
     */
    boolean existsByUserIdAndLectureId(Long userId, Long lectureId);

    /**
     * 사용자가 완료한 모든 강의 조회
     */
    List<LectureCompletion> findByUserIdAndIsCompleted(Long userId, Boolean isCompleted);

    /**
     * 특정 강의를 완료한 모든 사용자 수 조회
     */
    long countByLectureIdAndIsCompleted(Long lectureId, Boolean isCompleted);

    /**
     * 수강신청 ID로 모든 강의 완료 정보 조회
     */
    List<LectureCompletion> findByCourseEnrollmentId(Long enrollmentId);

    /**
     * 수강신청 ID와 완료 여부로 조회
     */
    List<LectureCompletion> findByCourseEnrollmentIdAndIsCompleted(Long enrollmentId, Boolean isCompleted);

    /**
     * 수강신청의 완료된 강의 ID 목록 조회
     */
    @Query("SELECT lc.lecture.id FROM LectureCompletion lc " +
            "WHERE lc.courseEnrollment.id = :enrollmentId " +
            "AND lc.isCompleted = true")
    List<Long> findCompletedLectureIdsByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    /**
     * 수강신청의 완료된 강의 수 조회
     */
    @Query("SELECT COUNT(lc) FROM LectureCompletion lc " +
            "WHERE lc.courseEnrollment.id = :enrollmentId " +
            "AND lc.isCompleted = true")
    long countCompletedByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    /**
     * 수강신청의 전체 강의 수 조회
     */
    @Query("SELECT COUNT(lc) FROM LectureCompletion lc " +
            "WHERE lc.courseEnrollment.id = :enrollmentId")
    long countTotalByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    /**
     * 사용자의 특정 강좌 내 완료한 강의 조회
     */
    @Query("SELECT lc FROM LectureCompletion lc " +
            "WHERE lc.user.id = :userId " +
            "AND lc.lecture.section.course.id = :courseId " +
            "AND lc.isCompleted = true")
    List<LectureCompletion> findCompletedLecturesByUserAndCourse(@Param("userId") Long userId,
                                                                   @Param("courseId") Long courseId);

    /**
     * 특정 강좌의 전체 강의 수 조회
     */
    @Query("SELECT COUNT(DISTINCT l) FROM Lecture l " +
            "WHERE l.section.course.id = :courseId")
    long countTotalLecturesByCourseId(@Param("courseId") Long courseId);

    /**
     * 사용자가 특정 강좌에서 완료한 강의 수 조회
     */
    @Query("SELECT COUNT(lc) FROM LectureCompletion lc " +
            "WHERE lc.user.id = :userId " +
            "AND lc.lecture.section.course.id = :courseId " +
            "AND lc.isCompleted = true")
    long countCompletedLecturesByUserAndCourse(@Param("userId") Long userId,
                                                @Param("courseId") Long courseId);

    /**
     * 사용자의 특정 섹션 내 완료한 강의 조회
     */
    @Query("SELECT lc FROM LectureCompletion lc " +
            "WHERE lc.user.id = :userId " +
            "AND lc.lecture.section.id = :sectionId " +
            "AND lc.isCompleted = true")
    List<LectureCompletion> findCompletedLecturesByUserAndSection(@Param("userId") Long userId,
                                                                    @Param("sectionId") Long sectionId);

    /**
     * 사용자가 특정 섹션에서 완료한 강의 수 조회
     */
    @Query("SELECT COUNT(lc) FROM LectureCompletion lc " +
            "WHERE lc.user.id = :userId " +
            "AND lc.lecture.section.id = :sectionId " +
            "AND lc.isCompleted = true")
    long countCompletedLecturesByUserAndSection(@Param("userId") Long userId,
                                                  @Param("sectionId") Long sectionId);

    /**
     * 특정 섹션의 전체 강의 수 조회
     */
    @Query("SELECT COUNT(l) FROM Lecture l WHERE l.section.id = :sectionId")
    long countTotalLecturesBySectionId(@Param("sectionId") Long sectionId);

    /**
     * 퀴즈를 시도한 강의 완료 정보 조회
     */
    @Query("SELECT lc FROM LectureCompletion lc " +
            "WHERE lc.user.id = :userId " +
            "AND lc.quizAttempts > 0")
    List<LectureCompletion> findWithQuizAttemptsByUserId(@Param("userId") Long userId);

    /**
     * 수강신청별 강의 완료율 계산
     */
    @Query("SELECT (COUNT(CASE WHEN lc.isCompleted = true THEN 1 END) * 100.0 / COUNT(*)) " +
            "FROM LectureCompletion lc " +
            "WHERE lc.courseEnrollment.id = :enrollmentId")
    Double calculateCompletionRateByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    /**
     * 사용자의 평균 강의 시청률 조회
     */
    @Query("SELECT AVG(lc.videoWatchPercentage) FROM LectureCompletion lc " +
            "WHERE lc.user.id = :userId")
    Double calculateAverageWatchPercentageByUserId(@Param("userId") Long userId);

    /**
     * 강좌의 평균 완료율 조회 (모든 수강생 기준)
     */
    @Query("SELECT (COUNT(CASE WHEN lc.isCompleted = true THEN 1 END) * 100.0 / COUNT(*)) " +
            "FROM LectureCompletion lc " +
            "WHERE lc.lecture.section.course.id = :courseId")
    Double calculateAverageCompletionRateByCourseId(@Param("courseId") Long courseId);

    /**
     * 일괄 삭제 (강좌 삭제 시 사용)
     */
    @Query("DELETE FROM LectureCompletion lc " +
            "WHERE lc.lecture.section.course.id = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);

    /**
     * 일괄 삭제 (사용자 삭제 시 사용)
     */
    void deleteAllByUserId(Long userId);
}
