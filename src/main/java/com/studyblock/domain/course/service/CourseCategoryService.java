package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.SearchCourseResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.CourseCategory;
import com.studyblock.domain.course.repository.CourseCategoryRepository;
import com.studyblock.domain.course.repository.CourseReviewRepository;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseCategoryService {

    private final CourseCategoryRepository courseCategoryRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final S3StorageService s3StorageService;

    private static final int COURSE_IMAGE_URL_EXPIRATION_MINUTES = 30;

    //카테고리별 강의 조회
    public Page<SearchCourseResponse> getCoursesByCategory(Long categoryId, int page, int size) {

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository를 통해 CourseCategory → Course 조회
        // CourseCategory의 category.id가 일치하는 Course만 선택
        Page<Course> coursePage = courseCategoryRepository.findCoursesByCategoryId(categoryId, pageable);

        // Entity → DTO 변환 (SearchCourseResponse.from(course))
        // Page.map()을 사용하면 Stream처럼 DTO로 한 번에 변환 가능
        // 각 강의에 평점 정보 설정
        coursePage.forEach(course -> {
            Double avgRating = courseReviewRepository.findAverageRatingByCourseId(course.getId());
            Long reviewCount = courseReviewRepository.countByCourseId(course.getId());

            //평점과 인원수
            course.setReviewInfo(
                avgRating != null ? avgRating : 0.0,
                reviewCount
            );
        });

        return coursePage.map(course -> {
            SearchCourseResponse response = SearchCourseResponse.from(course);
            applyCourseThumbnail(response);  // 썸네일 URL 변환 추가
            return response;
        });
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