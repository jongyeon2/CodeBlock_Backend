package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.enums.LectureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 코스 상세 화면용 강의 요약 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class LectureSummaryResponse {

    private Long id;
    private Long sectionId;
    private Integer sequence;
    private String title;
    private String description;
    private String thumbnailUrl;            // presigned URL (노출용)
    private String thumbnailOriginalUrl;    // 원본 S3 URL
    private LocalDate uploadDate;
    private LectureStatus status;
    private Boolean isFree;
    private Long priceCookie;
    private Integer discountPercentage;

    // 비디오 관련 정보 추가
    private Long lastVideoId;
    private String lastVideoEncodingStatus;
    private Long videoCount;
    
    // 미리보기 영상 존재 여부
    private Boolean hasPreviewVideo;
    
    // 사용자 접근성 정보
    private Boolean hasAccess;
    private Boolean locked;

    /**
     * Entity -> DTO 변환 (비디오 정보 없이)
     */
    public static LectureSummaryResponse from(Lecture lecture) {
        return LectureSummaryResponse.builder()
                .id(lecture.getId())
                .sectionId(lecture.getSection() != null ? lecture.getSection().getId() : null)
                .sequence(lecture.getSequence())
                .title(lecture.getTitle())
                .description(lecture.getDescription())
                .thumbnailUrl(lecture.getThumbnailUrl())
                .thumbnailOriginalUrl(lecture.getThumbnailUrl())
                .uploadDate(lecture.getUploadDate())
                .status(lecture.getStatus())
                .isFree(lecture.getIsFree())
                .priceCookie(lecture.getSection() != null ? lecture.getSection().getCookiePrice() : null)
                .discountPercentage(lecture.getSection() != null ? lecture.getSection().getDiscountPercentage() : null)
                .lastVideoId(null)
                .lastVideoEncodingStatus(null)
                .videoCount(0L)
                .hasPreviewVideo(lecture.hasPreviewVideo())
                .build();
    }

    /**
     * Entity -> DTO 변환 (비디오 정보 포함)
     */
    public static LectureSummaryResponse of(Lecture lecture, Long lastVideoId, EncodingStatus lastVideoEncodingStatus, Long videoCount) {
        return LectureSummaryResponse.builder()
                .id(lecture.getId())
                .sectionId(lecture.getSection() != null ? lecture.getSection().getId() : null)
                .sequence(lecture.getSequence())
                .title(lecture.getTitle())
                .description(lecture.getDescription())
                .thumbnailUrl(lecture.getThumbnailUrl())
                .thumbnailOriginalUrl(lecture.getThumbnailUrl())
                .uploadDate(lecture.getUploadDate())
                .status(lecture.getStatus())
                .isFree(lecture.getIsFree())
                .priceCookie(lecture.getSection() != null ? lecture.getSection().getCookiePrice() : null)
                .discountPercentage(lecture.getSection() != null ? lecture.getSection().getDiscountPercentage() : null)
                .lastVideoId(lastVideoId)
                .lastVideoEncodingStatus(lastVideoEncodingStatus != null ? lastVideoEncodingStatus.name() : null)
                .videoCount(videoCount)
                .hasPreviewVideo(lecture.hasPreviewVideo())
                .build();
    }
}
