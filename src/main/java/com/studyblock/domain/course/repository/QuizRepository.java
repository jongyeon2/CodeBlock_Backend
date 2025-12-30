package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * 특정 강의의 모든 퀴즈 조회 (순서대로)
     */
    List<Quiz> findByLecture_IdOrderBySequenceAsc(Long lectureId);

    /**
     * 특정 섹션의 모든 퀴즈 조회 (순서대로)
     */
    List<Quiz> findBySection_IdOrderBySequenceAsc(Long sectionId);

    /**
     * 특정 강의의 퀴즈 개수
     */
    long countByLecture_Id(Long lectureId);

    /**
     * 특정 섹션의 퀴즈 개수
     */
    long countBySection_Id(Long sectionId);

    /**
     * 퀴즈 ID로 문제 포함 조회
     */
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :quizId")
    Quiz findByIdWithQuestions(@Param("quizId") Long quizId);

    /**
     * 특정 코스의 모든 퀴즈 조회 (순서대로)
     */
    List<Quiz> findByCourse_IdOrderBySequenceAsc(Long courseId);

    /**
     * 특정 섹션의 마지막 sequence 값 조회
     */
    @Query("SELECT MAX(q.sequence) FROM Quiz q WHERE q.section.id = :sectionId")
    Integer findMaxSequenceBySectionId(@Param("sectionId") Long sectionId);

    /**
     * 퀴즈 ID로 Course와 Instructor를 함께 조회 (권한 검증용)
     * LazyInitializationException 방지를 위해 JOIN FETCH 사용
     */
    @Query("SELECT q FROM Quiz q " +
           "JOIN FETCH q.course c " +
           "JOIN FETCH c.instructor " +
           "WHERE q.id = :quizId")
    java.util.Optional<Quiz> findByIdWithCourseAndInstructor(@Param("quizId") Long quizId);
}
