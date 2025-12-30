package com.studyblock.domain.roadmap.dto.response;

import com.studyblock.domain.roadmap.entity.RoadmapNode;
import lombok.Builder;

@Builder
public record RoadmapNodeResponse(
        String id,
        NodeData data,
        Position position,
        String type
) {
    @Builder
    public record NodeData(
            String label,
            String description,
            String categoryName,
            Integer level,
            Integer estimatedHours,
            String jobId
    ) {}

    @Builder
    public record Position(
            Integer x,
            Integer y
    ) {}

    public static RoadmapNodeResponse from(RoadmapNode node) {
        return RoadmapNodeResponse.builder()
                .id(node.getNodeId())
                .data(NodeData.builder()
                        .label(node.getLabel())
                        .description(node.getDescription())
                        .categoryName(node.getCategoryName())
                        .level(node.getLevel().intValue())
                        .estimatedHours(node.getEstimatedHours())
                        .jobId(node.getJobId())
                        .build())
                .position(Position.builder()
                        .x(node.getPositionX())
                        .y(node.getPositionY())
                        .build())
                .type(node.getNodeType())
                .build();
    }
}
