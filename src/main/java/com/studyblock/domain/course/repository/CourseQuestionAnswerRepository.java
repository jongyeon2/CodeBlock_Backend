package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.CourseQuestionAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseQuestionAnswerRepository extends JpaRepository<CourseQuestionAnswer, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<CourseQuestionAnswer> findByQuestion_IdAndDeletedFalseOrderByCreatedAtAsc(Long questionId);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM CourseQuestionAnswer a WHERE a.question.id IN :questionIds AND a.deleted = false ORDER BY a.question.id, a.createdAt")
    List<CourseQuestionAnswer> findByQuestionIds(Collection<Long> questionIds);

    Optional<CourseQuestionAnswer> findByIdAndQuestion_Course_IdAndDeletedFalse(Long id, Long courseId);

    long countByQuestion_IdAndDeletedFalse(Long questionId);
}

