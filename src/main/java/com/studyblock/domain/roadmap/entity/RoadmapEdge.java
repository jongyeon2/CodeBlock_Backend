package com.studyblock.domain.roadmap.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "roadmap_edge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapEdge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "edge_id", unique = true, nullable = false, length = 50)
    private String edgeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_job_id")
    private RoadmapJob roadmapJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_node_id")
    private RoadmapNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_node_id")
    private RoadmapNode targetNode;

    @Column(name = "is_animated", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isAnimated;

    @Column(name = "edge_type", nullable = false, length = 50)
    private String edgeType;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive;

    @Builder
    private RoadmapEdge(String edgeId, RoadmapJob roadmapJob,
                       RoadmapNode sourceNode, RoadmapNode targetNode,
                       Boolean isAnimated, String edgeType, Boolean isActive) {
        this.edgeId = edgeId;
        this.roadmapJob = roadmapJob;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.isAnimated = isAnimated != null ? isAnimated : true;
        this.edgeType = edgeType != null ? edgeType : "smoothstep";
        this.isActive = isActive != null ? isActive : true;
    }

    public void updateEdge(RoadmapNode sourceNode, RoadmapNode targetNode,
                          Boolean isAnimated, String edgeType) {
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.isAnimated = isAnimated;
        this.edgeType = edgeType;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public String getSourceNodeId() {
        return sourceNode != null ? sourceNode.getNodeId() : null;
    }

    public String getTargetNodeId() {
        return targetNode != null ? targetNode.getNodeId() : null;
    }
}
