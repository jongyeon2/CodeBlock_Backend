package com.studyblock.domain.category.repository;

import com.studyblock.domain.category.entity.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자-카테고리 매핑 Repository
 * - 사용자의 관심 카테고리 관리
 * - 카테고리별 사용자 통계 조회
 */
public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {

    /**
     * 특정 사용자의 관심 카테고리 조회
     * @param userId 사용자 ID
     * @return 사용자의 관심 카테고리 목록
     */
    @Query("SELECT uc FROM UserCategory uc " +
           "JOIN FETCH uc.category c " +
           "WHERE uc.user.id = :userId " +
           "ORDER BY c.orderNo ASC")
    List<UserCategory> findByUserIdWithCategory(@Param("userId") Long userId);

    /**
     * 특정 카테고리를 관심으로 등록한 사용자 수 조회
     * @param categoryId 카테고리 ID
     * @return 해당 카테고리를 관심으로 등록한 사용자 수
     */
    @Query("SELECT COUNT(uc) FROM UserCategory uc WHERE uc.category.id = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 특정 사용자가 특정 카테고리를 관심으로 등록했는지 확인
     * @param userId 사용자 ID
     * @param categoryId 카테고리 ID
     * @return UserCategory 존재 여부
     */
    Optional<UserCategory> findByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * 특정 사용자의 관심 카테고리 삭제
     * @param userId 사용자 ID
     * @param categoryId 카테고리 ID
     */
    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * 특정 사용자의 모든 관심 카테고리 삭제
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 카테고리별 사용자 수 통계 (상위 N개)
     * @param limit 조회할 개수
     * @return 카테고리별 사용자 수 목록
     */
    @Query("SELECT c.id, c.name, COUNT(uc) as userCount " +
           "FROM Category c " +
           "LEFT JOIN UserCategory uc ON c.id = uc.category.id " +
           "GROUP BY c.id, c.name " +
           "ORDER BY userCount DESC")
    List<Object[]> findCategoryStatistics(@Param("limit") int limit);

    /**
     * 특정 사용자가 관심 카테고리로 등록한 카테고리 ID 목록 조회
     * @param userId 사용자 ID
     * @return 관심 카테고리 ID 목록
     */
    @Query("SELECT uc.category.id FROM UserCategory uc WHERE uc.user.id = :userId")
    List<Long> findCategoryIdsByUserId(@Param("userId") Long userId);
}
