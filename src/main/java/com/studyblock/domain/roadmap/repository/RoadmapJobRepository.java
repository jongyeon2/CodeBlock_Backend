package com.studyblock.domain.roadmap.repository;

import com.studyblock.domain.roadmap.entity.RoadmapJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapJobRepository extends JpaRepository<RoadmapJob, Long> {

    Optional<RoadmapJob> findByJobId(String jobId);

    @Query("SELECT rj FROM RoadmapJob rj WHERE rj.isActive = true ORDER BY rj.displayOrder ASC")
    List<RoadmapJob> findAllActiveOrderByDisplayOrder();

    boolean existsByJobId(String jobId);
}
