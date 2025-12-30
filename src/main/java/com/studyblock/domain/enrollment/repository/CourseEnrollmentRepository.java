package com.studyblock.domain.enrollment.repository;

import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 수강신청 Repository
 */
@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    /**
     * 사용자 ID와 강좌 ID로 수강신청 조회
     */
    Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * 사용자 ID와 강좌 ID로 수강신청 조회 (Course 즉시 로딩)
     */
    @EntityGraph(attributePaths = {"course"})
    Optional<CourseEnrollment> findWithCourseByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * 사용자 ID와 강좌 ID로 수강신청 존재 여부 확인
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * 사용자의 모든 수강신청 조회 (페이징)
     */
    Page<CourseEnrollment> findByUserId(Long userId, Pageable pageable);

    /**
     * 사용자의 특정 상태 수강신청 조회
     */
    Page<CourseEnrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status, Pageable pageable);

    /**
     * 사용자의 활성 수강신청 조회 (ACTIVE + COMPLETED)
     */
    @EntityGraph(attributePaths = {
            "course",
            "course.instructor",
            "course.instructor.user",
            "user"
    })
    @Query("SELECT e FROM CourseEnrollment e WHERE e.user.id = :userId " +
            "AND (e.status = com.studyblock.domain.enrollment.enums.EnrollmentStatus.ACTIVE " +
            "OR e.status = com.studyblock.domain.enrollment.enums.EnrollmentStatus.COMPLETED) " +
            "ORDER BY e.lastAccessedAt DESC NULLS LAST")
    Page<CourseEnrollment> findActiveEnrollmentsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 강좌의 모든 수강생 조회 (페이징)
     */
    Page<CourseEnrollment> findByCourseId(Long courseId, Pageable pageable);

    /**
     * 강좌의 활성 수강생 수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM course_enrollment e WHERE e.course_id = :courseId " +
            "AND e.status IN ('ACTIVE', 'COMPLETED')",
            nativeQuery = true)
    long countActiveByCourseId(@Param("courseId") Long courseId);

    /**
     * 사용자의 활성 수강신청 수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM course_enrollment e WHERE e.user_id = :userId " +
            "AND e.status IN ('ACTIVE', 'COMPLETED')",
            nativeQuery = true)
    long countActiveByUserId(@Param("userId") Long userId);

    /**
     * 환불 가능한 수강신청 조회
     */
    @Query("SELECT e FROM CourseEnrollment e WHERE e.user.id = :userId " +
            "AND e.status = com.studyblock.domain.enrollment.enums.EnrollmentStatus.ACTIVE " +
            "AND e.refundableUntil >= :now " +
            "AND e.contentViewPercentage <= e.refundLimitPercentage")
    List<CourseEnrollment> findRefundableEnrollments(@Param("userId") Long userId,
                                                      @Param("now") LocalDateTime now);

    /**
     * 만료 예정인 수강신청 조회
     */
    @Query("SELECT e FROM CourseEnrollment e WHERE e.status = com.studyblock.domain.enrollment.enums.EnrollmentStatus.ACTIVE " +
            "AND e.expiresAt IS NOT NULL " +
            "AND e.expiresAt BETWEEN :start AND :end")
    List<CourseEnrollment> findExpiringEnrollments(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    /**
     * 만료된 수강신청 조회 (배치 처리용)
     */
    @Query("SELECT e FROM CourseEnrollment e WHERE e.status = com.studyblock.domain.enrollment.enums.EnrollmentStatus.ACTIVE " +
            "AND e.expiresAt IS NOT NULL " +
            "AND e.expiresAt < :now")
    List<CourseEnrollment> findExpiredEnrollments(@Param("now") LocalDateTime now);

    /**
     * 최근 접속이 오래된 수강신청 조회 (재참여 유도용)
     */
    @Query("SELECT e FROM CourseEnrollment e WHERE e.status = com.studyblock.domain.enrollment.enums.EnrollmentStatus.ACTIVE " +
            "AND e.lastAccessedAt < :thresholdDate " +
            "ORDER BY e.lastAccessedAt ASC")
    Page<CourseEnrollment> findInactiveEnrollments(@Param("thresholdDate") LocalDateTime thresholdDate,
                                                    Pageable pageable);

    /**
     * 진도율이 특정 범위인 수강신청 조회
     */
    @Query("SELECT e FROM CourseEnrollment e WHERE e.user.id = :userId " +
            "AND e.progressPercentage >= :minProgress " +
            "AND e.progressPercentage <= :maxProgress")
    List<CourseEnrollment> findByUserIdAndProgressRange(@Param("userId") Long userId,
                                                         @Param("minProgress") double minProgress,
                                                         @Param("maxProgress") double maxProgress);

    /**
     * 완료한 강좌 수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM course_enrollment e WHERE e.user_id = :userId " +
            "AND e.status = 'COMPLETED'",
            nativeQuery = true)
    long countCompletedByUserId(@Param("userId") Long userId);

    /**
     * 특정 강좌에 사용자가 접근 가능한지 확인
     */
    @Query(value = "SELECT COUNT(*) " +
            "FROM course_enrollment e WHERE e.user_id = :userId " +
            "AND e.course_id = :courseId " +
            "AND e.status IN ('ACTIVE', 'COMPLETED') " +
            "AND (e.expires_at IS NULL OR e.expires_at > :now)",
            nativeQuery = true)
    long countAccessibleEnrollments(@Param("userId") Long userId,
                                     @Param("courseId") Long courseId,
                                     @Param("now") LocalDateTime now);

    /**
     * 주문 ID로 수강신청 조회
     */
    List<CourseEnrollment> findByOrderId(Long orderId);

    /**
     * 강사의 모든 강좌 수강생 통계
     */
    @Query(value = "SELECT COUNT(DISTINCT e.id) FROM course_enrollment e " +
            "INNER JOIN course c ON e.course_id = c.id " +
            "WHERE c.instructor_id = :instructorId " +
            "AND e.status IN ('ACTIVE', 'COMPLETED')",
            nativeQuery = true)
    long countStudentsByInstructorId(@Param("instructorId") Long instructorId);

    /**
     * 수강신청 출처별 통계 (관리자용)
     */
    @Query("SELECT e.enrollmentSource, COUNT(e) FROM CourseEnrollment e " +
            "WHERE e.course.id = :courseId " +
            "GROUP BY e.enrollmentSource")
    List<Object[]> countByEnrollmentSource(@Param("courseId") Long courseId);
}
