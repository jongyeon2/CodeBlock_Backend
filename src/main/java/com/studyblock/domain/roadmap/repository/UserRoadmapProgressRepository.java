package com.studyblock.domain.roadmap.repository;

import com.studyblock.domain.roadmap.entity.RoadmapNode;
import com.studyblock.domain.roadmap.entity.UserRoadmapProgress;
import com.studyblock.domain.roadmap.enums.ProgressStatus;
import com.studyblock.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoadmapProgressRepository extends JpaRepository<UserRoadmapProgress, Long> {

    Optional<UserRoadmapProgress> findByUserAndRoadmapNode(User user, RoadmapNode roadmapNode);

    @Query("SELECT urp FROM UserRoadmapProgress urp " +
           "LEFT JOIN FETCH urp.roadmapNode rn " +
           "WHERE urp.user = :user AND rn.roadmapJob.jobId = :jobId")
    List<UserRoadmapProgress> findAllByUserAndJobId(@Param("user") User user, @Param("jobId") String jobId);

    @Query("SELECT COUNT(urp) FROM UserRoadmapProgress urp " +
           "WHERE urp.user = :user AND urp.roadmapNode.roadmapJob.jobId = :jobId " +
           "AND urp.status = :status")
    long countByUserAndJobIdAndStatus(@Param("user") User user,
                                       @Param("jobId") String jobId,
                                       @Param("status") ProgressStatus status);

    boolean existsByUserAndRoadmapNode(User user, RoadmapNode roadmapNode);

    void deleteByUserAndRoadmapNode(User user, RoadmapNode roadmapNode);
}
