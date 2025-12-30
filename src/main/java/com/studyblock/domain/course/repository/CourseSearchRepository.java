package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseSearchRepository extends JpaRepository<Course, Long> {

    // 제목으로 서치 가능 (강사 정보 포함)
    @Query("SELECT DISTINCT c FROM Course c " +
            "LEFT JOIN FETCH c.instructor i " +
            "LEFT JOIN FETCH i.user u " +
            "LEFT JOIN c.courseCategories cc " +
            "LEFT JOIN cc.category cat " +
            "WHERE (c.title LIKE %:keyword% OR cat.name LIKE %:keyword%) " +
            "AND c.isPublished = true")
    List<Course> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 전체 강의 조회 (강사 정보 포함 - 초성 검색용)
    @Query("SELECT c FROM Course c " +
            "LEFT JOIN FETCH c.instructor i " +
            "LEFT JOIN FETCH i.user u " +
            "WHERE c.isPublished = true")
    List<Course> findAllWithInstructor();
}
