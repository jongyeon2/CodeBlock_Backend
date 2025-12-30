package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.CoursePrerequisite;
import com.studyblock.domain.course.enums.CoursePrerequisiteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    List<CoursePrerequisite> findByCourseIdOrderByDisplayOrderAsc(Long courseId);

    List<CoursePrerequisite> findByCourseIdAndTypeOrderByDisplayOrderAsc(Long courseId, CoursePrerequisiteType type);
}
