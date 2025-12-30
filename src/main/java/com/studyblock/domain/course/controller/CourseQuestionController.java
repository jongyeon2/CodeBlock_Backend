package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.CourseQuestionAnswerCreateRequest;
import com.studyblock.domain.course.dto.CourseQuestionAnswerResponse;
import com.studyblock.domain.course.dto.CourseQuestionAnswerUpdateRequest;
import com.studyblock.domain.course.dto.CourseQuestionCreateRequest;
import com.studyblock.domain.course.dto.CourseQuestionResponse;
import com.studyblock.domain.course.dto.CourseQuestionUpdateRequest;
import com.studyblock.domain.course.enums.CourseQuestionStatus;
import com.studyblock.domain.course.service.CourseQuestionService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseId}/questions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course QnA", description = "코스 Q&A 게시판 API")
public class CourseQuestionController {

    private final CourseQuestionService courseQuestionService;

    /**
     * Q&A 목록 조회
     */
    @GetMapping
    @Operation(
            summary = "코스 Q&A 목록 조회",
            description = "코스 Q&A 게시글을 페이지 단위로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "Q&A 목록 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Page<CourseQuestionResponse>>> getQuestions(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Parameter(description = "질문 상태 필터 (PENDING, ANSWERED)", example = "PENDING")
            @RequestParam(value = "status", required = false) CourseQuestionStatus status,
            @Parameter(description = "페이징 정보", hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<CourseQuestionResponse> response = courseQuestionService.getQuestions(courseId, status, pageable);
        return ResponseEntity.ok(CommonResponse.success("Q&A 목록 조회 성공", response));
    }

    /**
     * 질문 등록
     */
    @PostMapping
    @Operation(
            summary = "코스 Q&A 질문 등록",
            description = "코스 Q&A 게시판에 질문을 등록합니다."
    )
    @ApiResponse(responseCode = "201", description = "질문 등록 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseQuestionResponse>> createQuestion(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Valid @RequestBody CourseQuestionCreateRequest request) {

        CourseQuestionResponse response = courseQuestionService.createQuestion(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success("Q&A 질문 등록 성공", response));
    }

    /**
     * 질문 수정
     */
    @PutMapping("/{questionId}")
    @Operation(
            summary = "코스 Q&A 질문 수정",
            description = "등록된 Q&A 질문 내용을 수정합니다."
    )
    @ApiResponse(responseCode = "200", description = "질문 수정 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseQuestionResponse>> updateQuestion(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Parameter(description = "질문 ID", required = true, example = "50")
            @PathVariable("questionId") Long questionId,
            @Valid @RequestBody CourseQuestionUpdateRequest request) {

        CourseQuestionResponse response = courseQuestionService.updateQuestion(courseId, questionId, request);
        return ResponseEntity.ok(CommonResponse.success("Q&A 질문 수정 성공", response));
    }

    /**
     * 질문 삭제
     */
    @DeleteMapping("/{questionId}")
    @Operation(
            summary = "코스 Q&A 질문 삭제",
            description = "등록된 Q&A 질문을 삭제합니다."
    )
    @ApiResponse(responseCode = "200", description = "질문 삭제 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Void>> deleteQuestion(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable("courseId") Long courseId,
            @Parameter(description = "질문 ID", required = true, example = "50")
            @PathVariable("questionId") Long questionId) {

        courseQuestionService.deleteQuestion(courseId, questionId);
        return ResponseEntity.ok(CommonResponse.success("Q&A 질문 삭제 성공"));
    }

    /**
     * 답변 등록
     */
    @PostMapping("/{questionId}/answers")
    @Operation(
            summary = "코스 Q&A 답변 등록",
            description = "질문에 대한 답변을 추가합니다."
    )
    @ApiResponse(responseCode = "201", description = "답변 등록 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseQuestionAnswerResponse>> createAnswer(
            @PathVariable("courseId") Long courseId,
            @PathVariable("questionId") Long questionId,
            @AuthenticationPrincipal com.studyblock.domain.user.entity.User currentUser,
            @Valid @RequestBody CourseQuestionAnswerCreateRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요합니다."));
        }

        CourseQuestionAnswerResponse response = courseQuestionService.createAnswer(courseId, questionId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success("Q&A 답변 등록 성공", response));
    }

    /**
     * 답변 수정
     */
    @PutMapping("/answers/{answerId}")
    @Operation(
            summary = "코스 Q&A 답변 수정",
            description = "작성한 답변을 수정합니다."
    )
    @ApiResponse(responseCode = "200", description = "답변 수정 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseQuestionAnswerResponse>> updateAnswer(
            @PathVariable("courseId") Long courseId,
            @PathVariable("answerId") Long answerId,
            @AuthenticationPrincipal com.studyblock.domain.user.entity.User currentUser,
            @Valid @RequestBody CourseQuestionAnswerUpdateRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요합니다."));
        }

        CourseQuestionAnswerResponse response = courseQuestionService.updateAnswer(courseId, answerId, currentUser.getId(), request);
        return ResponseEntity.ok(CommonResponse.success("Q&A 답변 수정 성공", response));
    }

    /**
     * 답변 삭제
     */
    @DeleteMapping("/answers/{answerId}")
    @Operation(
            summary = "코스 Q&A 답변 삭제",
            description = "작성한 답변을 삭제합니다."
    )
    @ApiResponse(responseCode = "200", description = "답변 삭제 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Void>> deleteAnswer(
            @PathVariable("courseId") Long courseId,
            @PathVariable("answerId") Long answerId,
            @AuthenticationPrincipal com.studyblock.domain.user.entity.User currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요합니다."));
        }

        courseQuestionService.deleteAnswer(courseId, answerId, currentUser.getId());
        return ResponseEntity.ok(CommonResponse.success("Q&A 답변 삭제 성공"));
    }
}
