package com.studyblock.domain.roadmap.repository;

import com.studyblock.domain.roadmap.entity.RoadmapEdge;
import com.studyblock.domain.roadmap.entity.RoadmapJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapEdgeRepository extends JpaRepository<RoadmapEdge, Long> {

    Optional<RoadmapEdge> findByEdgeId(String edgeId);

    @Query("SELECT re FROM RoadmapEdge re " +
           "LEFT JOIN FETCH re.sourceNode " +
           "LEFT JOIN FETCH re.targetNode " +
           "WHERE re.roadmapJob = :roadmapJob AND re.isActive = true")
    List<RoadmapEdge> findAllByRoadmapJobAndIsActiveTrue(@Param("roadmapJob") RoadmapJob roadmapJob);

    @Query("SELECT re FROM RoadmapEdge re " +
           "LEFT JOIN FETCH re.sourceNode " +
           "LEFT JOIN FETCH re.targetNode " +
           "WHERE re.roadmapJob.jobId = :jobId AND re.isActive = true")
    List<RoadmapEdge> findAllByJobIdAndIsActiveTrue(@Param("jobId") String jobId);

    boolean existsByEdgeId(String edgeId);
}
