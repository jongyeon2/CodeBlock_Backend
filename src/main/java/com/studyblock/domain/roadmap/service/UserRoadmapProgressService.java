package com.studyblock.domain.roadmap.service;

import com.studyblock.domain.roadmap.dto.response.UserProgressResponse;
import com.studyblock.domain.roadmap.entity.RoadmapNode;
import com.studyblock.domain.roadmap.entity.UserRoadmapProgress;
import com.studyblock.domain.roadmap.enums.ProgressStatus;
import com.studyblock.domain.roadmap.repository.UserRoadmapProgressRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRoadmapProgressService {

    private final UserRoadmapProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final RoadmapService roadmapService;

    /**
     * 사용자의 특정 직군 진행 상황 조회
     */
    public List<UserProgressResponse> getUserProgress(Long userId, String jobId) {
        log.info("사용자 로드맵 진행 상황 조회 - userId: {}, jobId: {}", userId, jobId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<UserRoadmapProgress> progressList = progressRepository.findAllByUserAndJobId(user, jobId);

        return progressList.stream()
                .map(UserProgressResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 진행 상황 업데이트 (생성 또는 수정)
     */
    @Transactional
    public UserProgressResponse updateProgress(Long userId, String nodeId, ProgressStatus status) {
        log.info("로드맵 진행 상황 업데이트 - userId: {}, nodeId: {}, status: {}", userId, nodeId, status);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        RoadmapNode node = roadmapService.getNodeByNodeId(nodeId);

        // 기존 진행 상황이 있으면 업데이트, 없으면 생성
        UserRoadmapProgress progress = progressRepository.findByUserAndRoadmapNode(user, node)
                .orElseGet(() -> UserRoadmapProgress.builder()
                        .user(user)
                        .roadmapNode(node)
                        .status(ProgressStatus.NOT_STARTED)
                        .build());

        progress.updateStatus(status);

        UserRoadmapProgress savedProgress = progressRepository.save(progress);

        return UserProgressResponse.from(savedProgress);
    }

    /**
     * 진행률 계산 (완료된 노드 수 / 전체 노드 수)
     */
    public Double calculateProgressPercentage(Long userId, String jobId) {
        log.info("로드맵 진행률 계산 - userId: {}, jobId: {}", userId, jobId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        long totalNodes = roadmapService.getTotalNodeCount(jobId);
        if (totalNodes == 0) {
            return 0.0;
        }

        long completedNodes = progressRepository.countByUserAndJobIdAndStatus(
                user, jobId, ProgressStatus.COMPLETED
        );

        return (completedNodes * 100.0) / totalNodes;
    }

    /**
     * 완료된 노드 수 조회
     */
    public Long getCompletedNodeCount(Long userId, String jobId) {
        log.info("완료된 노드 수 조회 - userId: {}, jobId: {}", userId, jobId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return progressRepository.countByUserAndJobIdAndStatus(
                user, jobId, ProgressStatus.COMPLETED
        );
    }

    /**
     * 특정 노드의 진행 상황 삭제
     */
    @Transactional
    public void deleteProgress(Long userId, String nodeId) {
        log.info("로드맵 진행 상황 삭제 - userId: {}, nodeId: {}", userId, nodeId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        RoadmapNode node = roadmapService.getNodeByNodeId(nodeId);

        progressRepository.deleteByUserAndRoadmapNode(user, node);
    }
}
