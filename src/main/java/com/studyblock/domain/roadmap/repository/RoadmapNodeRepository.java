package com.studyblock.domain.roadmap.repository;

import com.studyblock.domain.roadmap.entity.RoadmapJob;
import com.studyblock.domain.roadmap.entity.RoadmapNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapNodeRepository extends JpaRepository<RoadmapNode, Long> {

    Optional<RoadmapNode> findByNodeId(String nodeId);

    @Query("SELECT rn FROM RoadmapNode rn " +
           "LEFT JOIN FETCH rn.category " +
           "WHERE rn.roadmapJob = :roadmapJob AND rn.isActive = true " +
           "ORDER BY rn.level ASC, rn.positionX ASC")
    List<RoadmapNode> findAllByRoadmapJobAndIsActiveTrue(@Param("roadmapJob") RoadmapJob roadmapJob);

    @Query("SELECT rn FROM RoadmapNode rn " +
           "LEFT JOIN FETCH rn.category " +
           "WHERE rn.roadmapJob.jobId = :jobId AND rn.isActive = true " +
           "ORDER BY rn.level ASC, rn.positionX ASC")
    List<RoadmapNode> findAllByJobIdAndIsActiveTrue(@Param("jobId") String jobId);

    boolean existsByNodeId(String nodeId);

    long countByRoadmapJobAndIsActiveTrue(RoadmapJob roadmapJob);
}
