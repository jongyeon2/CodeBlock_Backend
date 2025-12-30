package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.CourseFaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseFaqRepository extends JpaRepository<CourseFaq, Long> {

    List<CourseFaq> findByCourseIdOrderByDisplayOrderAsc(Long courseId);
}
