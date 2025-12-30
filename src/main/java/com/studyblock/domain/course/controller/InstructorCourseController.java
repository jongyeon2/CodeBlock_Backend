package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.RelatedCourseResponse;
import com.studyblock.domain.course.service.CourseService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Instructor", description = "강사 관련 API")
public class InstructorCourseController {

    private final CourseService courseService;

    /**
     * 강사의 다른 코스 목록 조회
     */
    @GetMapping("/{instructorId}/courses")
    @Operation(
            summary = "강사의 코스 목록 조회",
            description = "강사가 진행 중인 다른 코스 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "강사 코스 조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<List<RelatedCourseResponse>>> getInstructorCourses(
            @Parameter(description = "강사 프로필 ID", required = true, example = "1")
            @PathVariable("instructorId") Long instructorId) {

        List<RelatedCourseResponse> response = courseService.getCoursesByInstructor(instructorId);
        return ResponseEntity.ok(CommonResponse.success("강사 코스 조회 성공", response));
    }
}
