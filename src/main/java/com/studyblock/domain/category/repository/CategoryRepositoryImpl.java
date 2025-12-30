package com.studyblock.domain.category.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyblock.domain.category.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.studyblock.domain.category.entity.QCategory.category;

/**
 * CategoryRepositoryCustom 구현체
 * - QueryDSL을 사용한 계층적 카테고리 조회
 * - N+1 문제 없이 효율적인 쿼리 실행
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 모든 카테고리 조회 (정렬: depth, orderNo)
     * - 대분류 먼저, 그 다음 소분류 순서로 정렬
     *
     * 실행되는 SQL:
     * SELECT c.*
     * FROM category c
     * ORDER BY c.depth ASC, c.order_no ASC
     */
    @Override
    public List<Category> findAllCategoriesOrderByDepthAndOrderNo() {
        return queryFactory
                .selectFrom(category)
                .orderBy(
                        category.depth.asc(),
                        category.orderNo.asc()
                )
                .fetch();
    }

    /**
     * 대분류 카테고리만 조회 (parent_id가 NULL)
     * - depth = 0 또는 parent가 NULL인 카테고리
     * - orderNo 순으로 정렬
     *
     * 실행되는 SQL:
     * SELECT c.*
     * FROM category c
     * WHERE c.parent_id IS NULL
     * ORDER BY c.order_no ASC
     */
    @Override
    public List<Category> findParentCategories() {
        return queryFactory
                .selectFrom(category)
                .where(category.parent.isNull())
                .orderBy(category.orderNo.asc())
                .fetch();
    }

    /**
     * 특정 대분류의 소분류 카테고리 조회
     * - parentId에 해당하는 자식 카테고리들 조회
     * - orderNo 순으로 정렬
     *
     * 실행되는 SQL:
     * SELECT c.*
     * FROM category c
     * WHERE c.parent_id = ?
     * ORDER BY c.order_no ASC
     *
     * @param parentId 대분류 카테고리 ID
     * @return 소분류 카테고리 목록
     */
    @Override
    public List<Category> findChildCategoriesByParentId(Long parentId) {
        return queryFactory
                .selectFrom(category)
                .where(category.parent.id.eq(parentId))
                .orderBy(category.orderNo.asc())
                .fetch();
    }
}