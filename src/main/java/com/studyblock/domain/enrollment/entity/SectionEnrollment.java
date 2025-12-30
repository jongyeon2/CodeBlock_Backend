package com.studyblock.domain.enrollment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 섹션 단위 수강 현황
 * 섹션 구매 사용자의 진행률을 코스 전체 기준으로 추적한다.
 */
@Getter
@Entity
@Table(
        name = "section_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_section_enrollment_user_section", columnNames = {"user_id", "section_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
public class SectionEnrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "progress_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercentage = BigDecimal.ZERO;

    @Column(name = "completed_lectures_count")
    @Builder.Default
    private Integer completedLecturesCount = 0;

    @Column(name = "total_lectures_count")
    @Builder.Default
    private Integer totalLecturesCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public void markAsStarted() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        updateLastAccessed();
    }

    public void markAsCompleted() {
        completedAt = LocalDateTime.now();
        progressPercentage = new BigDecimal("100.00");
        updateLastAccessed();
    }

    public void updateLectureCompletion(int completedCount, int totalCount) {
        this.completedLecturesCount = Math.max(completedCount, 0);
        this.totalLecturesCount = Math.max(totalCount, 0);
        recalculateProgress();

        if (progressPercentage.compareTo(new BigDecimal("100.00")) >= 0 && completedAt == null) {
            markAsCompleted();
        } else if (completedCount > 0) {
            markAsStarted();
        }
    }

    public void updateProgress(BigDecimal newProgress) {
        if (newProgress == null) {
            return;
        }

        BigDecimal clamped = newProgress.max(BigDecimal.ZERO).min(new BigDecimal("100.00"));
        this.progressPercentage = clamped.setScale(2, RoundingMode.HALF_UP);

        if (clamped.compareTo(BigDecimal.ZERO) > 0) {
            markAsStarted();
        }

        if (clamped.compareTo(new BigDecimal("100.00")) >= 0 && completedAt == null) {
            markAsCompleted();
        } else {
            updateLastAccessed();
        }
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    private void recalculateProgress() {
        if (totalLecturesCount == 0) {
            this.progressPercentage = BigDecimal.ZERO;
            return;
        }

        BigDecimal completed = new BigDecimal(this.completedLecturesCount);
        BigDecimal total = new BigDecimal(this.totalLecturesCount);

        BigDecimal progress = completed
                .divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100.00"))
                .setScale(2, RoundingMode.HALF_UP);

        this.progressPercentage = progress;
        updateLastAccessed();
    }

    private void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
