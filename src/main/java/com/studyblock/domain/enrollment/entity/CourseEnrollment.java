package com.studyblock.domain.enrollment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.enrollment.enums.EnrollmentSource;
import com.studyblock.domain.enrollment.enums.EnrollmentStatus;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 수강신청 엔티티
 * Course-level enrollment tracking for full course purchases
 */
@Entity
@Table(name = "course_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_user_status", columnList = "user_id, status"),
                @Index(name = "idx_course_status", columnList = "course_id, status"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_enrollment_source", columnList = "enrollment_source"),
                @Index(name = "idx_expires_at", columnList = "expires_at"),
                @Index(name = "idx_last_accessed", columnList = "last_accessed_at"),
                @Index(name = "idx_order", columnList = "order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseEnrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === 관계 설정 ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // === 수강신청 상태 ===

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_source", nullable = false)
    private EnrollmentSource enrollmentSource;

    // === 진도율 추적 ===

    @Column(name = "progress_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercentage = BigDecimal.ZERO;

    @Column(name = "completed_lectures_count")
    @Builder.Default
    private Integer completedLecturesCount = 0;

    @Column(name = "total_lectures_count")
    @Builder.Default
    private Integer totalLecturesCount = 0;

    @Column(name = "completed_quizzes_count")
    @Builder.Default
    private Integer completedQuizzesCount = 0;

    @Column(name = "total_quizzes_count")
    @Builder.Default
    private Integer totalQuizzesCount = 0;

    // === 타이밍 ===

    @Column(name = "enrolled_at", nullable = false)
    @Builder.Default
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // === 조건부 환불 정책 (7일 + 콘텐츠 시청률 제한) ===

    @Column(name = "refundable_until")
    private LocalDateTime refundableUntil;

    @Column(name = "content_view_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal contentViewPercentage = BigDecimal.ZERO;

    @Column(name = "first_content_viewed_at")
    private LocalDateTime firstContentViewedAt;

    @Column(name = "refund_limit_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal refundLimitPercentage = new BigDecimal("10.00");

    // === 비즈니스 메서드 ===

    /**
     * 강좌 학습 시작 표시
     */
    public void markAsStarted() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        updateLastAccessed();
    }

    /**
     * 콘텐츠 시청 기록
     */
    public void markContentViewed() {
        if (this.firstContentViewedAt == null) {
            this.firstContentViewedAt = LocalDateTime.now();
        }
        updateLastAccessed();
    }

    /**
     * 마지막 접속 시간 업데이트
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 진도율 업데이트
     * @param newProgress 새로운 진도율 (0.00 ~ 100.00)
     */
    public void updateProgress(BigDecimal newProgress) {
        this.progressPercentage = newProgress;
        updateLastAccessed();

        // 100% 완료 시 자동으로 완료 상태로 변경
        if (newProgress.compareTo(new BigDecimal("100.00")) >= 0 && this.completedAt == null) {
            complete();
        }
    }

    /**
     * 강의 완료 카운트 업데이트
     */
    public void updateLectureCompletion(int completedCount, int totalCount) {
        this.completedLecturesCount = completedCount;
        this.totalLecturesCount = totalCount;
        recalculateProgress();
    }

    /**
     * 퀴즈 완료 카운트 업데이트
     */
    public void updateQuizCompletion(int completedCount, int totalCount) {
        this.completedQuizzesCount = completedCount;
        this.totalQuizzesCount = totalCount;
        recalculateProgress();
    }

    /**
     * 콘텐츠 시청률 업데이트
     */
    public void updateContentViewPercentage(BigDecimal viewPercentage) {
        this.contentViewPercentage = viewPercentage;
    }

    /**
     * 진도율 재계산 (강의 완료 + 퀴즈 통과 기반)
     */
    private void recalculateProgress() {
        if (totalLecturesCount == 0 && totalQuizzesCount == 0) {
            this.progressPercentage = BigDecimal.ZERO;
            return;
        }

        // 강의 완료 비율: 70% 가중치
        BigDecimal lectureProgress = totalLecturesCount > 0
                ? new BigDecimal(completedLecturesCount)
                .divide(new BigDecimal(totalLecturesCount), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("70.00"))
                : BigDecimal.ZERO;

        // 퀴즈 완료 비율: 30% 가중치
        BigDecimal quizProgress = totalQuizzesCount > 0
                ? new BigDecimal(completedQuizzesCount)
                .divide(new BigDecimal(totalQuizzesCount), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("30.00"))
                : BigDecimal.ZERO;

        // 퀴즈가 없는 경우 강의만으로 100% 계산
        if (totalQuizzesCount == 0) {
            lectureProgress = totalLecturesCount > 0
                    ? new BigDecimal(completedLecturesCount)
                    .divide(new BigDecimal(totalLecturesCount), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.00"))
                    : BigDecimal.ZERO;
        }

        this.progressPercentage = lectureProgress.add(quizProgress)
                .setScale(2, RoundingMode.HALF_UP);

        // 100% 완료 시 자동으로 완료 상태로 변경
        if (this.progressPercentage.compareTo(new BigDecimal("100.00")) >= 0 && this.completedAt == null) {
            complete();
        }
    }

    /**
     * 강좌 완료 처리
     */
    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = new BigDecimal("100.00");
        updateLastAccessed();
    }

    /**
     * 수강신청 취소 (환불 등)
     */
    public void revoke() {
        this.status = EnrollmentStatus.REVOKED;
    }

    /**
     * 수강신청 만료
     */
    public void expire() {
        this.status = EnrollmentStatus.EXPIRED;
    }

    // === 상태 확인 메서드 ===

    /**
     * 현재 강좌에 접근 가능한지 확인
     * @return 접근 가능 여부
     */
    public boolean isAccessible() {
        if (!status.isAccessible()) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * 환불 가능한지 확인 (조건부 환불 정책)
     * - 환불 기한 내
     * - 콘텐츠 시청률이 제한 이하
     * - 활성 상태
     * @return 환불 가능 여부
     */
    public boolean isRefundable() {
        if (status != EnrollmentStatus.ACTIVE) {
            return false;
        }
        if (refundableUntil == null || LocalDateTime.now().isAfter(refundableUntil)) {
            return false;
        }
        if (contentViewPercentage.compareTo(refundLimitPercentage) > 0) {
            return false;
        }
        return true;
    }

    /**
     * 콘텐츠를 시청했는지 확인
     * @return 시청 여부
     */
    public boolean hasViewedContent() {
        return firstContentViewedAt != null;
    }

    /**
     * 수강 중인지 확인 (완료되지 않음)
     * @return 수강 중 여부
     */
    public boolean isInProgress() {
        return status == EnrollmentStatus.ACTIVE;
    }

    /**
     * 완료되었는지 확인
     * @return 완료 여부
     */
    public boolean isCompleted() {
        return status == EnrollmentStatus.COMPLETED;
    }

    /**
     * 환불 불가 사유 반환
     * @return 환불 불가 사유 (환불 가능 시 null)
     */
    public String getRefundIneligibilityReason() {
        if (status != EnrollmentStatus.ACTIVE) {
            return "수강신청이 활성 상태가 아닙니다.";
        }
        if (refundableUntil == null || LocalDateTime.now().isAfter(refundableUntil)) {
            return "환불 기한이 지났습니다.";
        }
        if (contentViewPercentage.compareTo(refundLimitPercentage) > 0) {
            return String.format("콘텐츠 시청률(%.2f%%)이 환불 제한(%.2f%%)을 초과했습니다.",
                    contentViewPercentage, refundLimitPercentage);
        }
        return null;
    }
}
