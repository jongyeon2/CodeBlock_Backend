package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.*;
import com.studyblock.domain.course.enums.CoursePrerequisiteType;
import com.studyblock.domain.course.service.CourseService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course", description = "코스 관련 API")
public class CourseController {

    private final CourseService courseService;

    // ============================================
    // 코스 CRUD 엔드포인트
    // ============================================

    /**
     * 코스 생성
     * - 프론트엔드에서 전송한 데이터로 새로운 코스 생성
     */
    @PostMapping
    @Operation(
            summary = "코스 생성",
            description = "새로운 코스를 생성합니다. 카테고리 정보도 함께 저장됩니다."
    )
    @ApiResponse(responseCode = "201", description = "코스 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("코스 생성 요청: title={}, userId={}", request.getTitle(), currentUser.getId());

        // 강사 프로필 검증
        if (currentUser.getInstructorProfile() == null) {
            log.warn("강사 프로필이 없는 사용자의 코스 생성 시도 - userId: {}", currentUser.getId());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(CommonResponse.error("강사만 코스를 생성할 수 있습니다."));
        }

        Long instructorId = currentUser.getInstructorProfile().getId();
        CourseResponse response = courseService.createCourse(request, instructorId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success("코스가 성공적으로 생성되었습니다.", response));
    }

    /**
     * 모든 코스 조회
     * - 쿼리 파라미터로 공개 코스만 필터링 가능
     */
    @GetMapping
    @Operation(
            summary = "코스 목록 조회",
            description = "모든 코스 또는 공개된 코스만 조회합니다. published 파라미터로 필터링 가능합니다."
    )
    @ApiResponse(responseCode = "200", description = "코스 목록 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<CourseResponse>>> getAllCourses(
            @Parameter(description = "공개 코스만 조회 (true/false)", example = "true")
            @RequestParam(value = "published", required = false) Boolean published) {

        log.info("코스 목록 조회 요청: published={}", published);

        List<CourseResponse> responses;
        if (published != null && published) {
            responses = courseService.getPublishedCourses();
        } else {
            responses = courseService.getAllCourses();
        }

        return ResponseEntity.ok(CommonResponse.success("코스 목록 조회 성공", responses));
    }

    /**
     * 코스 수정
     * - 기존 코스 정보를 수정
     */
    @PutMapping("/{courseId}")
    @Operation(
            summary = "코스 수정",
            description = "기존 코스 정보를 수정합니다. 카테고리 정보도 함께 수정됩니다."
    )
    @ApiResponse(responseCode = "200", description = "코스 수정 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseResponse>> updateCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request) {

        log.info("코스 수정 요청: courseId={}, title={}", courseId, request.getTitle());

        CourseResponse response = courseService.updateCourse(courseId, request);

        return ResponseEntity.ok(CommonResponse.success("코스가 성공적으로 수정되었습니다.", response));
    }

    @PostMapping(value = "/{courseId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "코스 썸네일 업로드",
            description = "코스 썸네일 이미지를 업로드하고 즉시 사용 가능한 presigned URL을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "코스 썸네일 업로드 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseThumbnailResponse>> uploadCourseThumbnail(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @Parameter(description = "썸네일 이미지 파일", required = true)
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @AuthenticationPrincipal User currentUser) {

        if (thumbnail == null || thumbnail.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("업로드할 썸네일 파일이 비어 있습니다."));
        }

        CourseThumbnailResponse response = courseService.uploadCourseThumbnail(courseId, thumbnail, currentUser);
        return ResponseEntity.ok(CommonResponse.success("코스 썸네일 업로드 성공", response));
    }

    /**
     * 코스 부분 수정 (PATCH)
     * - 전달된 필드만 업데이트
     */
    @PatchMapping("/{courseId}")
    @Operation(
            summary = "코스 부분 수정",
            description = "전달된 필드만 부분 업데이트합니다(PATCH)."
    )
    @ApiResponse(responseCode = "200", description = "코스 부분 수정 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseResponse>> patchCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @RequestBody CoursePatchRequest request) {

        log.info("코스 부분 수정 요청: courseId={}", courseId);

        CourseResponse response = courseService.patchCourse(courseId, request);
        return ResponseEntity.ok(CommonResponse.success("코스가 성공적으로 부분 수정되었습니다.", response));
    }

    /**
     * 코스 삭제
     * - 코스와 연관된 모든 데이터가 CASCADE로 삭제됨
     */
    @DeleteMapping("/{courseId}")
    @Operation(
            summary = "코스 삭제",
            description = "코스를 삭제합니다. 코스와 연관된 모든 데이터(강의, 리뷰 등)도 함께 삭제됩니다."
    )
    @ApiResponse(responseCode = "200", description = "코스 삭제 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<Void>> deleteCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        log.info("코스 삭제 요청: courseId={}", courseId);

        courseService.deleteCourse(courseId);

        return ResponseEntity.ok(CommonResponse.success("코스가 성공적으로 삭제되었습니다.", null));
    }

    // ============================================
    // 특정 경로 매핑 (매핑 충돌 방지를 위해 먼저 배치)
    // ============================================

    /**
     * 내 코스 목록 조회 (강사 대시보드용)
     */
    @GetMapping("/instructor/my-courses")
    @Operation(
            summary = "내 코스 목록 조회",
            description = "현재 로그인한 강사가 만든 모든 코스를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "내 코스 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "강사 권한 필요")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<CourseResponse>>> getMyCourses(
            @AuthenticationPrincipal User currentUser) {

        // ✅ Null 체크 추가
        if (currentUser == null) {
            log.warn("인증되지 않은 사용자의 내 코스 조회 시도");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("로그인이 필요합니다."));
        }

        log.info("내 코스 목록 조회 요청: userId={}", currentUser.getId());

        // 강사 프로필 검증
        if (currentUser.getInstructorProfile() == null) {
            log.warn("강사 프로필이 없는 사용자의 내 코스 조회 시도 - userId: {}", currentUser.getId());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(CommonResponse.error("강사만 접근할 수 있습니다."));
        }

        Long instructorId = currentUser.getInstructorProfile().getId();
        List<CourseResponse> courses = courseService.getAllCoursesByInstructor(instructorId);

        return ResponseEntity.ok(CommonResponse.success("내 코스 목록 조회 성공", courses));
    }

    // ============================================
    // 코스 상세 정보 조회 엔드포인트
    // ============================================

    /**
     * 코스 기본 정보 조회 (단순 버전)
     */
    @GetMapping("/{courseId}")
    @Operation(
            summary = "코스 기본 정보 조회",
            description = "코스의 기본 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "코스 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseResponse>> getCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        log.info("코스 기본 정보 조회 요청: courseId={}", courseId);
        
        CourseResponse response = courseService.getCourse(courseId);
        return ResponseEntity.ok(CommonResponse.success("코스 정보 조회 성공", response));
    }

    /**
     * 코스 상세 정보 조회 (상세 버전)
     */
    @GetMapping("/{courseId}/detail")
    @Operation(
            summary = "코스 상세 정보 조회",
            description = "코스 상세 페이지용 상세 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "코스 상세 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CourseDetailResponse>> getCourseDetail(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            Authentication authentication) {

        log.info("코스 상세 정보 조회 요청: courseId={}", courseId);

        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userId = user.getId();
        }

        CourseDetailResponse response = courseService.getCourseDetail(courseId, userId);
        return ResponseEntity.ok(CommonResponse.success("코스 상세 정보 조회 성공", response));
    }

    /**
     * 코스에 연결된 강사 프로필 조회
     */
    @GetMapping("/{courseId}/instructor")
    @Operation(
            summary = "코스 강사 정보 조회",
            description = "코스에 연결된 강사 프로필 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "강사 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "강사 정보를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<InstructorResponse>> getInstructorByCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        InstructorResponse response = courseService.getInstructorProfileByCourseId(courseId);

        return ResponseEntity.ok(CommonResponse.success("강사 정보 조회 성공", response));
    }

    /**
     * 코스 학습 목표 조회
     */
    @GetMapping("/{courseId}/learning-outcomes")
    @Operation(
            summary = "코스 학습 목표 조회",
            description = "코스 학습 목표/성과 리스트를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "학습 목표 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<LearningOutcomeResponse>>> getLearningOutcomes(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        List<LearningOutcomeResponse> response = courseService.getLearningOutcomes(courseId);
        return ResponseEntity.ok(CommonResponse.success("학습 목표 조회 성공", response));
    }

    /**
     * 코스 FAQ 조회
     */
    @GetMapping("/{courseId}/faq")
    @Operation(
            summary = "코스 FAQ 조회",
            description = "코스 FAQ (질문/답변) 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "FAQ 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<CourseFaqResponse>>> getCourseFaqs(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        List<CourseFaqResponse> response = courseService.getCourseFaqs(courseId);
        return ResponseEntity.ok(CommonResponse.success("FAQ 조회 성공", response));
    }

    /**
     * 선수 지식/준비물 조회
     */
    @GetMapping("/{courseId}/prerequisites")
    @Operation(
            summary = "코스 선수 지식/준비물 조회",
            description = "코스 수강 전 필요한 선수 지식 또는 준비물을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "선수 지식/준비물 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<CoursePrerequisiteResponse>>> getCoursePrerequisites(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId,
            @Parameter(description = "선수 지식 유형 (REQUIRED, RECOMMENDED, MATERIAL)", example = "REQUIRED")
            @RequestParam(value = "type", required = false) CoursePrerequisiteType type) {

        List<CoursePrerequisiteResponse> response = courseService.getCoursePrerequisites(courseId, type);
        return ResponseEntity.ok(CommonResponse.success("선수 지식/준비물 조회 성공", response));
    }

    /**
     * 관련 코스 추천
     */
    @GetMapping("/{courseId}/related")
    @Operation(
            summary = "관련 코스 추천",
            description = "카테고리/난이도를 기준으로 연관 코스를 추천합니다."
    )
    @ApiResponse(responseCode = "200", description = "관련 코스 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<RelatedCourseResponse>>> getRelatedCourses(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        List<RelatedCourseResponse> response = courseService.getRelatedCourses(courseId);
        return ResponseEntity.ok(CommonResponse.success("관련 코스 조회 성공", response));
    }

    /**
     * 코스에 포함된 강의 요약 목록 조회
     */
    @GetMapping("/{courseId}/lectures")
    @Operation(
            summary = "코스 강의 목록 조회",
            description = "코스에 포함된 강의의 요약 정보를 순서대로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "강의 목록 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<LectureSummaryResponse>>> getLecturesByCourse(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        List<LectureSummaryResponse> response = courseService.getLectureSummariesByCourseId(courseId);

        return ResponseEntity.ok(CommonResponse.success("강의 목록 조회 성공", response));
    }

    /**
     * 코스의 무료 미리보기 강의 목록 조회
     */
    @GetMapping("/{courseId}/free-preview-lectures")
    @Operation(
            summary = "무료 미리보기 강의 목록 조회",
            description = "코스에 포함된 무료 강의(isFree=true) 목록을 조회합니다. 코스 소개 페이지에서 미리보기용으로 사용됩니다."
    )
    @ApiResponse(responseCode = "200", description = "무료 미리보기 강의 목록 조회 성공")
    @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<LectureSummaryResponse>>> getFreePreviewLectures(
            @Parameter(description = "코스 ID", required = true, example = "1")
            @PathVariable Long courseId) {

        List<LectureSummaryResponse> response = courseService.getFreePreviewLectures(courseId);

        return ResponseEntity.ok(CommonResponse.success("무료 미리보기 강의 목록 조회 성공", response));
    }
}
