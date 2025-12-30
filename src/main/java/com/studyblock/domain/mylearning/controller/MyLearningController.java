package com.studyblock.domain.mylearning.controller;

import com.studyblock.domain.mylearning.dto.MyLearningItemResponse;
import com.studyblock.domain.mylearning.service.MyLearningService;
import com.studyblock.domain.mylearning.service.MyLearningService.MyLearningStatsResponse;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 내 학습 통합 컨트롤러
 * 보유코스(CourseEnrollment)와 보유강의(LectureOwnership)를 통합하여 제공
 */
@Tag(name = "My Learning", description = "내 학습 통합 API - 보유코스와 보유강의를 하나의 인터페이스로 제공")
@RestController
@RequestMapping("/api/my-learning")
@RequiredArgsConstructor
@Slf4j
public class MyLearningController {

    private final MyLearningService myLearningService;

    /**
     * 내 학습 콘텐츠 목록 조회 (통합)
     * GET /api/my-learning
     *
     * 보유코스와 보유강의를 통합하여 최근 활동 순으로 정렬하여 반환
     */
    @Operation(
            summary = "내 학습 콘텐츠 목록 조회 (통합)",
            description = "보유한 코스와 섹션을 통합하여 조회합니다. " +
                         "최근 활동 날짜 순으로 정렬되며, 페이징을 지원합니다. " +
                         "page, size 파라미터로 페이징 설정 가능 (예: ?page=0&size=12)"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    @GetMapping
    public ResponseEntity<CommonResponse<Page<MyLearningItemResponse>>> getMyLearningItems(
            @Parameter(description = "페이징 정보 (page, size)", hidden = true)
            @PageableDefault(size = 12, sort = "lastActivityDate", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(CommonResponse.error("로그인이 필요한 서비스입니다."));
        }

        log.info("내 학습 콘텐츠 목록 조회 - userId: {}, page: {}, size: {}",
                currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<MyLearningItemResponse> items = myLearningService.getMyLearningItems(
                currentUser.getId(),
                pageable
        );

        return ResponseEntity.ok(CommonResponse.success(
                "내 학습 콘텐츠 조회 성공",
                items
        ));
    }

    /**
     * 내 학습 통계 조회
     * GET /api/my-learning/stats
     *
     * 보유코스 수, 완료한 코스 수, 보유섹션 수 등의 통계 정보 제공
     */
    @Operation(
            summary = "내 학습 통계 조회",
            description = "보유한 코스와 섹션의 통계 정보를 조회합니다. " +
                         "총 코스 수, 완료한 코스 수, 진행 중인 코스 수, 총 섹션 수 등을 제공합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @CommonApiResponses
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<MyLearningStatsResponse>> getMyLearningStats(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(CommonResponse.error("로그인이 필요한 서비스입니다."));
        }

        log.info("내 학습 통계 조회 - userId: {}", currentUser.getId());

        MyLearningStatsResponse stats = myLearningService.getMyLearningStats(currentUser.getId());

        return ResponseEntity.ok(CommonResponse.success(
                "내 학습 통계 조회 성공",
                stats
        ));
    }
}
