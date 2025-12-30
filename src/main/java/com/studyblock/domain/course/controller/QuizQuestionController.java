package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.QuizQuestionCreateRequest;
import com.studyblock.domain.course.dto.QuizQuestionResponse;
import com.studyblock.domain.course.dto.QuizQuestionUpdateRequest;
import com.studyblock.domain.course.service.QuizQuestionService;
import com.studyblock.global.aop.annotation.RequiresQuizOwnership;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes/{quizId}/questions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz Question", description = "퀴즈 문제 관리 API (강사용)")
public class QuizQuestionController {

    private final QuizQuestionService quizQuestionService;

    /**
     * 퀴즈에 문제 추가 (강사용)
     */
    @PostMapping
    @RequiresQuizOwnership
    @Operation(
            summary = "퀴즈 문제 추가 (강사용)",
            description = "퀴즈에 새로운 문제를 추가합니다. sequence는 자동으로 계산됩니다.\n\n" +
                    "**문제 유형:**\n" +
                    "- MULTIPLE_CHOICE: 객관식 (options 필수, correctAnswer는 옵션 인덱스)\n" +
                    "- SUBJECTIVE: 주관식 (correctAnswer는 정답 텍스트)\n" +
                    "- SHORT_ANSWER: 단답형 (correctAnswer는 정답 텍스트)\n\n" +
                    "**요청 예시 (객관식):**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"question\": \"Spring Framework의 핵심 개념이 아닌 것은?\",\n" +
                    "  \"question_type\": \"MULTIPLE_CHOICE\",\n" +
                    "  \"points\": 5,\n" +
                    "  \"options\": [\"IoC\", \"AOP\", \"MVC\", \"Hibernate\"],\n" +
                    "  \"correct_answer\": 3,\n" +
                    "  \"explanation\": \"Hibernate는 ORM 프레임워크입니다.\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "**요청 예시 (주관식/단답형):**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"question\": \"JPA의 정식 명칭을 작성하세요.\",\n" +
                    "  \"question_type\": \"SHORT_ANSWER\",\n" +
                    "  \"points\": 3,\n" +
                    "  \"correct_answer\": \"Java Persistence API\",\n" +
                    "  \"explanation\": \"JPA는 Java Persistence API의 약자입니다.\"\n" +
                    "}\n" +
                    "```"
    )
    @ApiResponse(responseCode = "200", description = "문제 추가 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<QuizQuestionResponse>> createQuestion(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId,
            @Valid @RequestBody QuizQuestionCreateRequest request) {

        log.info("문제 추가 요청: quizId={}, questionType={}", quizId, request.getQuestionType());
        QuizQuestionResponse response = quizQuestionService.createQuestion(quizId, request);
        return ResponseEntity.ok(CommonResponse.success("문제 추가 성공", response));
    }

    /**
     * 퀴즈 문제 수정 (강사용)
     */
    @PutMapping("/{questionId}")
    @RequiresQuizOwnership
    @Operation(
            summary = "퀴즈 문제 수정 (강사용)",
            description = "퀴즈 문제를 수정합니다. 모든 필드는 선택사항이며, 제공된 필드만 수정됩니다.\n\n" +
                    "**주의사항:**\n" +
                    "- questionType 변경은 불가능합니다 (필요시 삭제 후 재생성)\n" +
                    "- options를 수정할 때는 correct_answer도 함께 제공해야 합니다\n" +
                    "- options는 전체 교체 방식으로 동작합니다 (기존 옵션 삭제 후 새로 생성)\n\n" +
                    "**요청 예시 1 (문제 내용과 점수만 수정):**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"question\": \"수정된 문제 내용\",\n" +
                    "  \"points\": 10\n" +
                    "}\n" +
                    "```\n\n" +
                    "**요청 예시 2 (객관식 선택지 전체 교체):**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"options\": [\"새 옵션1\", \"새 옵션2\", \"새 옵션3\", \"새 옵션4\"],\n" +
                    "  \"correct_answer\": 2\n" +
                    "}\n" +
                    "```\n\n" +
                    "**요청 예시 3 (객관식 정답만 변경):**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"correct_answer\": 1\n" +
                    "}\n" +
                    "```\n\n" +
                    "**요청 예시 4 (주관식 정답 변경):**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"correct_answer\": \"새로운 정답 텍스트\"\n" +
                    "}\n" +
                    "```"
    )
    @ApiResponse(responseCode = "200", description = "문제 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    @ApiResponse(responseCode = "404", description = "퀴즈 또는 문제를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<QuizQuestionResponse>> updateQuestion(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId,
            @Parameter(description = "문제 ID", required = true, example = "1")
            @PathVariable Long questionId,
            @Valid @RequestBody QuizQuestionUpdateRequest request) {

        log.info("문제 수정 요청: quizId={}, questionId={}", quizId, questionId);
        QuizQuestionResponse response = quizQuestionService.updateQuestion(quizId, questionId, request);
        return ResponseEntity.ok(CommonResponse.success("문제 수정 성공", response));
    }

    /**
     * 퀴즈 문제 삭제 (강사용)
     */
    @DeleteMapping("/{questionId}")
    @RequiresQuizOwnership
    @Operation(
            summary = "퀴즈 문제 삭제 (강사용)",
            description = "퀴즈 문제를 삭제합니다. 연결된 옵션도 함께 삭제됩니다."
    )
    @ApiResponse(responseCode = "200", description = "문제 삭제 성공")
    @ApiResponse(responseCode = "404", description = "퀴즈 또는 문제를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Void>> deleteQuestion(
            @Parameter(description = "퀴즈 ID", required = true, example = "1")
            @PathVariable Long quizId,
            @Parameter(description = "문제 ID", required = true, example = "1")
            @PathVariable Long questionId) {

        log.info("문제 삭제 요청: quizId={}, questionId={}", quizId, questionId);
        quizQuestionService.deleteQuestion(quizId, questionId);
        return ResponseEntity.ok(CommonResponse.success("문제 삭제 성공", null));
    }
}