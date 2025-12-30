package com.studyblock.domain.upload.controller;

import com.studyblock.domain.upload.dto.ImageUploadResponse;
import com.studyblock.domain.upload.enums.ImageType;
import com.studyblock.domain.upload.service.ImageUploadService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 컨트롤러
 * - 썸네일, 프로필, 배너 등 다양한 이미지 업로드 지원
 * - S3 기반 이미지 저장 및 관리
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Upload", description = "이미지 업로드 관련 API")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    /**
     * 썸네일 이미지 업로드
     * - 코스, 강의 썸네일용
     * - 원본 + 썸네일 자동 생성
     */
    @PostMapping("/thumbnail")
    @Operation(
            summary = "썸네일 이미지 업로드",
            description = "코스나 강의의 썸네일 이미지를 업로드합니다. 원본과 썸네일이 자동으로 생성됩니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 형식, 크기 등)")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ImageUploadResponse>> uploadThumbnail(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "이미지 타입 (COURSE, LECTURE)", required = true)
            @RequestParam("type") String type) {

        log.info("썸네일 업로드 요청: filename={}, type={}", file.getOriginalFilename(), type);

        try {
            // 이미지 타입 검증
            ImageType imageType = ImageType.valueOf(type.toUpperCase());
            if (imageType != ImageType.COURSE && imageType != ImageType.LECTURE && imageType != ImageType.THUMBNAIL) {
                ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                        .success(false)
                        .message("썸네일은 COURSE 또는 LECTURE 타입만 가능합니다.")
                        .build();
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("INVALID_IMAGE_TYPE", errorResponse));
            }

            // 이미지 업로드
            ImageUploadResponse response = imageUploadService.uploadImage(file, imageType);

            if (response.getSuccess()) {
                return ResponseEntity.ok(CommonResponse.success("썸네일이 성공적으로 업로드되었습니다.", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("UPLOAD_FAILED", response));
            }

        } catch (IllegalArgumentException e) {
            log.warn("썸네일 업로드 실패: {}", e.getMessage());
                ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                        .success(false)
                        .message(e.getMessage())
                        .build();
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("INVALID_REQUEST", errorResponse));
        } catch (Exception e) {
            log.error("썸네일 업로드 중 오류 발생", e);
            ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                    .success(false)
                    .message("서버 오류가 발생했습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", errorResponse));
        }
    }

    /**
     * 프로필 이미지 업로드
     * - 사용자 프로필 이미지용
     * - 원본 + 썸네일 자동 생성
     */
    @PostMapping("/profile")
    @Operation(
            summary = "프로필 이미지 업로드",
            description = "사용자의 프로필 이미지를 업로드합니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ImageUploadResponse>> uploadProfile(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("프로필 이미지 업로드 요청: filename={}", file.getOriginalFilename());

        try {
            ImageUploadResponse response = imageUploadService.uploadImage(file, ImageType.PROFILE);

            if (response.getSuccess()) {
                return ResponseEntity.ok(CommonResponse.success("프로필 이미지가 성공적으로 업로드되었습니다.", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("UPLOAD_FAILED", response));
            }

        } catch (Exception e) {
            log.error("프로필 이미지 업로드 중 오류 발생", e);
            ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                    .success(false)
                    .message("서버 오류가 발생했습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", errorResponse));
        }
    }

    /**
     * 배너 이미지 업로드
     * - 메인 배너, 광고 배너용
     * - 원본 + 썸네일 자동 생성
     */
    @PostMapping("/banner")
    @Operation(
            summary = "배너 이미지 업로드",
            description = "배너 이미지를 업로드합니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ImageUploadResponse>> uploadBanner(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("배너 이미지 업로드 요청: filename={}", file.getOriginalFilename());

        try {
            ImageUploadResponse response = imageUploadService.uploadImage(file, ImageType.BANNER);

            if (response.getSuccess()) {
                return ResponseEntity.ok(CommonResponse.success("배너 이미지가 성공적으로 업로드되었습니다.", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("UPLOAD_FAILED", response));
            }

        } catch (Exception e) {
            log.error("배너 이미지 업로드 중 오류 발생", e);
            ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                    .success(false)
                    .message("서버 오류가 발생했습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", errorResponse));
        }
    }

    /**
     * 일반 이미지 업로드
     * - 다양한 용도의 이미지 업로드
     * - 원본 + 썸네일 자동 생성
     */
    @PostMapping("/image")
    @Operation(
            summary = "일반 이미지 업로드",
            description = "다양한 용도의 이미지를 업로드합니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ImageUploadResponse>> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "이미지 타입", required = true)
            @RequestParam("type") String type) {

        log.info("일반 이미지 업로드 요청: filename={}, type={}", file.getOriginalFilename(), type);

        try {
            // 이미지 타입 검증
            ImageType imageType = ImageType.valueOf(type.toUpperCase());

            ImageUploadResponse response = imageUploadService.uploadImage(file, imageType);

            if (response.getSuccess()) {
                return ResponseEntity.ok(CommonResponse.success("이미지가 성공적으로 업로드되었습니다.", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("UPLOAD_FAILED", response));
            }

        } catch (IllegalArgumentException e) {
            log.warn("이미지 업로드 실패: {}", e.getMessage());
                ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                        .success(false)
                        .message("유효하지 않은 이미지 타입입니다.")
                        .build();
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("INVALID_IMAGE_TYPE", errorResponse));
        } catch (Exception e) {
            log.error("이미지 업로드 중 오류 발생", e);
            ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                    .success(false)
                    .message("서버 오류가 발생했습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", errorResponse));
        }
    }

    /**
     * 썸네일만 업로드
     * - 빠른 썸네일 생성용
     * - 썸네일만 생성 (원본 저장 안함)
     */
    @PostMapping("/thumbnail-only")
    @Operation(
            summary = "썸네일만 업로드",
            description = "썸네일만 생성하여 업로드합니다. 원본 이미지는 저장되지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<ImageUploadResponse>> uploadThumbnailOnly(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "이미지 타입", required = true)
            @RequestParam("type") String type) {

        log.info("썸네일만 업로드 요청: filename={}, type={}", file.getOriginalFilename(), type);

        try {
            // 이미지 타입 검증
            ImageType imageType = ImageType.valueOf(type.toUpperCase());

            ImageUploadResponse response = imageUploadService.uploadThumbnailOnly(file, imageType);

            if (response.getSuccess()) {
                return ResponseEntity.ok(CommonResponse.success("썸네일이 성공적으로 업로드되었습니다.", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("UPLOAD_FAILED", response));
            }

        } catch (IllegalArgumentException e) {
            log.warn("썸네일 업로드 실패: {}", e.getMessage());
                ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                        .success(false)
                        .message("유효하지 않은 이미지 타입입니다.")
                        .build();
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error("INVALID_IMAGE_TYPE", errorResponse));
        } catch (Exception e) {
            log.error("썸네일 업로드 중 오류 발생", e);
            ImageUploadResponse errorResponse = ImageUploadResponse.builder()
                    .success(false)
                    .message("서버 오류가 발생했습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", errorResponse));
        }
    }
}
