package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    /**
     * 특정 퀴즈의 모든 문제 조회 (순서대로)
     */
    List<QuizQuestion> findByQuiz_IdOrderBySequenceAsc(Long quizId);

    /**
     * 특정 퀴즈의 문제 개수
     */
    long countByQuiz_Id(Long quizId);

    /**
     * 문제 ID로 옵션 포함 조회
     */
    @Query("SELECT qq FROM QuizQuestion qq LEFT JOIN FETCH qq.options WHERE qq.id = :questionId")
    QuizQuestion findByIdWithOptions(@Param("questionId") Long questionId);

    /**
     * 퀴즈 ID로 모든 문제와 옵션 조회
     */
    @Query("SELECT qq FROM QuizQuestion qq LEFT JOIN FETCH qq.options WHERE qq.quiz.id = :quizId ORDER BY qq.sequence ASC")
    List<QuizQuestion> findByQuizIdWithOptions(@Param("quizId") Long quizId);

    /**
     * 특정 퀴즈의 특정 문제 조회 (소유권 검증용)
     */
    @Query("SELECT qq FROM QuizQuestion qq WHERE qq.quiz.id = :quizId AND qq.id = :questionId")
    java.util.Optional<QuizQuestion> findByQuiz_IdAndId(@Param("quizId") Long quizId, @Param("questionId") Long questionId);

    /**
     * 특정 퀴즈의 마지막 sequence 값 조회
     */
    @Query("SELECT MAX(qq.sequence) FROM QuizQuestion qq WHERE qq.quiz.id = :quizId")
    Integer findMaxSequenceByQuizId(@Param("quizId") Long quizId);
}
