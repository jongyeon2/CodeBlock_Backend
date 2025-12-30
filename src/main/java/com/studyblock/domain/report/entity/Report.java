package com.studyblock.domain.report.entity;

import com.studyblock.domain.report.enums.ReportStatus;
import com.studyblock.domain.report.enums.ReportTargetType;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "report_reason")
    private String reportReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "reported_acted_at")
    private LocalDateTime reportedActedAt;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Builder
    public Report(User user, User reportedUser, ReportTargetType targetType, String reportReason, Long contentId) {
        this.user = user;
        this.reportedUser = reportedUser;
        this.targetType = targetType;
        this.reportReason = reportReason;
        this.contentId = contentId;
        this.status = ReportStatus.PENDING;
        this.reportedAt = LocalDateTime.now();
    }

    // Business methods
    public void startReview() {
        this.status = ReportStatus.REVIEWING;
    }

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.reportedActedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
        this.reportedActedAt = LocalDateTime.now();
    }
    //신고 접수
    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }
    //신고 처리 중
    public boolean isReviewing() {
        return this.status == ReportStatus.REVIEWING;
    }
    //신고 처리 완료
    public boolean isResolved() {
        return this.status == ReportStatus.RESOLVED;
    }
    //신고 거절
    public boolean isRejected() {
        return this.status == ReportStatus.REJECTED;
    }
}

