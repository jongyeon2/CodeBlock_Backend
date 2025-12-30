package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.CourseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    //중복 강의카드가 나오지 않도록(확인)
    @Query("""
        SELECT cc 
        FROM CourseCategory cc 
        JOIN FETCH cc.course 
        WHERE cc.category.id = :categoryId
        AND cc.course.isPublished = true
        """)
    List<CourseCategory> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     *  특정 카테고리(categoryId)에 속한 Course 목록을 페이징 조회
     */
    @Query("""
        SELECT DISTINCT cc.course 
        FROM CourseCategory cc
        WHERE cc.category.id = :categoryId
        AND cc.course.isPublished = true
        """)
    Page<Course> findCoursesByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

}
