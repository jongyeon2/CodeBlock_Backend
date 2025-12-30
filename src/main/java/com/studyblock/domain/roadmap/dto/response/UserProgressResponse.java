package com.studyblock.domain.roadmap.dto.response;

import com.studyblock.domain.roadmap.entity.UserRoadmapProgress;
import com.studyblock.domain.roadmap.enums.ProgressStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserProgressResponse(
        Long id,
        String nodeId,
        String nodeName,
        ProgressStatus status,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {
    public static UserProgressResponse from(UserRoadmapProgress progress) {
        return UserProgressResponse.builder()
                .id(progress.getId())
                .nodeId(progress.getRoadmapNode().getNodeId())
                .nodeName(progress.getRoadmapNode().getLabel())
                .status(progress.getStatus())
                .completedAt(progress.getCompletedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}
