package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;

import java.util.List;
import java.util.Optional;

/**
 * Course Repository Custom 인터페이스
 * - QueryDSL을 사용한 복잡한 코스 조회 쿼리
 */
public interface CourseRepositoryCustom {

    /**
     * 코스 ID로 코스 조회 (카테고리 정보 포함)
     * - CourseCategory와 Category를 Fetch Join
     * @param courseId 코스 ID
     * @return 코스 엔티티 (카테고리 포함)
     */
    Optional<Course> findByIdWithCategories(Long courseId);

    /**
     * 모든 공개 코스 조회
     * - isPublished = true인 코스만 조회
     * - 최신순 정렬
     * @return 공개 코스 목록
     */
    List<Course> findAllPublishedCourses();

    /**
     * 카테고리별 공개 코스 조회
     * - 특정 카테고리에 속한 공개 코스 조회
     * @param categoryId 카테고리 ID
     * @return 카테고리별 공개 코스 목록
     */
    List<Course> findPublishedCoursesByCategory(Long categoryId);

    /**
     * 난이도별 공개 코스 조회
     * @param level 난이도
     * @return 난이도별 공개 코스 목록
     */
    List<Course> findPublishedCoursesByLevel(CourseLevel level);

    /**
     * 인기 코스 조회 (수강생 수 기준)
     * @param limit 조회 개수
     * @return 인기 코스 목록
     */
    List<Course> findPopularCourses(int limit);
}