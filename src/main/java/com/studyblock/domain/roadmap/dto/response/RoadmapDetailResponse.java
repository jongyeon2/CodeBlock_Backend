package com.studyblock.domain.roadmap.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record RoadmapDetailResponse(
        RoadmapJobResponse job,
        List<RoadmapNodeResponse> nodes,
        List<RoadmapEdgeResponse> edges
) {
    public static RoadmapDetailResponse of(RoadmapJobResponse job,
                                           List<RoadmapNodeResponse> nodes,
                                           List<RoadmapEdgeResponse> edges) {
        return RoadmapDetailResponse.builder()
                .job(job)
                .nodes(nodes)
                .edges(edges)
                .build();
    }
}
