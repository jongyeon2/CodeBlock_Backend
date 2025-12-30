package com.studyblock.domain.category.service;

import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.category.entity.UserCategory;
import com.studyblock.domain.category.repository.CategoryRepository;
import com.studyblock.domain.category.repository.UserCategoryRepository;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 서비스
 * - 카테고리 관리
 * - 사용자 관심 카테고리 관리
 * - 카테고리 통계 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserCategoryRepository userCategoryRepository;

    /**
     * 사용자의 관심 카테고리 조회
     * @param userId 사용자 ID
     * @return 사용자의 관심 카테고리 목록
     */
    public List<UserCategory> getUserCategories(Long userId) {
        return userCategoryRepository.findByUserIdWithCategory(userId);
    }

    /**
     * 사용자 관심 카테고리 추가
     * @param user 사용자
     * @param categoryId 카테고리 ID
     * @return 추가된 UserCategory
     */
    @Transactional
    public UserCategory addUserCategory(User user, Long categoryId) {
        // 카테고리 존재 여부 확인
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        // 이미 등록된 관심 카테고리인지 확인
        Optional<UserCategory> existingUserCategory = 
                userCategoryRepository.findByUserIdAndCategoryId(user.getId(), categoryId);
        
        if (existingUserCategory.isPresent()) {
            throw new IllegalArgumentException("이미 관심 카테고리로 등록되어 있습니다.");
        }

        // 관심 카테고리 등록
        UserCategory userCategory = UserCategory.of(user, category);
        return userCategoryRepository.save(userCategory);
    }

    /**
     * 사용자 관심 카테고리 제거
     * @param userId 사용자 ID
     * @param categoryId 카테고리 ID
     */
    @Transactional
    public void removeUserCategory(Long userId, Long categoryId) {
        userCategoryRepository.deleteByUserIdAndCategoryId(userId, categoryId);
    }

    /**
     * 사용자의 모든 관심 카테고리 제거
     * @param userId 사용자 ID
     */
    @Transactional
    public void removeAllUserCategories(Long userId) {
        userCategoryRepository.deleteByUserId(userId);
    }

    /**
     * 사용자 관심 카테고리 일괄 업데이트
     * @param user 사용자
     * @param categoryIds 새로운 관심 카테고리 ID 목록
     */
    @Transactional
    public void updateUserCategories(User user, List<Long> categoryIds) {
        // 기존 관심 카테고리 모두 제거
        removeAllUserCategories(user.getId());

        // 새로운 관심 카테고리 등록
        for (Long categoryId : categoryIds) {
            try {
                addUserCategory(user, categoryId);
            } catch (Exception e) {
                log.warn("카테고리 {} 등록 실패: {}", categoryId, e.getMessage());
            }
        }
    }

    /**
     * 특정 카테고리를 관심으로 등록한 사용자 수 조회
     * @param categoryId 카테고리 ID
     * @return 해당 카테고리를 관심으로 등록한 사용자 수
     */
    public Long getCategoryUserCount(Long categoryId) {
        return userCategoryRepository.countByCategoryId(categoryId);
    }

    /**
     * 카테고리별 사용자 수 통계 조회
     * @param limit 조회할 개수
     * @return 카테고리별 사용자 수 통계
     */
    public List<Object[]> getCategoryStatistics(int limit) {
        return userCategoryRepository.findCategoryStatistics(limit);
    }

    /**
     * 사용자가 특정 카테고리를 관심으로 등록했는지 확인
     * @param userId 사용자 ID
     * @param categoryId 카테고리 ID
     * @return 등록 여부
     */
    public boolean isUserInterestedInCategory(Long userId, Long categoryId) {
        return userCategoryRepository.findByUserIdAndCategoryId(userId, categoryId).isPresent();
    }

    /**
     * 사용자의 관심 카테고리 ID 목록 조회
     * @param userId 사용자 ID
     * @return 관심 카테고리 ID 목록
     */
    public List<Long> getUserCategoryIds(Long userId) {
        return userCategoryRepository.findCategoryIdsByUserId(userId);
    }

    // ============================================
    // 카테고리 조회 API 메서드
    // ============================================

    /**
     * 모든 카테고리 조회
     * - QueryDSL을 사용한 효율적인 조회
     * - depth, orderNo 순으로 정렬
     * @return 모든 카테고리 목록
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAllCategoriesOrderByDepthAndOrderNo();
    }

    /**
     * 대분류 카테고리만 조회 (parent가 null인 카테고리)
     * - QueryDSL을 사용한 효율적인 조회
     * - orderNo 순으로 정렬
     * @return 대분류 카테고리 목록
     */
    public List<Category> getParentCategories() {
        return categoryRepository.findParentCategories();
    }

    /**
     * 특정 대분류의 소분류 카테고리 조회
     * - QueryDSL을 사용한 효율적인 조회
     * - orderNo 순으로 정렬
     * @param parentId 대분류 카테고리 ID
     * @return 소분류 카테고리 목록
     */
    public List<Category> getChildCategories(Long parentId) {
        // 부모 카테고리 존재 여부 확인
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        return categoryRepository.findChildCategoriesByParentId(parentId);
    }

    /**
     * 특정 카테고리 조회
     * @param categoryId 카테고리 ID
     * @return 카테고리 엔티티
     */
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    /**
     * parent_id != NULL 인 소분류 카테고리 전체 조회
     * - 모달창(카테고리 추가용)에서 사용됨
     */
    public List<Category> getAllChildCategories() {
        log.debug("소분류 카테고리 목록 조회");
        return categoryRepository.findByParentIsNotNullOrderByOrderNoAsc();
    }

    /**
     * 사용자가 아직 추가하지 않은 소분류 카테고리 조회
     * (비로그인 시 기본 6개만 반환)
     */
    public List<Category> getSelectableChildCategories(Long userId) {
        if (userId == null) {
            log.info("비로그인 유저: 기본 6개 카테고리만 반환");
            return categoryRepository.findDefaultCategories();
        }

        log.info("로그인 유저 (userId={}) - 추가하지 않은 카테고리 조회", userId);
        return categoryRepository.findSelectableChildCategories(userId);
    }
}


