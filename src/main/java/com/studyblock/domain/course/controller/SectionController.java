package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.LectureSummaryResponse;
import com.studyblock.domain.course.dto.ReorderSectionsRequest;
import com.studyblock.domain.course.dto.SectionRequest;
import com.studyblock.domain.course.dto.SectionResponse;
import com.studyblock.domain.course.service.SectionService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Section", description = "섹션 관련 API")
public class SectionController {

    private final SectionService sectionService;

    /**
     * 코스의 섹션 목록 조회 (사용자 권한별 필터링)
     * - 로그인 필수
     * - 전체 코스 수강자: 모든 섹션
     * - 섹션 단위 구매자: 구매한 섹션만
     * - 강사 본인: 모든 섹션
     * - 권한 없음: 빈 리스트
     */
    @GetMapping("/courses/{courseId}/sections")
    @Operation(
            summary = "코스의 섹션 목록 조회 (권한 필터링)",
            description = "사용자 권한에 따라 접근 가능한 섹션만 조회합니다. " +
                         "전체 코스 수강자는 모든 섹션, 섹션 단위 구매자는 구매한 섹션만 조회됩니다."
    )
    @ApiResponse(responseCode = "200", description = "섹션 목록 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<SectionResponse>>> getSectionsByCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        Long userId = currentUser != null ? currentUser.getId() : null;
        List<SectionResponse> response = sectionService.getSectionsByCourseIdForUser(courseId, userId);
        return ResponseEntity.ok(CommonResponse.success("섹션 목록 조회 성공", response));
    }

    /**
     * 섹션 상세 정보 조회 (강의 포함)
     */
    @GetMapping("/sections/{sectionId}")
    @Operation(
            summary = "섹션 상세 정보 조회",
            description = "섹션 ID로 해당 섹션의 상세 정보와 강의 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "섹션 상세 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<SectionResponse>> getSectionDetail(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @PathVariable Long sectionId) {

        SectionResponse response = sectionService.getSectionById(sectionId);
        return ResponseEntity.ok(CommonResponse.success("섹션 상세 정보 조회 성공", response));
    }

    /**
     * 섹션의 강의 목록 조회
     */
    @GetMapping("/sections/{sectionId}/lectures")
    @Operation(
            summary = "섹션의 강의 목록 조회",
            description = "섹션 ID로 해당 섹션에 속한 강의 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "강의 목록 조회 성공")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<LectureSummaryResponse>>> getLecturesBySection(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @PathVariable Long sectionId) {

        List<LectureSummaryResponse> response = sectionService.getLecturesBySectionId(sectionId);
        return ResponseEntity.ok(CommonResponse.success("강의 목록 조회 성공", response));
    }

    /**
     * 섹션 생성
     */
    @PostMapping("/courses/{courseId}/sections")
    @Operation(
            summary = "섹션 생성",
            description = "코스에 새로운 섹션을 추가합니다. 강사 권한 및 코스 소유자 확인이 필요합니다."
    )
    @ApiResponse(responseCode = "201", description = "섹션 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<SectionResponse>> createSection(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody SectionRequest request,
            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("섹션 생성 실패 - 인증되지 않은 사용자 - courseId: {}", courseId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        SectionResponse response = sectionService.createSection(courseId, request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("섹션이 성공적으로 생성되었습니다.", response));
    }

    /**
     * 섹션 수정
     */
    @PutMapping("/sections/{sectionId}")
    @Operation(
            summary = "섹션 수정",
            description = "기존 섹션의 정보를 수정합니다. 강사 권한 및 코스 소유자 확인이 필요합니다."
    )
    @ApiResponse(responseCode = "200", description = "섹션 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<SectionResponse>> updateSection(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionRequest request,
            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("섹션 수정 실패 - 인증되지 않은 사용자 - sectionId: {}", sectionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        SectionResponse response = sectionService.updateSection(sectionId, request, currentUser);
        return ResponseEntity.ok(CommonResponse.success("섹션이 성공적으로 수정되었습니다.", response));
    }

    /**
     * 섹션 삭제
     */
    @DeleteMapping("/sections/{sectionId}")
    @Operation(
            summary = "섹션 삭제",
            description = "기존 섹션을 삭제합니다. 강사 권한 및 코스 소유자 확인이 필요합니다."
    )
    @ApiResponse(responseCode = "200", description = "섹션 삭제 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Map<String, Boolean>>> deleteSection(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @PathVariable Long sectionId,
            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("섹션 삭제 실패 - 인증되지 않은 사용자 - sectionId: {}", sectionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        sectionService.deleteSection(sectionId, currentUser);

        Map<String, Boolean> result = new HashMap<>();
        result.put("success", true);

        return ResponseEntity.ok(CommonResponse.success("섹션이 성공적으로 삭제되었습니다.", result));
    }

    /**
     * 섹션 순서 변경
     */
    @PatchMapping("/courses/{courseId}/sections/reorder")
    @Operation(
            summary = "섹션 순서 변경",
            description = "코스의 섹션 순서를 재정렬합니다. 강사 권한 및 코스 소유자 확인이 필요합니다."
    )
    @ApiResponse(responseCode = "200", description = "섹션 순서 변경 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Map<String, Boolean>>> reorderSections(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody ReorderSectionsRequest request,
            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("섹션 순서 변경 실패 - 인증되지 않은 사용자 - courseId: {}", courseId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        sectionService.reorderSections(courseId, request.getOrderedSectionIds(), currentUser);

        Map<String, Boolean> result = new HashMap<>();
        result.put("success", true);

        return ResponseEntity.ok(CommonResponse.success("섹션 순서가 성공적으로 변경되었습니다.", result));
    }
}

