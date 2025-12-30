package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.LectureResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 섹션 자료 응답 DTO
 * 프런트엔드 요청사항에 맞춘 응답 스키마
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {

    private Long id;
    private String title;
    private String description;
    private String fileName;
    private String fileExtension;
    private Long fileSize;
    private String downloadUrl;  // Presigned URL
    private Integer downloadCount;
    private LocalDateTime updatedAt;

    /**
     * LectureResource 엔티티를 MaterialResponse로 변환
     * @param resource LectureResource 엔티티
     * @param presignedUrl S3 Presigned URL
     * @return MaterialResponse
     */
    public static MaterialResponse from(LectureResource resource, String presignedUrl) {
        // 파일명과 확장자 추출
        String fileName = extractFileName(resource.getFileUrl());
        String fileExtension = extractFileExtension(fileName);

        return MaterialResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .fileName(fileName)
                .fileExtension(fileExtension)
                .fileSize(resource.getFileSize())
                .downloadUrl(presignedUrl)
                .downloadCount(resource.getDownloadCount())
                .updatedAt(resource.getUploadAt())  // uploadAt을 updatedAt으로 매핑
                .build();
    }

    /**
     * URL에서 파일명 추출
     * @param fileUrl S3 파일 URL
     * @return 파일명
     */
    private static String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        return fileUrl;
    }

    /**
     * 파일명에서 확장자 추출 (점 제외)
     * @param fileName 파일명
     * @return 확장자 (예: "zip", "pdf")
     */
    private static String extractFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
}