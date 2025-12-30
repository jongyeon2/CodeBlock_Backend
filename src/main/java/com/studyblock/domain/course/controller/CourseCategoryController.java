package com.studyblock.domain.course.controller;

import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.category.repository.CategoryRepository;
import com.studyblock.domain.course.dto.SearchCourseResponse;
import com.studyblock.domain.course.service.CourseCategoryService;
import com.studyblock.global.dto.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course/category")
@RequiredArgsConstructor
@Slf4j
public class CourseCategoryController {

    private final CategoryRepository categoryRepository;
    private final CourseCategoryService courseCategoryService;

    @GetMapping("/all")
    @Operation(summary = "전체 카테고리 목록 조회", description = "대분류 및 소분류를 포함한 전체 카테고리 트리 구조를 반환합니다.")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> getAllCategories() {
        log.info("전체 카테고리 목록 조회 요청");

        List<Category> allCategories = categoryRepository.findAllByOrderByOrderNoAsc();

        // 트리 구조로 변환
        List<Map<String, Object>> categoryTree = allCategories.stream()
                .filter(c -> c.getParent() == null) // 대분류만
                .map(parent -> Map.of(
                        "id", parent.getId(),
                        "name", parent.getName(),
                        "children", allCategories.stream()
                                .filter(child -> child.getParent() != null && child.getParent().getId().equals(parent.getId()))
                                .map(child -> Map.of(
                                        "id", child.getId(),
                                        "name", child.getName()
                                ))
                                .toList()
                ))
                .toList();

        return ResponseEntity.ok(
                CommonResponse.success("전체 카테고리 목록 조회 성공", categoryTree)
        );
    }

    /**
     * [카테고리별 강의 목록 조회]
     * ------------------------------------------------------------
     * - 프론트엔드에서 특정 카테고리(소분류)를 클릭했을 때 호출됨
     * - 해당 카테고리(categoryId)에 속한 강의 목록을 페이징으로 반환
     * - React Query + Infinite Scroll 과 연결됨
     * ------------------------------------------------------------
     *
     * @param categoryId 카테고리 ID (PathVariable)
     * @param page       페이지 번호 (0부터 시작)
     * @param size       페이지 크기 (한 번에 불러올 강의 개수)
     * @return CommonResponse<Page<SearchCourseResponse>>
     *         → data 필드에 Page 객체가 포함되어 있음
     *         → Page 안에는 content, totalPages, totalElements, last 등 기본 속성 존재
     */

    //프론트엔드에서 특정 카테고리(소분류)를 클릭 시 카테고리별 강의목록 추출
    //-React Query + Infinite Scroll(무한 스크롤)
    @GetMapping("/{categoryId}")
    public ResponseEntity<CommonResponse<Page<SearchCourseResponse>>> getCoursesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        log.info("[카테고리별 강의 조회 요청] categoryId={}, page={}, size={}", categoryId, page, size);

        // Service 계층 호출 (DB 접근은 서비스에서 수행)
        Page<SearchCourseResponse> result = courseCategoryService.getCoursesByCategory(categoryId, page, size);

        // 성공 응답 반환
        return ResponseEntity.ok(
                CommonResponse.success("카테고리별 강의 조회 성공", result)
        );
    }
}