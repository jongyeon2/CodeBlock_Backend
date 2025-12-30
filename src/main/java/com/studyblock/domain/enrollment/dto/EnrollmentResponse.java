package com.studyblock.domain.enrollment.dto;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.enums.EnrollmentSource;
import com.studyblock.domain.enrollment.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 수강신청 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {
    private Long enrollmentId;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String courseThumbnailUrl;
    private String instructorName;
    private EnrollmentStatus status;
    private EnrollmentSource enrollmentSource;
    private BigDecimal progressPercentage;
    private Integer completedLecturesCount;
    private Integer totalLecturesCount;
    private Integer completedQuizzesCount;
    private Integer totalQuizzesCount;
    private LocalDateTime enrolledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private Boolean isRefundable;
    private String refundIneligibilityReason;
    @Builder.Default
    private List<Long> completedLectureIds = Collections.emptyList();
    @Builder.Default
    private List<Long> purchasedSectionIds = Collections.emptyList();
    @Builder.Default
    private Boolean hasFullCourseAccess = Boolean.TRUE;
    @Builder.Default
    private String snapshotType = "COURSE";

    public static EnrollmentResponse from(CourseEnrollment enrollment) {
        return from(enrollment, null, null);
    }

    public static EnrollmentResponse from(CourseEnrollment enrollment, List<Long> completedLectureIds) {
        return from(enrollment, completedLectureIds, null);
    }

    public static EnrollmentResponse from(CourseEnrollment enrollment, List<Long> completedLectureIds, List<Long> purchasedSectionIds) {
        Course course = enrollment.getCourse();
        String courseTitle = course != null ? course.getTitle() : null;
        String thumbnailUrl = course != null ? course.getThumbnailUrl() : null;
        String instructorName = course != null ? course.getInstructorName() : null;
        List<Long> lectureIds = completedLectureIds == null ? Collections.emptyList() : completedLectureIds;
        List<Long> sectionIds = purchasedSectionIds == null ? Collections.emptyList() : purchasedSectionIds;

        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .userId(enrollment.getUser().getId())
                .courseId(course != null ? course.getId() : null)
                .courseTitle(courseTitle)
                .courseThumbnailUrl(thumbnailUrl)
                .instructorName(instructorName)
                .status(enrollment.getStatus())
                .enrollmentSource(enrollment.getEnrollmentSource())
                .progressPercentage(enrollment.getProgressPercentage())
                .completedLecturesCount(enrollment.getCompletedLecturesCount())
                .totalLecturesCount(enrollment.getTotalLecturesCount())
                .completedQuizzesCount(enrollment.getCompletedQuizzesCount())
                .totalQuizzesCount(enrollment.getTotalQuizzesCount())
                .enrolledAt(enrollment.getEnrolledAt())
                .startedAt(enrollment.getStartedAt())
                .completedAt(enrollment.getCompletedAt())
                .lastAccessedAt(enrollment.getLastAccessedAt())
                .expiresAt(enrollment.getExpiresAt())
                .isRefundable(enrollment.isRefundable())
                .refundIneligibilityReason(enrollment.getRefundIneligibilityReason())
                .completedLectureIds(lectureIds)
                .purchasedSectionIds(sectionIds)
                .hasFullCourseAccess(true)
                .snapshotType("COURSE")
                .build();
    }

    public static EnrollmentResponse sectionSnapshot(
            Long userId,
            Course course,
            BigDecimal progressPercentage,
            Integer completedLecturesCount,
            Integer totalLecturesCount,
            List<Long> completedLectureIds,
            List<Long> purchasedSectionIds,
            LocalDateTime enrolledAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime lastAccessedAt
    ) {
        List<Long> lectureIds = completedLectureIds == null ? Collections.emptyList() : completedLectureIds;
        List<Long> sectionIds = purchasedSectionIds == null ? Collections.emptyList() : purchasedSectionIds;
        BigDecimal progress = progressPercentage == null ? BigDecimal.ZERO : progressPercentage.setScale(2, RoundingMode.HALF_UP);

        return EnrollmentResponse.builder()
                .enrollmentId(null)
                .userId(userId)
                .courseId(course != null ? course.getId() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .courseThumbnailUrl(course != null ? course.getThumbnailUrl() : null)
                .instructorName(course != null ? course.getInstructorName() : null)
                .status(null)
                .enrollmentSource(null)
                .progressPercentage(progress)
                .completedLecturesCount(completedLecturesCount)
                .totalLecturesCount(totalLecturesCount)
                .completedQuizzesCount(0)
                .totalQuizzesCount(0)
                .enrolledAt(enrolledAt)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .lastAccessedAt(lastAccessedAt)
                .expiresAt(null)
                .isRefundable(null)
                .refundIneligibilityReason(null)
                .completedLectureIds(lectureIds)
                .purchasedSectionIds(sectionIds)
                .hasFullCourseAccess(false)
                .snapshotType("SECTION")
                .build();
    }
}
