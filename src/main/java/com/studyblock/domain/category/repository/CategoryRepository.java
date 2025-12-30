package com.studyblock.domain.category.repository;

import com.studyblock.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 카테고리 Repository
 * - 카테고리 기본 CRUD
 * - 계층 구조 카테고리 조회
 * - QueryDSL 커스텀 쿼리 (CategoryRepositoryCustom)
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryRepositoryCustom {

    /**
     * 부모 카테고리로 자식 카테고리 조회
     * @param parentId 부모 카테고리 ID
     * @return 자식 카테고리 목록
     */
    List<Category> findByParent_IdOrderByOrderNoAsc(Long parentId);

    /**
     * 최상위 카테고리 조회 (부모가 없는 카테고리)
     * @return 최상위 카테고리 목록
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.orderNo ASC")
    List<Category> findTopLevelCategories();

    /**
     * 특정 깊이의 카테고리 조회
     * @param depth 카테고리 깊이
     * @return 해당 깊이의 카테고리 목록
     */
    List<Category> findByDepthOrderByOrderNoAsc(Byte depth);

    /**
     * 카테고리명으로 검색
     * @param name 카테고리명
     * @return 매칭되는 카테고리 목록
     */
    List<Category> findByNameContainingIgnoreCaseOrderByOrderNoAsc(String name);

    /**
     * 활성 카테고리 조회 (특정 조건에 따라 필터링 가능)
     * @return 활성 카테고리 목록
     */
    @Query("SELECT c FROM Category c ORDER BY c.depth ASC, c.orderNo ASC")
    List<Category> findAllOrderByDepthAndOrderNo();

    /**
     * 전체 카테고리 목록(정렬 포함)
     */
    List<Category> findAllByOrderByOrderNoAsc();

    /**
     * parent_id != NULL → 소분류 카테고리만 조회
     */
    List<Category> findByParentIsNotNullOrderByOrderNoAsc();

    /**
     * 인트로 페이지 선택된 카테고리 6개를 제외한 나머지 카테고리 조회
     */
    @Query("""
        SELECT c 
        FROM Category c 
        WHERE c.parent IS NOT NULL 
          AND c.id NOT IN (
              SELECT uc.category.id 
              FROM UserCategory uc 
              WHERE uc.user.id = :userId
          )
        ORDER BY c.orderNo ASC
    """)
    List<Category> findSelectableChildCategories(@Param("userId") Long userId);

    //임시로 소분류 카테고리 6개 지정
    @Query("""
        SELECT c
        FROM Category c
        WHERE c.id IN (7, 12, 17, 21, 28, 31)
        ORDER BY c.orderNo ASC
    """)
    List<Category> findDefaultCategories();
}