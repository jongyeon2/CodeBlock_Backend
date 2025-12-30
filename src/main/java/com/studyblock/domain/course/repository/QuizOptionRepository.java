package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {

    /**
     * 특정 문제의 모든 옵션 조회 (순서대로)
     */
    List<QuizOption> findByQuizQuestionIdOrderBySequenceAsc(Long questionId);

    /**
     * 특정 문제의 정답 옵션 조회
     */
    QuizOption findByQuizQuestionIdAndIsCorrectTrue(Long questionId);
}
