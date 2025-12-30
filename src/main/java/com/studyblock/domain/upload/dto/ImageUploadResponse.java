package com.studyblock.domain.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    /**
     * 업로드된 이미지의 S3 URL
     */
    private String url;

    /**
     * 원본 파일명
     */
    private String originalFilename;

    /**
     * S3에 저장된 파일명 (UUID 포함)
     */
    private String filename;

    /**
     * 파일 크기 (바이트)
     */
    private Long size;

    /**
     * MIME 타입
     */
    private String mimeType;

    /**
     * 이미지 타입 (썸네일, 프로필 등)
     */
    private String imageType;

    /**
     * 썸네일 URL (썸네일 생성 시)
     */
    private String thumbnailUrl;

    /**
     * 이미지 너비 (픽셀)
     */
    private Integer width;

    /**
     * 이미지 높이 (픽셀)
     */
    private Integer height;

    /**
     * 업로드 성공 여부
     */
    private Boolean success;

    /**
     * 응답 메시지
     */
    private String message;
}











