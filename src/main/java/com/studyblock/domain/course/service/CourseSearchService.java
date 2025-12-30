package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.SearchCourseResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseSearchRepository;
import com.studyblock.domain.course.repository.CourseReviewRepository;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseSearchService {

    private final CourseSearchRepository courseSearchRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final S3StorageService s3StorageService;

    private static final int COURSE_IMAGE_URL_EXPIRATION_MINUTES = 30;


    /**
     *   코스 검색
     * - 제목 기준으로 LIKE 검색
     */
    public List<SearchCourseResponse> searchCourses(String keyword, int page, int size) {

        //페이징 처리를 위해 Pageable 인터페이스 사용
        Pageable pageable = (Pageable) PageRequest.of(page, size);

        // Repository에서 코스 목록 검색
        List<Course> courses = courseSearchRepository.searchByKeyword(keyword, pageable);

        // 각 강의에 평점 정보 설정
        courses.forEach(course -> {
            Double avgRating = courseReviewRepository.findAverageRatingByCourseId(course.getId());
            Long reviewCount = courseReviewRepository.countByCourseId(course.getId());

            course.setReviewInfo(
                avgRating != null ? avgRating : 0.0,
                reviewCount
            );
        });

        // Course 엔티티 → SearchCourseResponse DTO로 변환
        return courses.stream()
                .map(SearchCourseResponse::from)
                .peek(this::applyCourseThumbnail)
                .toList();
    }

    /**
     * 전체 강의 목록 조회 (초성 검색용)
     */
    public List<SearchCourseResponse> getAllCourses() {
        log.info("전체 강의 목록 조회 (초성 검색용)");
        List<Course> courses = courseSearchRepository.findAllWithInstructor();

        // 각 강의에 평점 정보 설정
        courses.forEach(course -> {
            Double avgRating = courseReviewRepository.findAverageRatingByCourseId(course.getId());
            Long reviewCount = courseReviewRepository.countByCourseId(course.getId());

            course.setReviewInfo(
                    avgRating != null ? avgRating : 0.0,
                    reviewCount
            );
        });

        return courses.stream()
                .map(SearchCourseResponse::from)
                .peek(this::applyCourseThumbnail)
                .toList();
    }

    private void applyCourseThumbnail(SearchCourseResponse response) {
        if (response == null) {
            return;
        }

        String original = response.getThumbnailOriginalUrl();
        if (original == null || original.isBlank()) {
            original = response.getThumbnailUrl();
            response.setThumbnailOriginalUrl(original);
        }

        response.setThumbnailUrl(generateSignedUrl(original));
    }

    private String generateSignedUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            return null;
        }
        try {
            return s3StorageService.generatePresignedUrl(originalUrl, COURSE_IMAGE_URL_EXPIRATION_MINUTES);
        } catch (RuntimeException e) {
            log.warn("검색용 코스 썸네일 presigned URL 생성 실패 - url: {}", originalUrl, e);
            return null;
        }
    }
}
