package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.LectureRequest;
import com.studyblock.domain.course.dto.LectureSummaryResponse;
import com.studyblock.domain.course.service.LectureService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 강의 메타 정보 관리 컨트롤러
 *
 * Video API와 분리하여 강의 메타 정보(제목, 설명, 순서, 상태)만 관리합니다.
 * 비디오 업로드/자막/인코딩/스트리밍은 VideoController를 사용하세요.
 */
@Tag(name = "Lecture", description = "강의 메타 정보 관리 API - 강의 수정·삭제 (Video API와 분리)")
@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
@Slf4j
public class LectureController {

    private final LectureService lectureService;

    /**
     * 강의 생성
     * POST /api/lectures
     *
     * 주의:
     * - sectionId는 필수입니다. 강의가 속할 섹션을 지정해야 합니다.
     * - 비디오/자막 업로드는 별도의 Video API를 사용하세요.
     */
    @Operation(
            summary = "강의 생성",
            description = "새로운 강의를 생성합니다. sectionId는 필수이며, 강의가 속할 섹션을 지정해야 합니다. " +
                    "비디오/자막 관련 작업은 Video API(/api/videos/...)를 사용하세요. " +
                    "상태: DRAFT(작성중), PUBLISHED(게시됨), PRIVATE(비공개), ACTIVE(활성-레거시), INACTIVE(비활성-레거시)"
    )
    @ApiResponse(responseCode = "201", description = "강의 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사이면서 코스 소유자만 생성 가능")
    @ApiResponse(responseCode = "404", description = "섹션을 찾을 수 없음")
    @CommonApiResponses
    @PostMapping
    public ResponseEntity<CommonResponse<LectureSummaryResponse>> createLecture(
            @Parameter(
                    description = "생성할 강의 정보",
                    required = true,
                    schema = @Schema(implementation = LectureRequest.class)
            )
            @Valid @RequestBody LectureRequest request,

            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("강의 생성 실패 - 인증되지 않은 사용자 - sectionId: {}", request.getSectionId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        log.info("강의 생성 요청 - Section ID: {}, Title: {}, User: {}", 
                request.getSectionId(), request.getTitle(), currentUser.getEmail());

        // Service에서 비즈니스 로직 처리 (권한 검증 포함)
        LectureSummaryResponse response = lectureService.createLecture(request, currentUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("강의가 성공적으로 생성되었습니다.", response));
    }

    /**
     * 강의 메타 정보 수정
     * PUT /api/lectures/{lectureId}
     *
     * 주의:
     * - 이 API는 강의 메타 정보(title, description, sequence, status)만 수정합니다.
     * - request.sectionId는 무시됩니다 (섹션 변경 불가).
     * - 비디오 업로드/자막 업로드는 별도의 Video API를 사용하세요.
     */
    @Operation(
            summary = "강의 메타 정보 수정",
            description = "강의의 메타 정보(제목, 설명, 순서, 상태)를 수정합니다. " +
                    "request.sectionId는 무시됩니다 (섹션 변경 불가). " +
                    "비디오/자막 관련 작업은 Video API(/api/videos/...)를 사용하세요. " +
                    "상태: DRAFT(작성중), PUBLISHED(게시됨), PRIVATE(비공개), ACTIVE(활성-레거시), INACTIVE(비활성-레거시)"
    )
    @ApiResponse(responseCode = "200", description = "강의 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사이면서 강의 소유자만 수정 가능")
    @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    @CommonApiResponses
    @PutMapping("/{lectureId}")
    public ResponseEntity<CommonResponse<LectureSummaryResponse>> updateLecture(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @PathVariable Long lectureId,

            @Parameter(
                    description = "수정할 강의 메타 정보",
                    required = true,
                    schema = @Schema(implementation = LectureRequest.class)
            )
            @Valid @RequestBody LectureRequest request,

            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("강의 수정 실패 - 인증되지 않은 사용자 - lectureId: {}", lectureId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        log.info("강의 메타 정보 수정 요청 - Lecture ID: {}, User: {}", lectureId, currentUser.getEmail());

        // Service에서 비즈니스 로직 처리 (권한 검증 포함)
        LectureSummaryResponse response = lectureService.updateLecture(lectureId, request, currentUser);

        return ResponseEntity.ok(CommonResponse.success("강의 메타 정보가 성공적으로 수정되었습니다.", response));
    }

    /**
     * 강의 삭제
     * DELETE /api/lectures/{lectureId}
     *
     * 주의:
     * - 이 API는 강의와 연관된 모든 비디오, 퀴즈, 리소스를 삭제합니다.
     * - S3에 저장된 비디오 파일들도 모두 삭제됩니다.
     * - 삭제 후 복구할 수 없으므로 주의하세요.
     */
    @Operation(
            summary = "강의 삭제",
            description = "강의를 삭제합니다. 연관된 모든 비디오, 퀴즈, 리소스 및 S3 파일도 함께 삭제됩니다. " +
                    "⚠️ 삭제 후 복구할 수 없으니 주의하세요."
    )
    @ApiResponse(responseCode = "200", description = "강의 삭제 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 - 강사이면서 강의 소유자만 삭제 가능")
    @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    @CommonApiResponses
    @DeleteMapping("/{lectureId}")
    public ResponseEntity<CommonResponse<Map<String, Boolean>>> deleteLecture(
            @Parameter(description = "강의 ID", required = true, example = "1")
            @PathVariable Long lectureId,

            @AuthenticationPrincipal User currentUser) {

        // 인증 확인
        if (currentUser == null) {
            log.warn("강의 삭제 실패 - 인증되지 않은 사용자 - lectureId: {}", lectureId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("인증이 필요합니다."));
        }

        log.info("강의 삭제 요청 - Lecture ID: {}, User: {}", lectureId, currentUser.getEmail());

        // Service에서 비즈니스 로직 처리 (권한 검증 + S3 파일 삭제 포함)
        lectureService.deleteLecture(lectureId, currentUser);

        // 프론트엔드에서 요청한 { success: true } 형식으로 응답
        Map<String, Boolean> result = new HashMap<>();
        result.put("success", true);

        return ResponseEntity.ok(CommonResponse.success("강의가 성공적으로 삭제되었습니다.", result));
    }
}