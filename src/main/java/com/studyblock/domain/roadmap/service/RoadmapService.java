package com.studyblock.domain.roadmap.service;

import com.studyblock.domain.course.dto.CourseResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.roadmap.dto.response.*;
import com.studyblock.domain.roadmap.entity.RoadmapEdge;
import com.studyblock.domain.roadmap.entity.RoadmapJob;
import com.studyblock.domain.roadmap.entity.RoadmapNode;
import com.studyblock.domain.roadmap.repository.RoadmapEdgeRepository;
import com.studyblock.domain.roadmap.repository.RoadmapJobRepository;
import com.studyblock.domain.roadmap.repository.RoadmapNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadmapService {

    private final RoadmapJobRepository roadmapJobRepository;
    private final RoadmapNodeRepository roadmapNodeRepository;
    private final RoadmapEdgeRepository roadmapEdgeRepository;
    private final CourseRepository courseRepository;

    /**
     * 모든 활성화된 직군 목록 조회 (display_order 정렬)
     */
    public List<RoadmapJobResponse> getAllActiveJobs() {
        log.info("모든 활성화된 로드맵 직군 조회");
        List<RoadmapJob> jobs = roadmapJobRepository.findAllActiveOrderByDisplayOrder();
        return jobs.stream()
                .map(RoadmapJobResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 직군의 로드맵 전체 조회 (노드 + 엣지)
     */
    public RoadmapDetailResponse getRoadmapByJobId(String jobId) {
        log.info("로드맵 조회 - jobId: {}", jobId);

        RoadmapJob job = roadmapJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직군입니다: " + jobId));

        if (!job.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 직군입니다: " + jobId);
        }

        List<RoadmapNode> nodes = roadmapNodeRepository.findAllByRoadmapJobAndIsActiveTrue(job);
        List<RoadmapEdge> edges = roadmapEdgeRepository.findAllByRoadmapJobAndIsActiveTrue(job);

        RoadmapJobResponse jobResponse = RoadmapJobResponse.from(job);
        List<RoadmapNodeResponse> nodeResponses = nodes.stream()
                .map(RoadmapNodeResponse::from)
                .collect(Collectors.toList());
        List<RoadmapEdgeResponse> edgeResponses = edges.stream()
                .map(RoadmapEdgeResponse::from)
                .collect(Collectors.toList());

        return RoadmapDetailResponse.of(jobResponse, nodeResponses, edgeResponses);
    }

    /**
     * 노드 ID로 노드 조회
     */
    public RoadmapNode getNodeByNodeId(String nodeId) {
        return roadmapNodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노드입니다: " + nodeId));
    }

    /**
     * 노드의 카테고리로 관련 강의 검색
     */
    public Page<CourseResponse> getRelatedCourses(String nodeId, Pageable pageable) {
        log.info("노드의 관련 강의 조회 - nodeId: {}, page: {}, size: {}",
                nodeId, pageable.getPageNumber(), pageable.getPageSize());

        RoadmapNode node = getNodeByNodeId(nodeId);

        if (node.getCategory() == null) {
            log.warn("노드에 카테고리가 연결되어 있지 않습니다 - nodeId: {}", nodeId);
            return Page.empty(pageable);
        }

        // category_id로 강의 검색 후 DTO로 변환
        Page<Course> courses = courseRepository.findByCategoryId(node.getCategory().getId(), pageable);
        return courses.map(CourseResponse::from);
    }

    /**
     * 특정 직군의 전체 노드 수 조회
     */
    public long getTotalNodeCount(String jobId) {
        RoadmapJob job = roadmapJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직군입니다: " + jobId));

        return roadmapNodeRepository.countByRoadmapJobAndIsActiveTrue(job);
    }
}
