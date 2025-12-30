package com.studyblock.domain.roadmap.dto.response;

import com.studyblock.domain.roadmap.entity.RoadmapEdge;
import lombok.Builder;

@Builder
public record RoadmapEdgeResponse(
        String id,
        String source,
        String target,
        Boolean animated
) {
    public static RoadmapEdgeResponse from(RoadmapEdge edge) {
        return RoadmapEdgeResponse.builder()
                .id(edge.getEdgeId())
                .source(edge.getSourceNodeId())
                .target(edge.getTargetNodeId())
                .animated(edge.getIsAnimated())
                .build();
    }
}
