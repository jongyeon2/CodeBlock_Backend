package com.studyblock.domain.enrollment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.enrollment.enums.CompletionType;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 강의 완료 엔티티
 * Tracks individual lecture completion for progress calculation
 */
@Entity
@Table(name = "lecture_completion",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "lecture_id"})
        },
        indexes = {
                @Index(name = "idx_user_completed", columnList = "user_id, is_completed"),
                @Index(name = "idx_lecture_completed", columnList = "lecture_id, is_completed"),
                @Index(name = "idx_enrollment", columnList = "course_enrollment_id"),
                @Index(name = "idx_completion_type", columnList = "completion_type"),
                @Index(name = "idx_completed_at", columnList = "completed_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LectureCompletion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === 관계 설정 ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_enrollment_id")
    private CourseEnrollment courseEnrollment;

    // === 완료 상태 ===

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_type", nullable = false)
    @Builder.Default
    private CompletionType completionType = CompletionType.VIDEO_WATCHED;

    // === 타이밍 ===

    @Column(name = "first_viewed_at")
    private LocalDateTime firstViewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    // === 진도 상세 ===

    @Column(name = "total_time_spent_seconds")
    @Builder.Default
    private Integer totalTimeSpentSeconds = 0;

    @Column(name = "video_watch_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal videoWatchPercentage = BigDecimal.ZERO;

    @Column(name = "quiz_score", precision = 5, scale = 2)
    private BigDecimal quizScore;

    @Column(name = "quiz_attempts")
    @Builder.Default
    private Integer quizAttempts = 0;

    // === 비즈니스 메서드 ===

    /**
     * 첫 시청 기록
     */
    public void markFirstView() {
        if (this.firstViewedAt == null) {
            this.firstViewedAt = LocalDateTime.now();
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
     * 비디오 시청 진도 업데이트
     * @param watchPercentage 시청률 (0.00 ~ 100.00)
     * @param timeSpentSeconds 시청 시간 (초)
     */
    public void updateVideoProgress(BigDecimal watchPercentage, int timeSpentSeconds) {
        this.videoWatchPercentage = watchPercentage;
        this.totalTimeSpentSeconds = timeSpentSeconds;
        updateLastAccessed();

        // 90% 이상 시청 시 자동으로 완료 처리
        if (watchPercentage.compareTo(new BigDecimal("90.00")) >= 0 && !this.isCompleted) {
            markAsCompleted(CompletionType.VIDEO_WATCHED);
        }
    }

    /**
     * 퀴즈 점수 업데이트
     * @param score 퀴즈 점수
     * @param passed 합격 여부
     */
    public void updateQuizScore(BigDecimal score, boolean passed) {
        this.quizScore = score;
        this.quizAttempts++;
        updateLastAccessed();

        // 퀴즈 통과 시 강의 완료 처리
        if (passed && !this.isCompleted) {
            markAsCompleted(CompletionType.QUIZ_PASSED);
        }
    }

    /**
     * 강의 완료 표시
     * @param type 완료 타입
     */
    public void markAsCompleted(CompletionType type) {
        this.isCompleted = true;
        this.completionType = type;
        this.completedAt = LocalDateTime.now();
        updateLastAccessed();
    }

    /**
     * 수동으로 완료 표시
     */
    public void markAsCompletedManually() {
        markAsCompleted(CompletionType.MANUAL);
    }

    /**
     * 완료 취소
     */
    public void unmarkCompletion() {
        this.isCompleted = false;
        this.completedAt = null;
    }

    // === 상태 확인 메서드 ===

    /**
     * 완료 여부 확인
     * @return 완료 여부
     */
    public boolean isCompleted() {
        return Boolean.TRUE.equals(isCompleted);
    }

    /**
     * 시청 시작 여부 확인
     * @return 시청 시작 여부
     */
    public boolean hasStarted() {
        return firstViewedAt != null;
    }

    /**
     * 퀴즈 시도 여부 확인
     * @return 퀴즈 시도 여부
     */
    public boolean hasAttemptedQuiz() {
        return quizAttempts > 0;
    }

    /**
     * 비디오 시청으로 완료되었는지 확인
     * @return 비디오 시청 완료 여부
     */
    public boolean isCompletedByVideo() {
        return isCompleted && completionType == CompletionType.VIDEO_WATCHED;
    }

    /**
     * 퀴즈 통과로 완료되었는지 확인
     * @return 퀴즈 통과 완료 여부
     */
    public boolean isCompletedByQuiz() {
        return isCompleted && completionType == CompletionType.QUIZ_PASSED;
    }
}
