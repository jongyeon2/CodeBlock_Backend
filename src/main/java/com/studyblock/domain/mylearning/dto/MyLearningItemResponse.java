package com.studyblock.domain.mylearning.dto;

import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.entity.SectionEnrollment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 내 학습 통합 응답 DTO
 * CourseEnrollment(보유코스)와 LectureOwnership(보유강의)를 통합하여 표현
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyLearningItemResponse {

    /**
     * 아이템 타입: COURSE(전체 코스) 또는 SECTION(섹션 단위)
     */
    private ItemType itemType;

    /**
     * 아이템 ID (COURSE: courseId, SECTION: sectionId)
     */
    private Long itemId;

    /**
     * 아이템 제목 (COURSE: 코스명, SECTION: 섹션명)
     */
    private String itemTitle;

    /**
     * 썸네일 URL
     */
    private String thumbnailUrl;

    /**
     * 강사명
     */
    private String instructorName;

    /**
     * 진행률 (0-100, SECTION의 경우 진도 추적이 없으므로 0)
     */
    private BigDecimal progressPercentage;

    /**
     * 마지막 활동 날짜 (COURSE: lastAccessedAt, SECTION: createdAt)
     */
    private LocalDateTime lastActivityDate;

    /**
     * 소스 (PURCHASE, ADMIN_GRANT, COUPON 등)
     */
    private String source;

    /**
     * 코스 ID (모든 아이템의 부모 코스)
     */
    private Long courseId;

    /**
     * 코스 제목 (SECTION의 경우 부모 코스 정보)
     */
    private String courseTitle;

    /**
     * 섹션 ID (SECTION 타입인 경우에만)
     */
    private Long sectionId;

    /**
     * 섹션 제목 (SECTION 타입인 경우에만)
     */
    private String sectionTitle;

    /**
     * 완료 여부
     */
    private Boolean isCompleted;

    /**
     * 만료일 (있는 경우)
     */
    private LocalDateTime expiresAt;

    /**
     * 총 강의 수 (COURSE만 해당)
     */
    private Integer totalLecturesCount;

    /**
     * 완료한 강의 수 (COURSE만 해당)
     */
    private Integer completedLecturesCount;

    /**
     * CourseEnrollment로부터 변환
     */
    public static MyLearningItemResponse fromCourseEnrollment(CourseEnrollment enrollment) {
        return MyLearningItemResponse.builder()
                .itemType(ItemType.COURSE)
                .itemId(enrollment.getCourse().getId())
                .itemTitle(enrollment.getCourse().getTitle())
                .thumbnailUrl(enrollment.getCourse().getThumbnailUrl())
                .instructorName(enrollment.getCourse().getInstructorName())
                .progressPercentage(enrollment.getProgressPercentage())
                .lastActivityDate(enrollment.getLastAccessedAt() != null
                        ? enrollment.getLastAccessedAt()
                        : enrollment.getEnrolledAt())
                .source(enrollment.getEnrollmentSource().name())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .sectionId(null)
                .sectionTitle(null)
                .isCompleted(enrollment.isCompleted())
                .expiresAt(enrollment.getExpiresAt())
                .totalLecturesCount(enrollment.getTotalLecturesCount())
                .completedLecturesCount(enrollment.getCompletedLecturesCount())
                .build();
    }

    /**
     * LectureOwnership으로부터 변환
     */
    public static MyLearningItemResponse fromLectureOwnership(LectureOwnership ownership) {
        return MyLearningItemResponse.builder()
                .itemType(ItemType.SECTION)
                .itemId(ownership.getSection().getId())
                .itemTitle(ownership.getSection().getTitle())
                .thumbnailUrl(ownership.getSection().getCourse().getThumbnailUrl())
                .instructorName(ownership.getSection().getCourse().getInstructorName())
                .progressPercentage(BigDecimal.ZERO) // 섹션 단위는 진도 추적 없음
                .lastActivityDate(ownership.getCreatedAt())
                .source(ownership.getSource().name())
                .courseId(ownership.getSection().getCourse().getId())
                .courseTitle(ownership.getSection().getCourse().getTitle())
                .sectionId(ownership.getSection().getId())
                .sectionTitle(ownership.getSection().getTitle())
                .isCompleted(false) // 섹션 단위는 완료 개념 없음
                .expiresAt(ownership.getExpiresAt())
                .totalLecturesCount(null)
                .completedLecturesCount(null)
                .build();
    }

    /**
     * SectionEnrollment로부터 변환 (진도 정보 포함)
     */
    public static MyLearningItemResponse fromSectionEnrollment(SectionEnrollment enrollment) {
        return MyLearningItemResponse.builder()
                .itemType(ItemType.SECTION)
                .itemId(enrollment.getSection().getId())
                .itemTitle(enrollment.getSection().getTitle())
                .thumbnailUrl(enrollment.getCourse().getThumbnailUrl())
                .instructorName(enrollment.getCourse().getInstructorName())
                .progressPercentage(enrollment.getProgressPercentage())
                .lastActivityDate(enrollment.getLastAccessedAt() != null
                        ? enrollment.getLastAccessedAt()
                        : enrollment.getCreatedAt())
                .source("ENROLLMENT") // SectionEnrollment는 source 정보가 없으므로 기본값
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .sectionId(enrollment.getSection().getId())
                .sectionTitle(enrollment.getSection().getTitle())
                .isCompleted(enrollment.isCompleted())
                .expiresAt(null) // SectionEnrollment에는 만료일 개념이 없음
                .totalLecturesCount(enrollment.getTotalLecturesCount())
                .completedLecturesCount(enrollment.getCompletedLecturesCount())
                .build();
    }

    /**
     * 아이템 타입 열거형
     */
    public enum ItemType {
        COURSE,  // 전체 코스 수강
        SECTION  // 섹션 단위 구매
    }
}
