package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.CourseLearningOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseLearningOutcomeRepository extends JpaRepository<CourseLearningOutcome, Long> {

    List<CourseLearningOutcome> findByCourseIdOrderByDisplayOrderAsc(Long courseId);
}
