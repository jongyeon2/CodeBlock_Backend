package com.studyblock.domain.roadmap.entity;

import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "roadmap_node")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapNode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", unique = true, nullable = false, length = 50)
    private String nodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_job_id")
    private RoadmapJob roadmapJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Byte level;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Column(name = "node_type", nullable = false, length = 50)
    private String nodeType;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive;

    @Builder
    private RoadmapNode(String nodeId, RoadmapJob roadmapJob, Category category,
                       String label, String description, Integer level,
                       Integer estimatedHours, Integer positionX, Integer positionY,
                       String nodeType, Boolean isActive) {
        this.nodeId = nodeId;
        this.roadmapJob = roadmapJob;
        this.category = category;
        this.label = label;
        this.description = description;
        this.level = level != null ? level.byteValue() : (byte) 1;
        this.estimatedHours = estimatedHours;
        this.positionX = positionX;
        this.positionY = positionY;
        this.nodeType = nodeType != null ? nodeType : "techNode";
        this.isActive = isActive != null ? isActive : true;
    }

    public void updateNode(String label, String description, Category category,
                          Integer level, Integer estimatedHours,
                          Integer positionX, Integer positionY) {
        this.label = label;
        this.description = description;
        this.category = category;
        if (level != null) {
            this.level = level.byteValue();
        }
        this.estimatedHours = estimatedHours;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public void updatePosition(Integer x, Integer y) {
        this.positionX = x;
        this.positionY = y;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    public String getJobId() {
        return roadmapJob != null ? roadmapJob.getJobId() : null;
    }
}
