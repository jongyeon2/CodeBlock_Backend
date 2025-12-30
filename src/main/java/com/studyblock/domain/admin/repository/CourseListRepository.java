package com.studyblock.domain.admin.repository;

import com.studyblock.domain.admin.dto.CourseListResponse;
import com.studyblock.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseListRepository extends JpaRepository<Course, Long> {

    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.instructor i " +
           "LEFT JOIN FETCH i.user u " +
           "LEFT JOIN FETCH c.courseCategories cc " +
           "LEFT JOIN FETCH cc.category ")
    List<Course> findAllWithInstructor();

    // 차단된 강의 조회 (isPublished = false)
    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.instructor i " +
           "LEFT JOIN FETCH i.user u " +
           "LEFT JOIN FETCH c.courseCategories cc " +
           "LEFT JOIN FETCH cc.category " +
           "WHERE c.isPublished = false")
    List<Course> findByIsPublishedFalse();
}
