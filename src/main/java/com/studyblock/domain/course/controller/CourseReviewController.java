package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.*;
import com.studyblock.domain.course.service.CourseReviewService;
import com.studyblock.domain.course.service.CourseService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Review", description = "코스 수강 후기 API")
public class CourseReviewController {

    private final CourseReviewService courseReviewService;
    private final CourseService courseService;

    /**
     * 코스 수강 후기 목록 조회
     */
    @GetMapping
    @Operation(
            summary = "코스 수강 후기 목록 조회",
            description = "코스 수강 후기를 페이지 단위로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "수강 후기 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Page<CourseReviewResponse>>> getCourseReviews(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Parameter(description = "특정 강의 ID (선택)", example = "10")
            @RequestParam(value = "lectureId", required = false) Long lectureId,
            @Parameter(description = "페이징 정보", hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<CourseReviewResponse> response = courseReviewService.getCourseReviews(courseId, lectureId, pageable);
        return ResponseEntity.ok(CommonResponse.success("수강 후기 조회 성공", response));
    }

    /**
     * 코스 수강 후기 요약 정보 조회
     */
    @GetMapping("/summary")
    @Operation(
            summary = "코스 수강 후기 요약 조회",
            description = "코스 수강 후기 개수 및 평균 평점을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "요약 정보 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseReviewSummaryResponse>> getReviewSummary(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId) {

        CourseReviewSummaryResponse response = courseService.getReviewSummary(courseId);
        return ResponseEntity.ok(CommonResponse.success("수강 후기 요약 조회 성공", response));
    }

    /**
     * 코스 수강 후기 등록
     */
    @PostMapping
    @Operation(
            summary = "코스 수강 후기 등록",
            description = "코스에 대한 수강 후기를 등록합니다."
    )
    @ApiResponse(responseCode = "201", description = "수강 후기 등록 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseReviewResponse>> createReview(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Valid @RequestBody CourseReviewCreateRequest request) {

        CourseReviewResponse response = courseReviewService.createReview(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success("수강 후기 등록 성공", response));
    }

    /**
     * 코스 수강 후기 수정
     */
    @PutMapping("/{reviewId}")
    @Operation(
            summary = "코스 수강 후기 수정",
            description = "등록된 수강 후기를 수정합니다."
    )
    @ApiResponse(responseCode = "200", description = "수강 후기 수정 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseReviewResponse>> updateReview(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Parameter(description = "리뷰 ID", required = true, example = "100")
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody CourseReviewUpdateRequest request) {

        CourseReviewResponse response = courseReviewService.updateReview(courseId, reviewId, request);
        return ResponseEntity.ok(CommonResponse.success("수강 후기 수정 성공", response));
    }

    /**
     * 코스 수강 후기 삭제
     */
    @DeleteMapping("/{reviewId}")
    @Operation(
            summary = "코스 수강 후기 삭제",
            description = "등록된 수강 후기를 삭제합니다."
    )
    @ApiResponse(responseCode = "200", description = "수강 후기 삭제 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Void>> deleteReview(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Parameter(description = "리뷰 ID", required = true, example = "100")
            @PathVariable("reviewId") Long reviewId) {

        courseReviewService.deleteReview(courseId, reviewId);
        return ResponseEntity.ok(CommonResponse.success("수강 후기 삭제 성공"));
    }
}
