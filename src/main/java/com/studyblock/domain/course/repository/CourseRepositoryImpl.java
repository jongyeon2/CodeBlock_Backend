package com.studyblock.domain.course.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.enums.CourseLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.studyblock.domain.category.entity.QCategory.category;
import static com.studyblock.domain.course.entity.QCourse.course;
import static com.studyblock.domain.course.entity.QCourseCategory.courseCategory;

/**
 * CourseRepositoryCustom 구현체
 * - QueryDSL을 사용한 효율적인 코스 조회
 * - N+1 문제 방지를 위한 Fetch Join 활용
 */
@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 코스 ID로 코스 조회 (카테고리 정보 포함)
     * - CourseCategory와 Category를 Fetch Join하여 N+1 문제 방지
     *
     * 실행되는 SQL:
     * SELECT c.*, cc.*, cat.*
     * FROM course c
     * LEFT JOIN course_category cc ON c.id = cc.course_id
     * LEFT JOIN category cat ON cc.category_id = cat.id
     * WHERE c.id = ?
     */
    @Override
    public Optional<Course> findByIdWithCategories(Long courseId) {
        Course result = queryFactory
                .selectFrom(course)
                .leftJoin(course.courseCategories, courseCategory).fetchJoin()
                .leftJoin(courseCategory.category, category).fetchJoin()
                .where(course.id.eq(courseId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 모든 공개 코스 조회
     * - isPublished = true인 코스만 조회
     * - 최신순 정렬 (createdAt DESC)
     */
    @Override
    public List<Course> findAllPublishedCourses() {
        return queryFactory
                .selectFrom(course)
                .where(course.isPublished.isTrue())
                .orderBy(course.createdAt.desc())
                .fetch();
    }

    /**
     * 카테고리별 공개 코스 조회
     * - 특정 카테고리에 속한 공개 코스 조회
     * - 최신순 정렬
     */
    @Override
    public List<Course> findPublishedCoursesByCategory(Long categoryId) {
        return queryFactory
                .selectFrom(course)
                .join(course.courseCategories, courseCategory)
                .where(
                        courseCategory.category.id.eq(categoryId),
                        course.isPublished.isTrue()
                )
                .orderBy(course.createdAt.desc())
                .fetch();
    }

    /**
     * 난이도별 공개 코스 조회
     * - 특정 난이도의 공개 코스 조회
     * - 최신순 정렬
     */
    @Override
    public List<Course> findPublishedCoursesByLevel(CourseLevel level) {
        return queryFactory
                .selectFrom(course)
                .where(
                        course.level.eq(level),
                        course.isPublished.isTrue()
                )
                .orderBy(course.createdAt.desc())
                .fetch();
    }

    /**
     * 인기 코스 조회 (수강생 수 기준)
     * - 공개된 코스 중 수강생 수가 많은 순으로 조회
     * - limit 개수만큼만 조회
     */
    @Override
    public List<Course> findPopularCourses(int limit) {
        return queryFactory
                .selectFrom(course)
                .where(course.isPublished.isTrue())
                .orderBy(course.enrollmentCount.desc())
                .limit(limit)
                .fetch();
    }
}