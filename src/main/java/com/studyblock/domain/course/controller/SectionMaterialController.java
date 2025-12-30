package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.MaterialDeleteResponse;
import com.studyblock.domain.course.dto.MaterialResponse;
import com.studyblock.domain.course.dto.MaterialUpdateRequest;
import com.studyblock.domain.course.service.SectionMaterialService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 섹션 자료 관리 컨트롤러
 * 섹션 단위로 강의 자료를 업로드/수정/삭제하는 REST API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Section Material", description = "섹션 자료 관리 API - 섹션별 자료 업로드, 수정, 삭제")
public class SectionMaterialController {

    private final SectionMaterialService materialService;

    /**
     * 섹션 자료 목록 조회
     * GET /api/sections/{sectionId}/materials
     */
    @GetMapping("/sections/{sectionId}/materials")
    @Operation(
            summary = "섹션 자료 목록 조회",
            description = "섹션에 업로드된 자료 목록을 조회합니다. " +
                         "각 자료는 60분간 유효한 다운로드 URL을 포함합니다. " +
                         "강사 권한이 필요하며, 해당 섹션의 소유자만 조회할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사가 아니거나 섹션 소유자가 아님")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<MaterialResponse>>> getMaterialsBySection(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @PathVariable Long sectionId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        List<MaterialResponse> response = materialService.getMaterialsBySection(sectionId, currentUser);
        return ResponseEntity.ok(CommonResponse.success("섹션 자료 목록 조회 성공", response));
    }

    /**
     * 섹션 자료 업로드
     * POST /api/materials/upload
     */
    @PostMapping(value = "/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "섹션 자료 업로드",
            description = "섹션에 새로운 자료를 업로드합니다. " +
                         "파일 크기 제한: 100MB. " +
                         "title을 입력하지 않으면 파일명이 자동으로 사용됩니다. " +
                         "강사 권한이 필요하며, 해당 섹션의 소유자만 업로드할 수 있습니다."
    )
    @ApiResponse(responseCode = "201", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 - 파일 없음, 크기 초과, 유효하지 않은 파일")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사가 아니거나 섹션 소유자가 아님")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<MaterialResponse>> uploadMaterial(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @RequestParam("sectionId") Long sectionId,

            @Parameter(description = "업로드할 파일 (최대 100MB)", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "자료 제목 (선택, 미입력시 파일명 사용)", required = false)
            @RequestParam(value = "title", required = false) String title,

            @Parameter(description = "자료 설명 (선택)", required = false)
            @RequestParam(value = "description", required = false) String description,

            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        MaterialResponse response = materialService.uploadMaterial(
                sectionId, file, title, description, currentUser
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("자료 업로드 성공", response));
    }

    /**
     * 자료 메타데이터 수정
     * PUT /api/materials/{materialId}
     */
    @PutMapping("/materials/{materialId}")
    @Operation(
            summary = "자료 메타데이터 수정",
            description = "자료의 제목과 설명을 수정합니다. " +
                         "파일 자체는 수정되지 않으며, 메타데이터만 변경됩니다. " +
                         "강사 권한이 필요하며, 해당 섹션의 소유자만 수정할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 - 제목이 비어있음")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사가 아니거나 섹션 소유자가 아님")
    @ApiResponse(responseCode = "404", description = "자료를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<MaterialResponse>> updateMaterial(
            @Parameter(description = "자료 ID", required = true, example = "1")
            @PathVariable Long materialId,

            @Valid @RequestBody MaterialUpdateRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        MaterialResponse response = materialService.updateMaterial(materialId, request, currentUser);
        return ResponseEntity.ok(CommonResponse.success("자료 수정 성공", response));
    }

    /**
     * 자료 삭제
     * DELETE /api/materials/{materialId}
     */
    @DeleteMapping("/materials/{materialId}")
    @Operation(
            summary = "자료 삭제",
            description = "자료를 삭제합니다. " +
                         "S3에 저장된 파일도 함께 삭제됩니다. " +
                         "강사 권한이 필요하며, 해당 섹션의 소유자만 삭제할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사가 아니거나 섹션 소유자가 아님")
    @ApiResponse(responseCode = "404", description = "자료를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<MaterialDeleteResponse>> deleteMaterial(
            @Parameter(description = "자료 ID", required = true, example = "1")
            @PathVariable Long materialId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        MaterialDeleteResponse response = materialService.deleteMaterial(materialId, currentUser);
        return ResponseEntity.ok(CommonResponse.success("자료 삭제 성공", response));
    }
}