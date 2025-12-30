package com.studyblock.domain.roadmap.controller;

import com.studyblock.domain.course.dto.CourseResponse;
import com.studyblock.domain.roadmap.dto.request.RoadmapProgressUpdateRequest;
import com.studyblock.domain.roadmap.dto.response.RoadmapDetailResponse;
import com.studyblock.domain.roadmap.dto.response.RoadmapJobResponse;
import com.studyblock.domain.roadmap.dto.response.UserProgressResponse;
import com.studyblock.domain.roadmap.service.RoadmapService;
import com.studyblock.domain.roadmap.service.UserRoadmapProgressService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roadmap", description = "로드맵 관련 API")
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final UserRoadmapProgressService progressService;

    /**
     * 1. 모든 직군 목록 조회 (메인 페이지)
     */
    @GetMapping("/jobs")
    @Operation(
            summary = "모든 로드맵 직군 조회",
            description = "활성화된 모든 로드맵 직군 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "직군 목록 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<RoadmapJobResponse>>> getAllJobs() {
        log.info("GET /api/roadmap/jobs - 모든 직군 조회");
        List<RoadmapJobResponse> jobs = roadmapService.getAllActiveJobs();
        return ResponseEntity.ok(
                CommonResponse.success("직군 목록 조회 성공", jobs)
        );
    }

    /**
     * 2. 특정 직군의 로드맵 상세 조회 (노드 + 엣지)
     */
    @GetMapping("/{jobId}")
    @Operation(
            summary = "로드맵 상세 조회",
            description = "특정 직군의 로드맵 노드와 엣지 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "로드맵 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 직군")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<RoadmapDetailResponse>> getRoadmapDetail(
            @Parameter(description = "직군 ID (backend, frontend, fullstack, data)", required = true, example = "backend")
            @PathVariable String jobId) {
        log.info("GET /api/roadmap/{} - 로드맵 상세 조회", jobId);
        RoadmapDetailResponse roadmap = roadmapService.getRoadmapByJobId(jobId);
        return ResponseEntity.ok(
                CommonResponse.success("로드맵 조회 성공", roadmap)
        );
    }

    /**
     * 3. 특정 노드의 관련 강의 조회 (카테고리 기반)
     */
    @GetMapping("/nodes/{nodeId}/courses")
    @Operation(
            summary = "노드 관련 강의 조회",
            description = "특정 기술 노드와 관련된 강의 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "관련 강의 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 노드")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Page<CourseResponse>>> getRelatedCourses(
            @Parameter(description = "노드 ID", required = true, example = "backend-1")
            @PathVariable String nodeId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "3")
            @RequestParam(defaultValue = "3") int size,
            @Parameter(description = "정렬 기준", example = "id,desc")
            @RequestParam(defaultValue = "id,desc") String sort) {
        log.info("GET /api/roadmap/nodes/{}/courses - page: {}, size: {}", nodeId, page, size);

        // 정렬 파싱
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<CourseResponse> courses = roadmapService.getRelatedCourses(nodeId, pageable);
        return ResponseEntity.ok(
                CommonResponse.success("관련 강의 조회 성공", courses)
        );
    }

    /**
     * 4. 사용자 진행 상황 조회 (로그인 필요)
     */
    @GetMapping("/progress/{jobId}")
    @Operation(
            summary = "사용자 진행 상황 조회",
            description = "로그인한 사용자의 특정 직군 진행 상황을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "진행 상황 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<UserProgressResponse>>> getUserProgress(
            @Parameter(description = "직군 ID", required = true, example = "backend")
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/roadmap/progress/{} - user: {}", jobId, userDetails.getUsername());

        // UserDetails에서 userId 추출 (실제 구현은 프로젝트에 맞게 수정 필요)
        Long userId = extractUserIdFromUserDetails(userDetails);

        List<UserProgressResponse> progress = progressService.getUserProgress(userId, jobId);
        return ResponseEntity.ok(
                CommonResponse.success("진행 상황 조회 성공", progress)
        );
    }

    /**
     * 5. 사용자 진행 상황 업데이트 (로그인 필요)
     */
    @PutMapping("/progress/{nodeId}")
    @Operation(
            summary = "진행 상황 업데이트",
            description = "특정 노드의 진행 상황을 업데이트합니다."
    )
    @ApiResponse(responseCode = "200", description = "진행 상황 업데이트 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<UserProgressResponse>> updateProgress(
            @Parameter(description = "노드 ID", required = true, example = "backend-1")
            @PathVariable String nodeId,
            @Valid @RequestBody RoadmapProgressUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("PUT /api/roadmap/progress/{} - user: {}, status: {}",
                nodeId, userDetails.getUsername(), request.status());

        Long userId = extractUserIdFromUserDetails(userDetails);

        UserProgressResponse progress = progressService.updateProgress(
                userId, nodeId, request.status()
        );
        return ResponseEntity.ok(
                CommonResponse.success("진행 상황 업데이트 성공", progress)
        );
    }

    /**
     * 6. 진행률 조회
     */
    @GetMapping("/progress/{jobId}/percentage")
    @Operation(
            summary = "진행률 조회",
            description = "특정 직군의 진행률을 퍼센트로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "진행률 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Double>> getProgressPercentage(
            @Parameter(description = "직군 ID", required = true, example = "backend")
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/roadmap/progress/{}/percentage - user: {}", jobId, userDetails.getUsername());

        Long userId = extractUserIdFromUserDetails(userDetails);

        Double percentage = progressService.calculateProgressPercentage(userId, jobId);
        return ResponseEntity.ok(
                CommonResponse.success("진행률 조회 성공", percentage)
        );
    }

    /**
     * UserDetails에서 userId 추출 (실제 구현 필요)
     * TODO: 실제 UserDetails 구현에 맞게 수정
     */
    private Long extractUserIdFromUserDetails(UserDetails userDetails) {
        // 실제 구현은 프로젝트의 CustomUserDetails 구조에 따라 달라질 수 있음
        // 임시로 username을 userId로 변환 (실제로는 CustomUserDetails에서 getId() 호출)
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 정보입니다.");
        }
    }
}
