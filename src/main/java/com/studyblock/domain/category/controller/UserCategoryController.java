package com.studyblock.domain.category.controller;

import com.studyblock.domain.category.dto.CategoryResponse;
import com.studyblock.domain.category.dto.UserCategoryAddRequest;
import com.studyblock.domain.category.dto.UserCategoryUpdateRequest;
import com.studyblock.domain.category.entity.UserCategory;
import com.studyblock.domain.category.service.CategoryService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-categories")
@Tag(name = "사용자 관심 카테고리", description = "사용자 관심 카테고리 관리 API")
public class UserCategoryController {

    private final CategoryService categoryService;

    
    // ============================================
    // 사용자 관심 카테고리 관리 엔드포인트
    // ============================================

    /**
     * 내 관심 카테고리 조회
     * - 현재 로그인한 사용자의 관심 카테고리 목록 반환
     */
    @GetMapping
    @Operation(
            summary = "내 관심 카테고리 조회",
            description = "현재 로그인한 사용자가 등록한 관심 카테고리 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "관심 카테고리 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> getMyUserCategories(
            @AuthenticationPrincipal User currentUser
    ) {
        // 로그인하지 않은 사용자
        if (currentUser == null) {
            log.warn("관심 카테고리 조회 실패: 인증되지 않은 사용자");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요한 서비스입니다."));
        }

        log.info("관심 카테고리 조회 요청: userId={}", currentUser.getId());

        List<UserCategory> userCategories = categoryService.getUserCategories(currentUser.getId());
        List<CategoryResponse> response = userCategories.stream()
                .map(uc -> CategoryResponse.from(uc.getCategory()))
                .collect(Collectors.toList());

        log.info("관심 카테고리 조회 완료: userId={}, {} 개", currentUser.getId(), response.size());
        return ResponseEntity.ok(CommonResponse.success("관심 카테고리 조회 성공", response));
    }

    /**
     * 관심 카테고리 추가
     * - 현재 로그인한 사용자의 관심 카테고리에 추가
     */
    @PostMapping
    @Operation(
            summary = "관심 카테고리 추가",
            description = "현재 로그인한 사용자의 관심 카테고리에 새로운 카테고리를 추가합니다."
    )
    @ApiResponse(responseCode = "201", description = "관심 카테고리 추가 성공")
    @ApiResponse(responseCode = "400", description = "이미 등록된 카테고리이거나 존재하지 않는 카테고리")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    public ResponseEntity<CommonResponse<CategoryResponse>> addUserCategory(
            @Valid @RequestBody UserCategoryAddRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        // 로그인하지 않은 사용자
        if (currentUser == null) {
            log.warn("관심 카테고리 추가 실패: 인증되지 않은 사용자");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요한 서비스입니다."));
        }

        log.info("관심 카테고리 추가 요청: userId={}, categoryId={}", currentUser.getId(), request.getCategoryId());

        UserCategory userCategory = categoryService.addUserCategory(currentUser, request.getCategoryId());
        CategoryResponse response = CategoryResponse.from(userCategory.getCategory());

        log.info("관심 카테고리 추가 완료: userId={}, categoryId={}", currentUser.getId(), request.getCategoryId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("관심 카테고리가 성공적으로 추가되었습니다.", response));
    }

    /**
     * 관심 카테고리 삭제
     * - 현재 로그인한 사용자의 관심 카테고리에서 제거
     */
    @DeleteMapping("/{categoryId}")
    @Operation(
            summary = "관심 카테고리 삭제",
            description = "현재 로그인한 사용자의 관심 카테고리에서 특정 카테고리를 삭제합니다."
    )
    @ApiResponse(responseCode = "200", description = "관심 카테고리 삭제 성공")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    public ResponseEntity<CommonResponse<Void>> removeUserCategory(
            @Parameter(description = "삭제할 카테고리 ID", required = true)
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User currentUser
    ) {
        // 로그인하지 않은 사용자
        if (currentUser == null) {
            log.warn("관심 카테고리 삭제 실패: 인증되지 않은 사용자");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요한 서비스입니다."));
        }

        log.info("관심 카테고리 삭제 요청: userId={}, categoryId={}", currentUser.getId(), categoryId);

        categoryService.removeUserCategory(currentUser.getId(), categoryId);

        log.info("관심 카테고리 삭제 완료: userId={}, categoryId={}", currentUser.getId(), categoryId);
        return ResponseEntity.ok(CommonResponse.success("관심 카테고리가 성공적으로 삭제되었습니다.", null));
    }

    /**
     * 관심 카테고리 일괄 업데이트
     * - 기존 관심 카테고리를 모두 삭제하고 새로운 목록으로 교체
     */
    @PutMapping
    @Operation(
            summary = "관심 카테고리 일괄 업데이트",
            description = "현재 로그인한 사용자의 관심 카테고리를 기존 목록을 모두 삭제하고 새로운 목록으로 교체합니다."
    )
    @ApiResponse(responseCode = "200", description = "관심 카테고리 일괄 업데이트 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> updateUserCategories(
            @Valid @RequestBody UserCategoryUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        // 로그인하지 않은 사용자
        if (currentUser == null) {
            log.warn("관심 카테고리 일괄 업데이트 실패: 인증되지 않은 사용자");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요한 서비스입니다."));
        }

        log.info("관심 카테고리 일괄 업데이트 요청: userId={}, categoryIds={}", currentUser.getId(), request.getCategoryIds());

        categoryService.updateUserCategories(currentUser, request.getCategoryIds());

        // 업데이트된 목록 조회
        List<UserCategory> userCategories = categoryService.getUserCategories(currentUser.getId());
        List<CategoryResponse> response = userCategories.stream()
                .map(uc -> CategoryResponse.from(uc.getCategory()))
                .collect(Collectors.toList());

        log.info("관심 카테고리 일괄 업데이트 완료: userId={}, {} 개", currentUser.getId(), response.size());
        return ResponseEntity.ok(CommonResponse.success("관심 카테고리가 성공적으로 업데이트되었습니다.", response));
    }
    
}
