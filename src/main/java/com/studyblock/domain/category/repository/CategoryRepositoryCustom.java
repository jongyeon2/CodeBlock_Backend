package com.studyblock.domain.category.repository;

import com.studyblock.domain.category.entity.Category;

import java.util.List;

/**
 * CategoryRepository Custom 인터페이스
 * - QueryDSL을 사용한 복잡한 카테고리 조회 쿼리
 */
public interface CategoryRepositoryCustom {

    /**
     * 모든 카테고리 조회 (정렬: depth, orderNo)
     * @return 모든 카테고리 목록
     */
    List<Category> findAllCategoriesOrderByDepthAndOrderNo();

    /**
     * 대분류 카테고리만 조회 (parent_id가 NULL)
     * @return 대분류 카테고리 목록
     */
    List<Category> findParentCategories();

    /**
     * 특정 대분류의 소분류 카테고리 조회
     * @param parentId 대분류 카테고리 ID
     * @return 소분류 카테고리 목록
     */
    List<Category> findChildCategoriesByParentId(Long parentId);
}