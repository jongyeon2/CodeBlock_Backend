package com.studyblock.domain.course.entity;

import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비디오 시청 진도율 엔티티
 * - 사용자별 비디오 시청 기록 저장
 * - Redis 캐싱 + MySQL 영구 저장 하이브리드 방식
 */
@Entity
@Table(
        name = "video_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_video",
                columnNames = {"user_id", "video_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "last_position", nullable = false)
    private Integer lastPosition = 0;  // 마지막 시청 시점 (초)

    @Column(name = "duration")
    private Integer duration;  // 전체 영상 길이 (초)

    @Column(name = "is_completed", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isCompleted = false;  // 완료 여부

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder
    public VideoProgress(User user, Video video, Integer lastPosition,
                         Integer duration, Boolean isCompleted) {
        this.user = user;
        this.video = video;
        this.lastPosition = lastPosition != null ? lastPosition : 0;
        this.duration = duration;
        this.isCompleted = isCompleted != null ? isCompleted : false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 시청 위치 업데이트
     * - 90% 이상 시청 시 자동으로 완료 처리
     *
     * @param position 시청 위치 (초)
     */
    public void updatePosition(Integer position) {
        this.lastPosition = position;
        this.updatedAt = LocalDateTime.now();

        // 90% 이상 시청 시 자동 완료
        if (this.duration != null && this.duration > 0) {
            double progressPercent = (position * 100.0) / this.duration;
            if (progressPercent >= 90.0) {
                this.isCompleted = true;
            }
        }
    }

    public void updateDuration(Integer duration) {
        if (duration != null && duration > 0) {
            this.duration = duration;
        }
    }

    /**
     * 시청 완료 처리
     */
    public void complete() {
        this.isCompleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 진도율 계산 (퍼센트)
     *
     * @return 진도율 (0~100)
     */
    public Double getProgressPercent() {
        if (duration == null || duration == 0) {
            return 0.0;
        }
        return Math.min((lastPosition * 100.0) / duration, 100.0);
    }
}