package com.studyblock.domain.roadmap.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.roadmap.enums.ProgressStatus;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_roadmap_progress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoadmapProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_node_id")
    private RoadmapNode roadmapNode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private UserRoadmapProgress(User user, RoadmapNode roadmapNode, ProgressStatus status) {
        this.user = user;
        this.roadmapNode = roadmapNode;
        this.status = status != null ? status : ProgressStatus.NOT_STARTED;
    }

    public void updateStatus(ProgressStatus status) {
        this.status = status;
        if (status == ProgressStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        } else if (status != ProgressStatus.COMPLETED) {
            this.completedAt = null;
        }
    }

    public void markAsCompleted() {
        this.status = ProgressStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsInProgress() {
        this.status = ProgressStatus.IN_PROGRESS;
        this.completedAt = null;
    }

    public void markAsNotStarted() {
        this.status = ProgressStatus.NOT_STARTED;
        this.completedAt = null;
    }

    public boolean isCompleted() {
        return this.status == ProgressStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return this.status == ProgressStatus.IN_PROGRESS;
    }

    public boolean isNotStarted() {
        return this.status == ProgressStatus.NOT_STARTED;
    }
}
