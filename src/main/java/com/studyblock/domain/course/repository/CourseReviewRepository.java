package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Page<CourseReview> findByCourseId(Long courseId, Pageable pageable);

    Page<CourseReview> findByCourseIdAndLectureId(Long courseId, Long lectureId, Pageable pageable);

    long countByCourseId(Long courseId);

    @Query("select avg(r.rating) from CourseReview r where r.course.id = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") Long courseId);

    /**
     * 평점별 리뷰 개수 조회 (통계용)
     * @return List<Object[]>: [rating, count]
     */
    @Query("SELECT r.rating, COUNT(r) FROM CourseReview r WHERE r.course.id = :courseId GROUP BY r.rating")
    List<Object[]> countByRatingGroupByCourseId(@Param("courseId") Long courseId);

    /**
     * 특정 평점 이상의 리뷰 개수 조회 (추천율 계산용)
     */
    @Query("SELECT COUNT(r) FROM CourseReview r WHERE r.course.id = :courseId AND r.rating >= :minRating")
    long countByCourseIdAndRatingGreaterThanEqual(@Param("courseId") Long courseId, @Param("minRating") Integer minRating);

    /**
     * 사용자와 코스로 기존 리뷰 조회 (중복 체크용)
     * 최신 리뷰 하나만 반환 (여러 개가 있을 경우 대비)
     * Fetch Join으로 연관 엔티티 함께 조회
     */
    @Query("SELECT r FROM CourseReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.course " +
           "LEFT JOIN FETCH r.lecture " +
           "WHERE r.user.id = :userId AND r.course.id = :courseId ORDER BY r.createdAt DESC")
    List<CourseReview> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /**
     * 리뷰 ID로 Fetch Join하여 연관 엔티티와 함께 조회
     */
    @Query("SELECT r FROM CourseReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.course " +
           "LEFT JOIN FETCH r.lecture " +
           "WHERE r.id = :reviewId")
    java.util.Optional<CourseReview> findByIdWithRelations(@Param("reviewId") Long reviewId);

    /**
     * 사용자가 특정 코스에 리뷰를 작성했는지 확인
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // 특정 유저가 작성한 모든 리뷰 조회(최신순)
    @Query("""
        SELECT new com.studyblock.domain.course.dto.CourseReviewResponse(
            cr.id, cr.rating, cr.content, cr.lectureSpecific,
            l.id, l.title,
            u.nickname, u.img,
            cr.createdAt, cr.updatedAt,
            c.id, c.title
        )
        FROM CourseReview cr
        LEFT JOIN cr.course c
        LEFT JOIN cr.user u
        LEFT JOIN cr.lecture l
        WHERE cr.user.id = :userId
        ORDER BY cr.createdAt DESC
    """)
    List<com.studyblock.domain.course.dto.CourseReviewResponse> findByUserIdWithDetails(@Param("userId") Long userId);

    // 특정 유저의 리뷰 갯수 카운트
    long countByUserId(Long userId);
}
