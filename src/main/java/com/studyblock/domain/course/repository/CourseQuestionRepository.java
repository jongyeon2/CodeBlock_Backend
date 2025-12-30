package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.CourseQuestion;
import com.studyblock.domain.course.enums.CourseQuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, Long> {

    Page<CourseQuestion> findByCourseId(Long courseId, Pageable pageable);

    Page<CourseQuestion> findByCourseIdAndStatus(Long courseId, CourseQuestionStatus status, Pageable pageable);
}
