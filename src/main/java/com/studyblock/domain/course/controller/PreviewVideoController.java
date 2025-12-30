package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.PreviewVideoResponse;
import com.studyblock.domain.course.dto.PreviewVideoStreamResponse;
import com.studyblock.domain.course.dto.PreviewVideoUploadResponse;
import com.studyblock.domain.course.dto.ResolutionUpdateRequest;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.service.PreviewVideoService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 맛보기 비디오 관리 컨트롤러
 * - 강의당 1:1로 관리되는 맛보기 비디오 CRUD
 * - VideoController의 구조를 재사용하되 PreviewVideo 특화 로직 적용
 */
@Tag(name = "Preview Video", description = "맛보기 비디오 관리 API - 맛보기 비디오 업로드, 스트리밍, 관리 등")
@RestController
@RequestMapping("/api/preview-videos")
@RequiredArgsConstructor
@Slf4j
public class PreviewVideoController {

    private final PreviewVideoService previewVideoService;

    /**
     * 맛보기 비디오 업로드
     * POST /api/preview-videos/upload
     */
    @Operation(
            summary = "맛보기 비디오 업로드",
            description = "강의당 하나의 맛보기 비디오를 AWS S3에 업로드합니다. " +
                    "이미 맛보기 비디오가 존재하는 경우 409 Conflict 에러를 반환합니다. " +
                    "썸네일은 선택사항입니다. targetResolution으로 인코딩 해상도를 지정할 수 있습니다 (1080p, 720p, 540p). 기본값은 1080p입니다."
    )
    @ApiResponse(responseCode = "201", description = "맛보기 비디오 업로드 성공")
    @ApiResponse(responseCode = "400", description = "필수 필드 누락 또는 파일 형식 오류")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "강사만 접근 가능, 강의 소유자가 아님")
    @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    @ApiResponse(responseCode = "409", description = "이미 해당 강의에 맛보기 비디오가 존재함")
    @ApiResponse(responseCode = "413", description = "파일 크기 500MB 초과")
    @CommonApiResponses
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<PreviewVideoUploadResponse>> uploadPreviewVideo(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @RequestParam("lectureId") Long lectureId,

            @Parameter(description = "업로드할 비디오 파일 (mp4, avi, mov 등)", required = true)
            @RequestParam("videoFile") MultipartFile videoFile,

            @Parameter(description = "비디오 썸네일 이미지 (jpg, png 등)", required = false)
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,

            @AuthenticationPrincipal User currentUser) {

        PreviewVideoUploadResponse response = previewVideoService.uploadPreviewVideo(
                lectureId, videoFile, thumbnailFile, currentUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("맛보기 비디오 업로드 성공", response));
    }

    /**
     * 강의별 맛보기 비디오 조회
     * GET /api/preview-videos/lecture/{lectureId}
     */
    @Operation(
            summary = "강의별 맛보기 비디오 조회",
            description = "특정 강의에 속한 맛보기 비디오를 조회합니다. 강의당 1:1 관계이므로 하나의 맛보기 비디오만 반환됩니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "맛보기 비디오를 찾을 수 없음")
    @CommonApiResponses
    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<CommonResponse<PreviewVideoResponse>> getPreviewVideoByLecture(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @PathVariable Long lectureId) {

        PreviewVideoResponse response = previewVideoService.getPreviewVideoByLecture(lectureId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 맛보기 비디오 상세 조회
     * GET /api/preview-videos/{previewVideoId}
     */
    @Operation(
            summary = "맛보기 비디오 상세 조회",
            description = "맛보기 비디오의 상세 정보를 조회합니다. (제목, 설명, 재생시간, 인코딩 상태 등)"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "맛보기 비디오를 찾을 수 없음")
    @CommonApiResponses
    @GetMapping("/{previewVideoId}")
    public ResponseEntity<CommonResponse<PreviewVideoResponse>> getPreviewVideo(
            @Parameter(description = "맛보기 비디오 ID", required = true, example = "1")
            @PathVariable Long previewVideoId) {

        PreviewVideoResponse response = previewVideoService.getPreviewVideo(previewVideoId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 맛보기 비디오 스트리밍 URL 조회 (Presigned URL)
     * GET /api/preview-videos/{previewVideoId}/stream-url
     */
    @Operation(
            summary = "맛보기 비디오 스트리밍 URL 조회",
            description = "맛보기 비디오 재생을 위한 임시 스트리밍 URL(Presigned URL)을 생성합니다. URL은 60분간 유효합니다."
    )
    @ApiResponse(responseCode = "200", description = "스트리밍 URL 생성 성공")
    @ApiResponse(responseCode = "404", description = "맛보기 비디오를 찾을 수 없음")
    @ApiResponse(responseCode = "400", description = "인코딩이 완료되지 않음 (설정에 따라 선택적)")
    @CommonApiResponses
    @GetMapping("/{previewVideoId}/stream-url")
    public ResponseEntity<CommonResponse<PreviewVideoStreamResponse>> getStreamUrl(
            @Parameter(description = "맛보기 비디오 ID", required = true, example = "1")
            @PathVariable Long previewVideoId) {

        PreviewVideoStreamResponse response = previewVideoService.getStreamUrl(previewVideoId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 맛보기 비디오 삭제
     * DELETE /api/preview-videos/{previewVideoId}
     */
    @Operation(
            summary = "맛보기 비디오 삭제",
            description = "맛보기 비디오를 삭제합니다. S3에 저장된 파일도 함께 삭제됩니다."
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "강사만 접근 가능, 강의 소유자가 아님")
    @ApiResponse(responseCode = "404", description = "맛보기 비디오를 찾을 수 없음")
    @CommonApiResponses
    @DeleteMapping("/{previewVideoId}")
    public ResponseEntity<CommonResponse<Void>> deletePreviewVideo(
            @Parameter(description = "맛보기 비디오 ID", required = true, example = "1")
            @PathVariable Long previewVideoId,

            @AuthenticationPrincipal User currentUser) {

        previewVideoService.deletePreviewVideo(previewVideoId, currentUser);
        return ResponseEntity.ok(CommonResponse.success("맛보기 비디오 삭제 성공"));
    }

    /**
     * 맛보기 비디오 인코딩 상태 업데이트 (내부용/관리자용)
     * PATCH /api/preview-videos/{previewVideoId}/encoding-status
     */
    @Operation(
            summary = "인코딩 상태 업데이트 (관리자)",
            description = "맛보기 비디오 인코딩 상태를 수동으로 업데이트합니다. (PENDING, PROCESSING, COMPLETED, FAILED)"
    )
    @ApiResponse(responseCode = "200", description = "상태 업데이트 성공")
    @ApiResponse(responseCode = "404", description = "맛보기 비디오를 찾을 수 없음")
    @CommonApiResponses
    @PatchMapping("/{previewVideoId}/encoding-status")
    public ResponseEntity<CommonResponse<PreviewVideoResponse>> updateEncodingStatus(
            @Parameter(description = "맛보기 비디오 ID", required = true, example = "1")
            @PathVariable Long previewVideoId,

            @Parameter(description = "인코딩 상태", required = true,
                    schema = @Schema(allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED"}))
            @RequestParam EncodingStatus status,

            @AuthenticationPrincipal User currentUser) {

        PreviewVideoResponse response = previewVideoService.updateEncodingStatus(
                previewVideoId, status, currentUser);

        return ResponseEntity.ok(CommonResponse.success("인코딩 상태 업데이트 성공", response));
    }

    /**
     * 맛보기 비디오 기본 해상도 변경
     * PATCH /api/preview-videos/{previewVideoId}/resolution
     */
    @Operation(
            summary = "맛보기 비디오 기본 해상도 변경",
            description = "맛보기 비디오의 기본 재생 해상도를 변경합니다."
    )
    @ApiResponse(responseCode = "200", description = "해상도 변경 성공")
    @CommonApiResponses
    @PatchMapping("/{previewVideoId}/resolution")
    public ResponseEntity<CommonResponse<Void>> updateDefaultResolution(
            @Parameter(description = "맛보기 비디오 ID", required = true) @PathVariable Long previewVideoId,
            @RequestBody ResolutionUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        previewVideoService.updateDefaultResolution(previewVideoId, request.getResolution(), currentUser);
        return ResponseEntity.ok(CommonResponse.success("기본 해상도가 성공적으로 변경되었습니다."));
    }
}

