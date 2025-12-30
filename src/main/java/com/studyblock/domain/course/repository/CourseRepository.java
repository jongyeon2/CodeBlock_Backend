package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {

    @EntityGraph(attributePaths = {"courseCategories", "courseCategories.category"})
    Optional<Course> findOneWithCategoriesById(Long id);

    List<Course> findTop10ByLevelAndIdNotOrderByEnrollmentCountDesc(CourseLevel level, Long excludeCourseId);

    @Query("""
            SELECT DISTINCT c
            FROM Course c
            JOIN c.courseCategories cc
            WHERE cc.category.id = :categoryId
              AND c.id <> :excludeCourseId
            ORDER BY c.enrollmentCount DESC
            """)
    List<Course> findTopByCategoryOrderByEnrollmentDesc(@Param("categoryId") Long categoryId,
                                                        @Param("excludeCourseId") Long excludeCourseId,
                                                        Pageable pageable);

    @Query("""
            SELECT DISTINCT c
            FROM Course c
            JOIN c.courseCategories cc
            WHERE cc.category.id = :categoryId
              AND c.level = :level
              AND c.id <> :excludeCourseId
            ORDER BY c.enrollmentCount DESC
            """)
    List<Course> findTopByCategoryAndLevelOrderByEnrollmentDesc(@Param("categoryId") Long categoryId,
                                                                @Param("level") CourseLevel level,
                                                                @Param("excludeCourseId") Long excludeCourseId,
                                                                Pageable pageable);

    List<Course> findTop10ByIdInOrderByEnrollmentCountDesc(List<Long> courseIds);

    /**
     * 카테고리 ID로 강의 검색 (페이지네이션)
     * 로드맵 노드의 관련 강의 조회에 사용
     */
    @EntityGraph(attributePaths = {"instructor", "instructor.user"})
    @Query("""
            SELECT DISTINCT c
            FROM Course c
            JOIN c.courseCategories cc
            WHERE cc.category.id = :categoryId
              AND c.isPublished = true
            ORDER BY c.enrollmentCount DESC
            """)
    Page<Course> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * 강사별 정규코스 조회 (최신순)
     * instructor.id로 접근하려면 언더스코어(_) 사용
     */
    List<Course> findByInstructor_IdOrderByCreatedAtDesc(Long instructorId);

    /**
     * 강사별 공개 정규코스만 조회 (최신순)
     */
    @Query("""
            SELECT c FROM Course c
            WHERE c.instructor.id = :instructorId
              AND c.isPublished = true
            ORDER BY c.createdAt DESC
            """)
    List<Course> findPublishedCoursesByInstructorId(@Param("instructorId") Long instructorId);

    /**
     * 강사별 정규코스 개수
     * instructor.id로 접근하려면 언더스코어(_) 사용
     */
    long countByInstructor_Id(Long instructorId);

    /**
     * 공개 상태별 강의 개수
     * 관리자 대시보드 통계에 사용
     */
    Integer countByIsPublished(Boolean isPublished);
}
