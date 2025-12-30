package com.studyblock.domain.admin.repository;

import com.studyblock.domain.course.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewListRepository extends JpaRepository<CourseReview, Long> {

}
