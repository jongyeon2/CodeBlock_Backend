package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.*;
import com.studyblock.domain.course.service.QuizService;
import com.studyblock.global.aop.annotation.RequiresInstructorRole;
import com.studyblock.global.aop.annotation.RequiresQuizOwnership;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz", description = "퀴즈 관련 API")
public class QuizController {

    private final QuizService quizService;

    /**
     * 강의의 퀴즈 목록 조회
     */
    @GetMapping("/lectures/{lectureId}/quizzes")
    @Operation(
            summary = "강의의 퀴즈 목록 조회",
            description = "강의 ID로 해당 강의의 모든 퀴즈를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 목록 조회 성공")
    @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<QuizSummaryResponse>>> getQuizzesByLecture(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @PathVariable Long lectureId) {

        List<QuizSummaryResponse> response = quizService.getQuizzesByLectureId(lectureId);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 목록 조회 성공", response));
    }

    /**
     * 섹션의 퀴즈 목록 조회
     */
    @GetMapping("/sections/{sectionId}/quizzes")
    @Operation(
            summary = "섹션의 퀴즈 목록 조회",
            description = "섹션 ID로 해당 섹션의 모든 퀴즈를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 목록 조회 성공")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<QuizSummaryResponse>>> getQuizzesBySection(
            @Parameter(description = "섹션 ID", required = true, example = "1")
            @PathVariable Long sectionId) {

        List<QuizSummaryResponse> response = quizService.getQuizzesBySectionId(sectionId);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 목록 조회 성공", response));
    }

    /**
     * 퀴즈 상세 정보 조회 (문제 포함)
     */
    @GetMapping("/quizzes/{quizId}")
    @Operation(
            summary = "퀴즈 상세 정보 조회",
            description = "퀴즈 ID로 해당 퀴즈의 상세 정보와 문제 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 상세 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<QuizDetailResponse>> getQuizDetail(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId) {

        QuizDetailResponse response = quizService.getQuizDetail(quizId);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 상세 정보 조회 성공", response));
    }

    /**
     * 퀴즈 제출 및 채점
     */
    @PostMapping("/quizzes/{quizId}/submit")
    @Operation(
            summary = "퀴즈 제출 및 채점",
            description = "퀴즈 답안을 제출하고 채점 결과를 받습니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 제출 성공")
    @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<QuizResultResponse>> submitQuiz(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId,
            @RequestBody QuizSubmitRequest request) {

        QuizResultResponse response = quizService.submitQuiz(quizId, request);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 제출 성공", response));
    }

    /**
     * 퀴즈 시도 이력 조회
     */
    @GetMapping("/quizzes/{quizId}/attempts")
    @Operation(
            summary = "퀴즈 시도 이력 조회",
            description = "사용자의 퀴즈 시도 이력을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "시도 이력 조회 성공")
    @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<Object>>> getQuizAttempts(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId) {

        List<Object> response = quizService.getQuizAttempts(quizId);
        return ResponseEntity.ok(CommonResponse.success("시도 이력 조회 성공", response));
    }

    // ===== 강사용 퀴즈 관리 API =====

    /**
     * 퀴즈 생성 (강사용)
     */
    @PostMapping("/quizzes")
    @RequiresInstructorRole
    @Operation(
            summary = "퀴즈 생성 (강사용)",
            description = "강사가 섹션에 새로운 퀴즈를 생성합니다. sequence는 자동으로 계산됩니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @ApiResponse(responseCode = "404", description = "섹션 또는 강의를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<QuizSummaryResponse>> createQuiz(
            @RequestBody QuizCreateRequest request) {

        QuizSummaryResponse response = quizService.createQuiz(request);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 생성 성공", response));
    }

    /**
     * 코스의 퀴즈 목록 조회 (강사용)
     */
    @GetMapping("/courses/{courseId}/quizzes")
    @RequiresInstructorRole
    @Operation(
            summary = "코스의 퀴즈 목록 조회 (강사용)",
            description = "코스 ID로 해당 코스의 모든 퀴즈를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 목록 조회 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<QuizSummaryResponse>>> getQuizzesByCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        List<QuizSummaryResponse> response = quizService.getQuizzesByCourseId(courseId);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 목록 조회 성공", response));
    }

    /**
     * 퀴즈 수정 (강사용)
     */
    @PutMapping("/quizzes/{quizId}")
    @RequiresQuizOwnership
    @Operation(
            summary = "퀴즈 수정 (강사용)",
            description = "퀴즈의 정보를 수정합니다. position 변경 시 sequence가 자동으로 재계산됩니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 수정 성공")
    @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<QuizSummaryResponse>> updateQuiz(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId,
            @RequestBody QuizUpdateRequest request) {

        QuizSummaryResponse response = quizService.updateQuiz(quizId, request);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 수정 성공", response));
    }

    /**
     * 퀴즈 삭제 (강사용)
     */
    @DeleteMapping("/quizzes/{quizId}")
    @RequiresQuizOwnership
    @Operation(
            summary = "퀴즈 삭제 (강사용)",
            description = "퀴즈를 삭제합니다. 연결된 문제와 옵션도 함께 삭제됩니다."
    )
    @ApiResponse(responseCode = "200", description = "퀴즈 삭제 성공")
    @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Void>> deleteQuiz(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId) {

        quizService.deleteQuiz(quizId);
        return ResponseEntity.ok(CommonResponse.success("퀴즈 삭제 성공", null));
    }
}
