package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.VideoResponse;
import com.studyblock.domain.course.dto.VideoStreamResponse;
import com.studyblock.domain.course.dto.VideoUploadResponse;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.service.VideoService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 비디오 관리 컨트롤러
 */
@Tag(name = "Video", description = "강의 비디오 관리 API - 비디오 업로드, 스트리밍, 자막 관리 등")
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    // VideoService만 의존 (Repository 직접 접근 제거)
    private final VideoService videoService;

    /**
     * 강의 비디오 업로드
     * POST /api/videos/upload
     *
     * 개선: try-catch 제거, Service에 위임
     */
    @Operation(
            summary = "비디오 업로드",
            description = "강의 비디오를 AWS S3에 업로드합니다. 썸네일은 선택사항입니다. " +
                    "targetResolution으로 인코딩 해상도를 지정할 수 있습니다 (1080p, 720p, 540p). 기본값은 1080p입니다."
    )
    @ApiResponse(responseCode = "201", description = "비디오 업로드 성공")
    @CommonApiResponses
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<VideoUploadResponse>> uploadVideo(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @RequestParam("lectureId") Long lectureId,

            @Parameter(description = "업로드할 비디오 파일 (mp4, avi, mov 등)", required = true)
            @RequestParam("videoFile") MultipartFile videoFile,

            @Parameter(description = "비디오 썸네일 이미지 (jpg, png 등)", required = false)
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,

            @AuthenticationPrincipal User currentUser) {

        // 비즈니스 로직은 Service에 위임
        VideoUploadResponse response = videoService.uploadVideo(lectureId, videoFile, thumbnailFile, currentUser);

        // Controller는 HTTP 상태 코드와 응답 형식만 결정
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("비디오 업로드 성공", response));
    }

    /**
     * 자막 파일 업로드
     * POST /api/videos/{videoId}/subtitle
     *
     */
    @Operation(
            summary = "자막 파일 업로드",
            description = "업로드된 비디오에 자막 파일을 추가합니다. (SRT, VTT 형식 지원)"
    )
    @ApiResponse(responseCode = "200", description = "자막 업로드 성공")
    @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
    @CommonApiResponses
    @PostMapping(value = "/{videoId}/subtitle", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Void>> uploadSubtitle(
            @Parameter(description = "비디오 ID", required = true, example = "1")
            @PathVariable Long videoId,

            @Parameter(description = "자막 파일 (srt, vtt 형식)", required = true)
            @RequestParam("subtitleFile") MultipartFile subtitleFile,

            @AuthenticationPrincipal User currentUser) {

        // Service에 위임
        videoService.uploadSubtitle(videoId, subtitleFile, currentUser);

        return ResponseEntity.ok(CommonResponse.success("자막 업로드 성공"));
    }

    /**
     * 비디오 스트리밍 URL 조회 (Presigned URL)
     * GET /api/videos/{videoId}/stream-url
     *
     * 개선: try-catch 제거, Service에 위임
     */
    @Operation(
            summary = "비디오 스트리밍 URL 조회",
            description = "비디오 재생을 위한 임시 스트리밍 URL(Presigned URL)을 생성합니다. URL은 60분간 유효합니다."
    )
    @ApiResponse(responseCode = "200", description = "스트리밍 URL 생성 성공")
    @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
    @CommonApiResponses
    @GetMapping("/{videoId}/stream-url")
    public ResponseEntity<CommonResponse<VideoStreamResponse>> getStreamUrl(
            @Parameter(description = "비디오 ID", required = true, example = "1")
            @PathVariable Long videoId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        // Service에서 처리
        VideoStreamResponse response = videoService.getStreamUrl(videoId, currentUser);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 특정 강의의 비디오 목록 조회 (페이징 지원)
     * GET /api/videos/lecture/{lectureId}
     *
     * 개선: Pageable 추가로 페이징 기능 제공
     * 요청 예시: /api/videos/lecture/1?page=0&size=10&sort=createdAt,desc
     */
    @Operation(
            summary = "강의별 비디오 목록 조회 (페이징)",
            description = "특정 강의에 속한 비디오 목록을 페이징하여 조회합니다. " +
                    "page, size, sort 파라미터로 페이징 설정 가능 (예: ?page=0&size=10&sort=createdAt,desc)"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CommonApiResponses
    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<CommonResponse<Page<VideoResponse>>> getVideosByLecture(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @PathVariable Long lectureId,

            @Parameter(description = "페이징 정보 (page, size, sort)", hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Service에서 페이징 처리
        Page<VideoResponse> response = videoService.getVideosByLecture(lectureId, pageable);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 특정 강의의 비디오 목록 조회 (전체 목록)
     * GET /api/videos/lecture/{lectureId}/all
     *
     * 개선: 페이징 없이 전체 목록이 필요한 경우를 위한 별도 엔드포인트
     */
    @Operation(
            summary = "강의별 비디오 전체 목록 조회",
            description = "특정 강의에 속한 모든 비디오 목록을 조회합니다 (페이징 없음)."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CommonApiResponses
    @GetMapping("/lecture/{lectureId}/all")
    public ResponseEntity<CommonResponse<List<VideoResponse>>> getAllVideosByLecture(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @PathVariable Long lectureId) {

        // Service에서 전체 목록 조회
        List<VideoResponse> response = videoService.getAllVideosByLecture(lectureId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 비디오 상세 조회
     * GET /api/videos/{videoId}
     *
     * 개선: try-catch 제거, Service에 위임
     */
    @Operation(
            summary = "비디오 상세 조회",
            description = "비디오의 상세 정보를 조회합니다. (제목, 설명, 재생시간, 인코딩 상태 등)"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
    @CommonApiResponses
    @GetMapping("/{videoId}")
    public ResponseEntity<CommonResponse<VideoResponse>> getVideo(
            @Parameter(description = "비디오 ID", required = true, example = "1")
            @PathVariable Long videoId) {

        // Service에서 조회
        VideoResponse response = videoService.getVideo(videoId);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 비디오 삭제
     * DELETE /api/videos/{videoId}
     *
     * 개선: try-catch 제거, Service에 위임
     */
    @Operation(
            summary = "비디오 삭제",
            description = "비디오를 삭제합니다. S3에 저장된 파일도 함께 삭제됩니다."
    )
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
    @CommonApiResponses
    @DeleteMapping("/{videoId}")
    public ResponseEntity<CommonResponse<Void>> deleteVideo(
            @Parameter(description = "비디오 ID", required = true, example = "1")
            @PathVariable Long videoId,

            @AuthenticationPrincipal User currentUser) {

        // Service에서 삭제 처리
        videoService.deleteVideo(videoId, currentUser);

        return ResponseEntity.ok(CommonResponse.success("비디오 삭제 성공"));
    }

    /**
     * 비디오 인코딩 상태 업데이트 (내부용/관리자용)
     * PATCH /api/videos/{videoId}/encoding-status
     *
     * 개선:
     * - 문자열 비교 제거 → EncodingStatus Enum 사용
     * - try-catch 제거 → GlobalExceptionHandler가 처리
     * - switch 로직은 Service로 이동
     */
    @Operation(
            summary = "인코딩 상태 업데이트 (관리자)",
            description = "비디오 인코딩 상태를 수동으로 업데이트합니다. (PENDING, PROCESSING, COMPLETED, FAILED)"
    )
    @ApiResponse(responseCode = "200", description = "상태 업데이트 성공")
    @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
    @CommonApiResponses
    @PatchMapping("/{videoId}/encoding-status")
    public ResponseEntity<CommonResponse<VideoResponse>> updateEncodingStatus(
            @Parameter(description = "비디오 ID", required = true, example = "1")
            @PathVariable Long videoId,

            @Parameter(description = "인코딩 상태", required = true,
                    schema = @Schema(allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED"}))
            @RequestParam EncodingStatus status,

            @AuthenticationPrincipal User currentUser) {

        // Service에서 Enum 기반 상태 업데이트 처리
        VideoResponse response = videoService.updateEncodingStatus(videoId, status, currentUser);

        return ResponseEntity.ok(CommonResponse.success("인코딩 상태 업데이트 성공", response));
    }

    /**
     * 비디오 기본 해상도 변경
     * PATCH /api/videos/{videoId}/resolution
     */
    @Operation(
            summary = "비디오 기본 해상도 변경",
            description = "비디오의 기본 재생 해상도를 변경합니다."
    )
    @ApiResponse(responseCode = "200", description = "해상도 변경 성공")
    @CommonApiResponses
    @PatchMapping("/{videoId}/resolution")
    public ResponseEntity<CommonResponse<Void>> updateDefaultResolution(
            @Parameter(description = "비디오 ID", required = true) @PathVariable Long videoId,
            @RequestBody com.studyblock.domain.course.dto.ResolutionUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        videoService.updateDefaultResolution(videoId, request.getResolution(), currentUser);
        return ResponseEntity.ok(CommonResponse.success("기본 해상도가 성공적으로 변경되었습니다."));
    }
}