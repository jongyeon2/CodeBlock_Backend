package com.studyblock.domain.enrollment.controller;

import com.studyblock.domain.enrollment.dto.EnrollmentResponse;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.service.EnrollmentService;
import com.studyblock.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.Map;

/**
 * 수강신청 컨트롤러
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment", description = "수강신청 API")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * 내 수강 목록 조회
     */
    @GetMapping("/my-courses")
    @Operation(summary = "내 수강 목록 조회", description = "현재 사용자의 수강 중인 강좌 목록을 조회합니다.")
    public ResponseEntity<Page<EnrollmentResponse>> getMyEnrollments(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "lastAccessedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CourseEnrollment> enrollments = enrollmentService.getMyEnrollments(user.getId(), pageable);
        Page<EnrollmentResponse> response = enrollments.map(EnrollmentResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 수강신청 상세 조회
     */
    @GetMapping("/{enrollmentId}")
    @Operation(summary = "수강신청 상세 조회", description = "특정 수강신청의 상세 정보를 조회합니다.")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal User user
    ) {
        CourseEnrollment enrollment = enrollmentService.getEnrollment(enrollmentId, user.getId());
        List<Long> completedLectureIds = enrollmentService.getCompletedLectureIds(enrollment.getId());
        return ResponseEntity.ok(EnrollmentResponse.from(enrollment, completedLectureIds));
    }

    /**
     * 강좌 수강신청 (무료 강좌 전용)
     */
    @PostMapping("/courses/{courseId}/enroll")
    @Operation(summary = "강좌 수강신청", description = "무료 강좌에 수강신청합니다. 유료 강좌는 결제가 필요합니다.")
    public ResponseEntity<EnrollmentResponse> enrollCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user
    ) {
        CourseEnrollment enrollment = enrollmentService.enrollFreeCourse(user.getId(), courseId);
        return ResponseEntity.ok(EnrollmentResponse.from(enrollment));
    }

    /**
     * 특정 강좌 수강 여부 확인
     */
    @GetMapping("/courses/{courseId}/is-enrolled")
    @Operation(summary = "수강 여부 확인", description = "사용자가 특정 강좌를 수강 중인지 확인합니다.")
    public ResponseEntity<Boolean> isEnrolled(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user
    ) {
        // 인증되지 않은 사용자는 false 반환
        if (user == null) {
            log.warn("인증되지 않은 사용자의 수강 여부 확인 요청 - courseId: {}", courseId);
            return ResponseEntity.ok(false);
        }
        boolean enrolled = enrollmentService.isEnrolled(user.getId(), courseId);
        return ResponseEntity.ok(enrolled);
    }

    /**
     * 특정 강좌 수강신청 정보 조회
     */
    @GetMapping("/courses/{courseId}")
    @Operation(summary = "강좌 수강신청 정보 조회", description = "사용자의 특정 강좌 수강신청 정보를 조회합니다.")
    public ResponseEntity<EnrollmentResponse> getEnrollmentByUserAndCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user
    ) {
        EnrollmentResponse response = enrollmentService.getEnrollmentByUserAndCourse(user.getId(), courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내가 구매한 모든 섹션 목록 조회 (코스 필터 없음)
     */
    @GetMapping("/users/me/purchased-sections")
    @Operation(summary = "구매한 섹션 목록 조회", description = "사용자가 구매한 모든 섹션 ID 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getMyPurchasedSections(
            @AuthenticationPrincipal User user
    ) {
        List<Long> purchasedSectionIds = enrollmentService.getAllPurchasedSectionIds(user.getId());

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "purchasedSectionIds", purchasedSectionIds
        ));
    }

    /**
     * 수강신청 취소 (환불)
     */
    @DeleteMapping("/{enrollmentId}")
    @Operation(summary = "수강신청 취소", description = "수강신청을 취소하고 환불을 요청합니다.")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal User user
    ) {
        CourseEnrollment enrollment = enrollmentService.cancelEnrollment(enrollmentId, user.getId());
        return ResponseEntity.ok(EnrollmentResponse.from(enrollment));
    }

    /**
     * 수강신청 진도율 업데이트 (시스템용)
     */
    @PutMapping("/{enrollmentId}/progress")
    @Operation(summary = "진도율 업데이트", description = "수강신청의 진도율을 재계산합니다.")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal User user
    ) {
        enrollmentService.updateEnrollmentProgress(enrollmentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 통계 조회
     */
    @GetMapping("/stats")
    @Operation(summary = "통계 조회", description = "사용자의 수강신청 통계를 조회합니다.")
    public ResponseEntity<EnrollmentService.EnrollmentStats> getMyStats(
            @AuthenticationPrincipal User user
    ) {
        EnrollmentService.EnrollmentStats stats = enrollmentService.getUserStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}
