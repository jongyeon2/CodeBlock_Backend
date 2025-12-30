package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.SearchCourseResponse;
import com.studyblock.domain.course.service.CourseSearchService;
import com.studyblock.global.dto.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search/courses")
@RequiredArgsConstructor
@Slf4j
public class SearchCourseController {

    private final CourseSearchService courseSearchService;

    /**
     * 테스트용 간단한 엔드포인트
     */
    @GetMapping("/test")
    @Operation(summary = "테스트 엔드포인트", description = "API 연결 테스트용")
    public ResponseEntity<CommonResponse<String>> test() {
        log.info("테스트 엔드포인트 호출");
        return ResponseEntity.ok(
                CommonResponse.success("API 연결 성공!", "Hello StudyBlock!")
        );
    }


    /**
     * 전체 강의 목록 조회 (초성 검색을 위한 전체 강의 목록 반환)
     * 프론트엔드에서 초성 필터링을 위해 모든 강의를 반환
     */
    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<SearchCourseResponse>>> getAllCourses() {
        log.info("전체 강의 목록 조회 요청");

        List<SearchCourseResponse> courses = courseSearchService.getAllCourses();

        return ResponseEntity.ok(CommonResponse.success("전체 강의 조회 성공", courses));
    }

    /**
     *  코스 검색
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<SearchCourseResponse>>> searchCourses(
            @RequestParam String keyword, //키워드
            @RequestParam(defaultValue = "0") int page, //페이지 번호
            @RequestParam(defaultValue = "10") int size //서버로부터 받아올 데이터 갯수
    ) {
        log.info("코스 검색 요청: keyword={}, page={}, size={}", keyword, page, size);

        //서비스 호출
        List<SearchCourseResponse> results = courseSearchService.searchCourses(keyword, page, size);

        return ResponseEntity.ok(CommonResponse.success("검색 성공", results));
    }

}
