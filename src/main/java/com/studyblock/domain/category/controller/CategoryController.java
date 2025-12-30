package com.studyblock.domain.category.controller;

import com.studyblock.domain.category.dto.CategoryResponse;
import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.category.service.CategoryService;
import com.studyblock.global.dto.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 컨트롤러
 * - 계층적 카테고리 조회 API 제공
 * - QueryDSL을 사용한 효율적인 조회
 * - 강의 등록 시 카테고리 선택 기능 지원
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
@Tag(name = "카테고리", description = "카테고리 조회 API")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 모든 카테고리 조회
     * - depth, orderNo 순으로 정렬
     * - 대분류 먼저, 그 다음 소분류 순서로 반환
     *
     * @return 모든 카테고리 목록
     */
    @Operation(summary = "모든 카테고리 조회", description = "모든 카테고리를 depth, orderNo 순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("모든 카테고리 조회 요청");

        List<Category> categories = categoryService.getAllCategories();
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());

        log.info("모든 카테고리 조회 완료: {} 개", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 대분류 카테고리만 조회
     * - parent_id가 NULL인 카테고리
     * - orderNo 순으로 정렬
     *
     * @return 대분류 카테고리 목록
     */
    @Operation(summary = "대분류 카테고리 조회", description = "최상위 대분류 카테고리만 조회합니다.")
    @GetMapping("/parents")
    public ResponseEntity<List<CategoryResponse>> getParentCategories() {
        log.info("대분류 카테고리 조회 요청");

        List<Category> categories = categoryService.getParentCategories();
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());

        log.info("대분류 카테고리 조회 완료: {} 개", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 대분류의 소분류 카테고리 조회
     * - parentId에 해당하는 자식 카테고리들 조회
     * - orderNo 순으로 정렬
     *
     * @param parentId 대분류 카테고리 ID
     * @return 소분류 카테고리 목록
     */
    @Operation(summary = "소분류 카테고리 조회", description = "특정 대분류의 소분류 카테고리를 조회합니다.")
    @GetMapping("/children/{parentId}")
    public ResponseEntity<List<CategoryResponse>> getChildCategories(
            @Parameter(description = "대분류 카테고리 ID", required = true)
            @PathVariable Long parentId
    ) {
        log.info("소분류 카테고리 조회 요청: parentId={}", parentId);

        List<Category> categories = categoryService.getChildCategories(parentId);
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());

        log.info("소분류 카테고리 조회 완료: parentId={}, {} 개", parentId, response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 카테고리 조회
     * - ID로 단일 카테고리 조회
     *
     * @param categoryId 카테고리 ID
     * @return 카테고리 상세 정보
     */
    @Operation(summary = "특정 카테고리 조회", description = "카테고리 ID로 단일 카테고리를 조회합니다.")
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "카테고리 ID", required = true)
            @PathVariable Long categoryId
    ) {
        log.info("카테고리 조회 요청: categoryId={}", categoryId);

        Category category = categoryService.getCategoryById(categoryId);
        CategoryResponse response = CategoryResponse.from(category);

        log.info("카테고리 조회 완료: categoryId={}, name={}", categoryId, category.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * [인트로 메인페이지 전용]
     * ---------------------------------------------------------
     * - parent_id가 존재하는 '소분류' 카테고리만 반환
     * - 메인페이지 진입 시 기본 6개 + 추가 모달용 전체 목록
     * ---------------------------------------------------------
     */
    @GetMapping("/children")
    @Operation(summary = "소분류 카테고리 전체 조회", description = "인트로 페이지에서 사용됨. parent_id가 존재하는 모든 소분류 카테고리를 반환합니다.")
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> getAllChildCategories() {
        log.info("[인트로페이지] 소분류 카테고리 조회 요청");

        List<Category> childCategories = categoryService.getAllChildCategories();
        List<CategoryResponse> response = childCategories.stream()
                .map(CategoryResponse::from)
                .toList();

        log.info("소분류 카테고리 {}개 반환 완료", response.size());
        return ResponseEntity.ok(CommonResponse.success("소분류 카테고리 조회 성공", response));
    }

    /**
     * [인트로 메인페이지 전용]
     * ---------------------------------------------------------
     * - 카테고리 추가 시 띄워지는 모달창에 사용자가 추가하지 않은 카테고리 조회
     * ---------------------------------------------------------
     */
    @GetMapping("/children/selectable")
    @Operation(
            summary = "선택 가능한 소분류 카테고리 조회",
            description = "비로그인 시 기본 6개 카테고리, 로그인 시 추가하지 않은 카테고리 반환"
    )
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> getSelectableChildCategories(
            @RequestParam(required = false) Long userId
    ) {
        List<Category> categories = categoryService.getSelectableChildCategories(userId);
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(CommonResponse.success("소분류 카테고리 조회 성공", response));
    }
}
