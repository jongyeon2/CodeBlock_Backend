package com.studyblock.domain.roadmap.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "roadmap_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", unique = true, nullable = false, length = 50)
    private String jobId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "roadmapJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadmapNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "roadmapJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadmapEdge> edges = new ArrayList<>();

    @Builder
    private RoadmapJob(String jobId, String title, String description,
                      Boolean isActive, Integer displayOrder) {
        this.jobId = jobId;
        this.title = title;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void updateJob(String title, String description, Integer displayOrder) {
        this.title = title;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
