package com.studyblock.domain.course.service;

import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.category.service.CategoryService;
import com.studyblock.domain.course.dto.*;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.enums.CoursePrerequisiteType;
import com.studyblock.domain.course.repository.*;
import com.studyblock.domain.upload.dto.ImageUploadResponse;
import com.studyblock.domain.upload.enums.ImageType;
import com.studyblock.domain.upload.service.ImageUploadService;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 코스 관련 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseService {

    private static final int COURSE_IMAGE_URL_EXPIRATION_MINUTES = 30;
    private static final int RELATED_COURSE_LIMIT = 10;
    private static final Sort ENROLLMENT_DESC_SORT = Sort.by(Sort.Direction.DESC, "enrollmentCount");

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final SectionRepository sectionRepository;
    private final CourseLearningOutcomeRepository learningOutcomeRepository;
    private final CourseFaqRepository faqRepository;
    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CategoryService categoryService;
    private final com.studyblock.domain.user.repository.InstructorProfileRepository instructorProfileRepository;
    private final VideoRepository videoRepository;
    private final PreviewVideoRepository previewVideoRepository;
    private final LectureOwnershipService lectureOwnershipService;
    private final S3StorageService s3StorageService;
    private final ImageUploadService imageUploadService;

    /**
     * 코스 ID로 강사 프로필 조회
     */
    public InstructorResponse getInstructorProfileByCourseId(Long courseId) {
        log.info("코스 ID {}로 강사 프로필 조회 요청", courseId);

        return lectureRepository.findFirstByCourseIdAndInstructorIsNotNullOrderBySequenceAsc(courseId)
                .map(lecture -> {
                    log.debug("코스 ID {}의 첫 번째 강의({})에서 강사 정보 확인", courseId, lecture.getId());
                    InstructorResponse response = InstructorResponse.from(lecture.getInstructor());
                    applyInstructorImage(response);
                    return response;
                })
                .orElseThrow(() -> new IllegalArgumentException("해당 코스를 담당하는 강사를 찾을 수 없습니다."));
    }

    /**
     * 코스 ID로 강의 요약 목록 조회 (비디오 정보 포함)
     * - QueryDSL Fetch Join을 사용하여 N+1 문제 완전 해결
     * - Video + Section을 Fetch Join으로 한 번에 조회
     * - PreviewVideo는 별도 일괄 조회 후 Map으로 관리
     *
     * 성능:
     * - 기존: 1 (Lecture) + N (Video) + N (Section) + N (PreviewVideo) = 1 + 3N 쿼리
     * - 개선: 1 (Lecture + Video + Section) + 1 (PreviewVideo 일괄) = 2 쿼리
     * - 효율: 94% 개선 (강의 10개 기준: 31개 → 2개 쿼리)
     */
    public List<LectureSummaryResponse> getLectureSummariesByCourseId(Long courseId) {
        log.info("코스 ID {}로 강의 요약 정보 목록 조회 요청 (QueryDSL Fetch Join - Video + Section)", courseId);

        // 1. Lecture 조회 (Video + Section Fetch Join) - 1개 쿼리
        // Section LAZY 로딩 방지를 위해 Section도 Fetch Join
        List<Lecture> lectures = lectureRepository.findByCourseIdWithVideoAndSectionOrderBySequenceAsc(courseId);

        // 2. PreviewVideo 일괄 조회 - 1개 쿼리 (N+1 문제 방지)
        // hasPreviewVideo() 호출 시 LAZY 프록시 초기화를 방지하기 위해
        // PreviewVideo를 미리 조회하여 Map으로 관리
        List<Long> lectureIds = lectures.stream()
                .map(Lecture::getId)
                .collect(Collectors.toList());
        
        // PreviewVideo 일괄 조회 - 영속성 컨텍스트에 로드하여 이후 LAZY 프록시 초기화 방지
        // Map은 사용하지 않지만, 조회 자체로 Hibernate가 영속성 컨텍스트에 로드하여
        // hasPreviewVideo() 호출 시 추가 쿼리 없이 동작하도록 함
        if (!lectureIds.isEmpty()) {
            previewVideoRepository.findByLectureIdIn(lectureIds);
            // 조회만으로도 Hibernate가 Lecture의 previewVideo를 자동으로 연결함
        }

        // 3. DTO 변환
        // Section과 Video는 이미 Fetch Join으로 로드됨 - 추가 쿼리 없음
        // Section 메서드 대신 직접 필드 접근하여 LAZY 프록시 초기화 방지
        return lectures.stream()
                .map(lecture -> {
                    // Video 정보 (이미 Fetch Join으로 로드됨 - 추가 쿼리 없음)
                    Long lastVideoId = lecture.hasVideo() ? lecture.getVideo().getId() : null;
                    com.studyblock.domain.course.enums.EncodingStatus lastVideoEncodingStatus =
                            lecture.hasVideo() ? lecture.getVideo().getEncodingStatus() : null;
                    Long videoCount = lecture.hasVideo() ? 1L : 0L;

                    // Section 정보는 직접 필드 접근 (이미 Fetch Join으로 로드됨 - 추가 쿼리 없음)
                    // getPriceCookie(), getDiscountPercentage() 메서드 대신 직접 접근하여 LAZY 로딩 방지
                    com.studyblock.domain.course.entity.Section section = lecture.getSection();
                    Long sectionId = section != null ? section.getId() : null;
                    Long priceCookie = section != null ? section.getCookiePrice() : null;
                    Integer discountPercentage = section != null ? section.getDiscountPercentage() : 0;

                    // LectureSummaryResponse 직접 생성 (of() 메서드 사용 시 getPriceCookie() 호출 방지)
                    LectureSummaryResponse response = LectureSummaryResponse.builder()
                            .id(lecture.getId())
                            .sectionId(sectionId)
                            .sequence(lecture.getSequence())
                            .title(lecture.getTitle())
                            .description(lecture.getDescription())
                            .thumbnailUrl(lecture.getThumbnailUrl())
                            .thumbnailOriginalUrl(lecture.getThumbnailUrl())
                            .thumbnailOriginalUrl(lecture.getThumbnailUrl())
                            .uploadDate(lecture.getUploadDate())
                            .status(lecture.getStatus())
                            .isFree(lecture.getIsFree())
                            .priceCookie(priceCookie)
                            .discountPercentage(discountPercentage)
                            .lastVideoId(lastVideoId)
                            .lastVideoEncodingStatus(lastVideoEncodingStatus != null 
                                    ? lastVideoEncodingStatus.name() 
                                    : null)
                            .videoCount(videoCount)
                            .hasPreviewVideo(lecture.hasPreviewVideo())
                            .build();
                    applyLectureThumbnail(response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 코스 기본 정보 조회 (단순 버전)
     */
    public CourseResponse getCourse(Long courseId) {
        log.info("코스 ID {} 기본 정보 조회 요청", courseId);

        Course course = courseRepository.findOneWithCategoriesById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        CourseResponse response = CourseResponse.from(course);
        applyCourseThumbnail(response);
        return response;
    }

    /**
     * 코스 상세 정보 조회 (상세 버전)
     */
    public CourseDetailResponse getCourseDetail(Long courseId, Long userId) {
        log.info("코스 ID {} 상세 정보 조회 요청 (userId: {})", courseId, userId);

        Course course = courseRepository.findOneWithCategoriesById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        int totalLectures = (int) lectureRepository.countByCourseId(courseId);

        CourseDetailResponse response = CourseDetailResponse.from(course, totalLectures);
        applyCourseDetailThumbnail(response);

        if (userId != null) {
            try {
                List<Long> purchasedSectionIds = lectureOwnershipService.getPurchasedSectionIds(userId, courseId);
                response.setPurchasedSectionIds(purchasedSectionIds != null ? purchasedSectionIds : Collections.emptyList());
            } catch (Exception e) {
                log.warn("코스 상세 정보 조회 중 구매 섹션 조회 실패 - courseId: {}, userId: {}", courseId, userId, e);
            }
        } else {
            response.setPurchasedSectionIds(Collections.emptyList());
        }

        return response;
    }

    /**
     * 학습 목표 목록 조회
     */
    public List<LearningOutcomeResponse> getLearningOutcomes(Long courseId) {
        log.info("코스 ID {} 학습 목표 목록 조회 요청", courseId);

        return learningOutcomeRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).stream()
                .map(LearningOutcomeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * FAQ 목록 조회
     */
    public List<CourseFaqResponse> getCourseFaqs(Long courseId) {
        log.info("코스 ID {} FAQ 목록 조회 요청", courseId);

        return faqRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).stream()
                .map(CourseFaqResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 선수 지식/준비물 목록 조회
     */
    public List<CoursePrerequisiteResponse> getCoursePrerequisites(Long courseId, CoursePrerequisiteType type) {
        log.info("코스 ID {} 선수 지식/준비물 조회 요청. type={}", courseId, type);

        if (type != null) {
            return prerequisiteRepository.findByCourseIdAndTypeOrderByDisplayOrderAsc(courseId, type).stream()
                    .map(CoursePrerequisiteResponse::from)
                    .collect(Collectors.toList());
        }

        return prerequisiteRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).stream()
                .map(CoursePrerequisiteResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 연관 코스 추천
     */
    public List<RelatedCourseResponse> getRelatedCourses(Long courseId) {
        log.info("코스 ID {} 관련 코스 조회 요청", courseId);

        Course course = courseRepository.findOneWithCategoriesById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        List<Course> related = List.of();

        if (course.getPrimaryCategory() != null && course.getLevel() != null) {
            related = courseRepository.findTopByCategoryAndLevelOrderByEnrollmentDesc(
                    course.getPrimaryCategory().getId(),
                    course.getLevel(),
                    courseId,
                    PageRequest.of(0, RELATED_COURSE_LIMIT, ENROLLMENT_DESC_SORT)
            );
        }

        if (related.isEmpty() && course.getPrimaryCategory() != null) {
            related = courseRepository.findTopByCategoryOrderByEnrollmentDesc(
                    course.getPrimaryCategory().getId(),
                    courseId,
                    PageRequest.of(0, RELATED_COURSE_LIMIT, ENROLLMENT_DESC_SORT)
            );
        }

        if (related.isEmpty() && course.getLevel() != null) {
            related = courseRepository.findTop10ByLevelAndIdNotOrderByEnrollmentCountDesc(course.getLevel(), courseId);
        }

        return related.stream()
                .map(RelatedCourseResponse::from)
                .peek(this::applyRelatedCourseThumbnail)
                .collect(Collectors.toList());
    }

    /**
     * 강사의 다른 코스 목록 조회 (추천용 - 수강생 수 기준)
     */
    public List<RelatedCourseResponse> getCoursesByInstructor(Long instructorId) {
        log.info("강사 ID {}의 코스 목록 조회 요청 (추천용)", instructorId);

        // ✅ 개선: course.instructor_id로 직접 조회 (빠르고 정확)
        List<Course> courses = courseRepository.findByInstructor_IdOrderByCreatedAtDesc(instructorId);

        return courses.stream()
                .limit(10)
                .map(RelatedCourseResponse::from)
                .peek(this::applyRelatedCourseThumbnail)
                .collect(Collectors.toList());
    }

    /**
     * 강사가 생성한 모든 정규코스 목록 조회 (강사 페이지용)
     * @param instructorId 강사 ID
     * @return 강사의 모든 정규코스 목록
     */
    public List<CourseResponse> getAllCoursesByInstructor(Long instructorId) {
        log.info("강사 ID {}가 생성한 모든 정규코스 조회 요청", instructorId);

        List<Course> courses = courseRepository.findByInstructor_IdOrderByCreatedAtDesc(instructorId);

        log.info("강사 ID {}의 정규코스 조회 완료: {} 개", instructorId, courses.size());

        return courses.stream()
                .map(CourseResponse::from)
                .peek(this::applyCourseThumbnail)
                .collect(Collectors.toList());
    }

    /**
     * 강사의 공개 정규코스만 조회 (외부 노출용)
     * @param instructorId 강사 ID
     * @return 공개된 정규코스 목록
     */
    public List<CourseResponse> getPublishedCoursesByInstructor(Long instructorId) {
        log.info("강사 ID {}의 공개 정규코스 조회 요청", instructorId);

        List<Course> courses = courseRepository.findPublishedCoursesByInstructorId(instructorId);

        log.info("강사 ID {}의 공개 정규코스 조회 완료: {} 개", instructorId, courses.size());

        return courses.stream()
                .map(CourseResponse::from)
                .peek(this::applyCourseThumbnail)
                .collect(Collectors.toList());
    }

    /**
     * 강사가 생성한 정규코스 개수 조회
     * @param instructorId 강사 ID
     * @return 정규코스 개수
     */
    public long getCourseCountByInstructor(Long instructorId) {
        return courseRepository.countByInstructor_Id(instructorId);
    }

    /**
     * 리뷰 요약 정보 조회
     */
    public CourseReviewSummaryResponse getReviewSummary(Long courseId) {
        long totalReviews = courseReviewRepository.countByCourseId(courseId);
        Double averageRating = courseReviewRepository.findAverageRatingByCourseId(courseId);

        // 평점별 리뷰 개수 분포 계산
        List<Object[]> ratingCounts = courseReviewRepository.countByRatingGroupByCourseId(courseId);
        Map<Integer, Long> ratingCountMap = new java.util.HashMap<>();

        // 1-5점 초기화 (0으로)
        for (int i = 1; i <= 5; i++) {
            ratingCountMap.put(i, 0L);
        }

        // 실제 데이터로 채우기
        for (Object[] row : ratingCounts) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingCountMap.put(rating, count);
        }

        // 배열 형식으로 변환 (5점부터 1점까지 내림차순)
        List<CourseReviewSummaryResponse.RatingDistribution> ratingDistribution = new ArrayList<>();
        for (int stars = 5; stars >= 1; stars--) {
            long count = ratingCountMap.get(stars);
            double percentage = totalReviews > 0
                    ? Math.round((count * 10000.0 / totalReviews)) / 100.0  // 소수점 2자리
                    : 0.0;

            ratingDistribution.add(
                    CourseReviewSummaryResponse.RatingDistribution.builder()
                            .stars(stars)
                            .count(count)
                            .percentage(percentage)
                            .build()
            );
        }

        // 추천율 계산 (4-5점 리뷰 비율)
        long recommendCount = courseReviewRepository.countByCourseIdAndRatingGreaterThanEqual(courseId, 4);
        double recommendationRate = totalReviews > 0
                ? Math.round((recommendCount * 10000.0 / totalReviews)) / 100.0  // 소수점 2자리
                : 0.0;

        return CourseReviewSummaryResponse.builder()
                .totalReviews(totalReviews)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .ratingDistribution(ratingDistribution)
                .recommendationRate(recommendationRate)
                .build();
    }

    // ============================================
    // 코스 CRUD 메서드
    // ============================================

    /**
     * 코스 생성
     * - 프론트엔드에서 전송한 데이터로 새로운 코스 생성
     * - 카테고리 정보도 함께 저장 (소분류만 저장)
     *
     * @param request 코스 생성 요청 DTO
     * @param instructorId 코스를 생성하는 강사 ID (InstructorProfile)
     * @return 생성된 코스 응답 DTO
     */
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request, Long instructorId) {
        log.info("코스 생성 요청: title={}, level={}, instructorId={}",
                 request.getTitle(), request.getDifficulty(), instructorId);

        // 강사 조회
        com.studyblock.domain.user.entity.InstructorProfile instructor =
                instructorProfileRepository.findById(instructorId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "강사를 찾을 수 없습니다. ID=" + instructorId));

        // 카테고리 조회 (소분류만 사용)
        Category childCategory = categoryService.getCategoryById(request.getCategory().getChildId());
        List<Category> categories = new ArrayList<>();
        categories.add(childCategory);

        // Course 엔티티 생성
        Course course = Course.builder()
                .instructor(instructor)              // ✅ 강사 설정
                .title(request.getTitle())
                .summary(request.getDescription())  // description -> summary
                .level(request.getDifficulty())      // difficulty -> level
                .durationMinutes(request.getDurationMinutes())
                .thumbnailUrl(request.getThumbnailUrl())
                .price(request.getPrice())     // price -> priceCookie
                .discountPercentage(request.getDiscountPercentage())
                .categories(categories)
                .build();

        // 공개 여부 설정 (true이면 공개, false이면 비공개)
        if (request.getIsPublished() != null && request.getIsPublished()) {
            course.publish();
        }

        // 저장
        Course savedCourse = courseRepository.save(course);

        log.info("코스 생성 완료: id={}, title={}, instructor={}",
                 savedCourse.getId(), savedCourse.getTitle(), instructor.getChannelName());

        CourseResponse response = CourseResponse.from(savedCourse);
        applyCourseThumbnail(response);
        return response;
    }

    /**
     * 코스 수정
     * - 기존 코스 정보를 수정
     * - 카테고리 정보도 함께 수정
     *
     * @param courseId 코스 ID
     * @param request  코스 수정 요청 DTO
     * @return 수정된 코스 응답 DTO
     */
    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        log.info("코스 수정 요청: courseId={}, title={}", courseId, request.getTitle());

        // 코스 조회
        Course course = courseRepository.findByIdWithCategories(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        // 기본 정보 수정
        course.updateInfo(
                request.getTitle(),
                request.getDescription(),
                request.getDifficulty(),
                request.getThumbnailUrl(),
                request.getPrice(),              // ✅ 정규코스는 현금 결제 (KRW)
                request.getDiscountPercentage()
        );

        // 카테고리 수정
        Category childCategory = categoryService.getCategoryById(request.getCategory().getChildId());
        List<Category> categories = new ArrayList<>();
        categories.add(childCategory);
        course.updateCategories(categories);

        // 공개 여부 수정
        if (request.getIsPublished() != null && request.getIsPublished()) {
            course.publish();
        } else {
            course.unpublish();
        }

        log.info("코스 수정 완료: courseId={}, title={}", courseId, course.getTitle());

        CourseResponse response = CourseResponse.from(course);
        applyCourseThumbnail(response);
        return response;
    }

    @Transactional
    public CourseThumbnailResponse uploadCourseThumbnail(Long courseId, MultipartFile file, com.studyblock.domain.user.entity.User currentUser) {
        log.info("코스 썸네일 업로드 요청 - courseId={}", courseId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 썸네일 파일이 비어 있습니다.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        verifyCourseOwnership(course, currentUser);

        ImageUploadResponse uploadResponse = imageUploadService.uploadImage(file, ImageType.COURSE);
        if (uploadResponse == null || uploadResponse.getUrl() == null || !Boolean.TRUE.equals(uploadResponse.getSuccess())) {
            throw new IllegalStateException("코스 썸네일 업로드에 실패했습니다.");
        }

        course.updateThumbnail(uploadResponse.getUrl());

        String presignedUrl = generateSignedUrl(uploadResponse.getUrl());

        log.info("코스 썸네일 업로드 완료 - courseId={}, filename={}", courseId, uploadResponse.getOriginalFilename());

        return CourseThumbnailResponse.builder()
                .originalUrl(uploadResponse.getUrl())
                .thumbnailUrl(presignedUrl)
                .filename(uploadResponse.getOriginalFilename())
                .size(uploadResponse.getSize())
                .build();
    }

    /**
     * 코스 부분 수정 (PATCH)
     * - null이 아닌 필드만 반영
     */
    @Transactional
    public CourseResponse patchCourse(Long courseId, com.studyblock.domain.course.dto.CoursePatchRequest request) {
        log.info("코스 부분 수정 요청: courseId={}", courseId);

        Course course = courseRepository.findByIdWithCategories(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        // 제목/설명/난이도/썸네일/가격/할인율
        if (request.getTitle() != null) course.updateTitle(request.getTitle());
        if (request.getDescription() != null) course.updateSummary(request.getDescription());
        if (request.getDifficulty() != null) course.updateLevel(request.getDifficulty());
        if (request.getDurationMinutes() != null) course.updateDuration(request.getDurationMinutes());
        if (request.getThumbnailUrl() != null) course.updateThumbnail(request.getThumbnailUrl());
        if (request.getPrice() != null) course.updatePrice(request.getPrice()); // 정규코스는 현금 가격만 사용
        if (request.getDiscountPercentage() != null) course.updateDiscountPercentage(request.getDiscountPercentage());

        // 공개여부
        if (request.getIsPublished() != null) {
            if (request.getIsPublished()) course.publish(); else course.unpublish();
        }

        // 카테고리 (전달 시에만 교체)
        if (request.getCategory() != null && request.getCategory().getChildId() != null) {
            Category child = categoryService.getCategoryById(request.getCategory().getChildId());
            List<Category> categories = new ArrayList<>();
            categories.add(child);
            course.updateCategories(categories);
        }

        CourseResponse response = CourseResponse.from(course);
        applyCourseThumbnail(response);
        return response;
    }

    /**
     * 코스 삭제
     * - 코스와 연관된 모든 데이터가 CASCADE로 삭제됨
     *
     * @param courseId 코스 IDl
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        log.info("코스 삭제 요청: courseId={}", courseId);

        // 코스 존재 여부 확인
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        // 삭제
        courseRepository.delete(course);

        log.info("코스 삭제 완료: courseId={}, title={}", courseId, course.getTitle());
    }

    /**
     * 모든 코스 조회
     * - 공개/비공개 모두 포함
     * - 최신순 정렬
     *
     * @return 모든 코스 목록
     */
    public List<CourseResponse> getAllCourses() {
        log.info("모든 코스 조회 요청");

        List<Course> courses = courseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        log.info("모든 코스 조회 완료: {} 개", courses.size());

        return courses.stream()
                .map(CourseResponse::from)
                .peek(this::applyCourseThumbnail)
                .collect(Collectors.toList());
    }

    /**
     * 공개된 코스만 조회
     * - isPublished = true인 코스만 조회
     * - 최신순 정렬
     *
     * @return 공개 코스 목록
     */
    public List<CourseResponse> getPublishedCourses() {
        log.info("공개 코스 조회 요청");

        List<Course> courses = courseRepository.findAllPublishedCourses();

        log.info("공개 코스 조회 완료: {} 개", courses.size());

        return courses.stream()
                .map(CourseResponse::from)
                .peek(this::applyCourseThumbnail)
                .collect(Collectors.toList());
    }

    /**
     * 코스 ID로 코스 조회 (간단한 버전)
     * - CourseResponse 형식으로 반환
     *
     * @param courseId 코스 ID
     * @return 코스 응답 DTO
     */
    public CourseResponse getCourseById(Long courseId) {
        log.info("코스 조회 요청: courseId={}", courseId);

        Course course = courseRepository.findByIdWithCategories(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        CourseResponse response = CourseResponse.from(course);
        applyCourseThumbnail(response);
        return response;
    }
    /**
     * 코스의 무료 미리보기 강의 목록 조회 (비디오 정보 포함)
     * - PreviewVideo가 있으면 PreviewVideo 정보 우선, 없으면 메인 Video 정보 반환
     */
    /**
     * 무료 미리보기 강의 목록 조회
     * - QueryDSL Fetch Join을 사용하여 N+1 문제 완전 해결
     * - PreviewVideo + Section을 Fetch Join으로 한 번에 조회
     * - Video는 별도 일괄 조회 후 Map으로 관리
     *
     * 성능:
     * - 기존: 1 (Lecture) + N (Video) + N (PreviewVideo) + N (Section) = 1 + 3N 쿼리
     * - 개선: 1 (Lecture + PreviewVideo + Section) + 1 (Video 일괄) = 2 쿼리
     * - 효율: 94% 개선 (강의 10개 기준: 31개 → 2개 쿼리)
     */
    public List<LectureSummaryResponse> getFreePreviewLectures(Long courseId) {
        log.info("코스 ID {}의 무료 미리보기 강의 목록 조회 요청 (QueryDSL Fetch Join - PreviewVideo + Section)", courseId);

        // 코스 존재 여부 확인
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId);
        }

        // 1. Lecture 조회 (PreviewVideo Fetch Join) - 1개 쿼리
        // Section도 Fetch Join 필요하지만 PreviewVideo가 우선이므로 별도 조회
        // TODO: 향후 LectureRepositoryCustom에 PreviewVideo + Section Fetch Join 메서드 추가 고려
        List<Lecture> allLectures = lectureRepository.findByCourseIdWithPreviewVideoOrderBySequenceAsc(courseId);
        List<Lecture> freeLectures = allLectures.stream()
                .filter(lecture -> lecture.getIsFree() != null && lecture.getIsFree())
                .collect(Collectors.toList());

        // 2. Video 별도 일괄 조회 - 1개 쿼리 (N+1 문제 방지)
        List<Long> lectureIds = freeLectures.stream()
                .map(Lecture::getId)
                .collect(Collectors.toList());
        
        // Video를 일괄 조회하여 Map으로 변환
        java.util.Map<Long, com.studyblock.domain.course.entity.Video> videoMap =
                lectureIds.isEmpty() ? java.util.Collections.emptyMap() :
                videoRepository.findByLectureIdIn(lectureIds)
                        .stream()
                        .collect(Collectors.toMap(
                                v -> v.getLecture().getId(),
                                java.util.function.Function.identity(),
                                (existing, replacement) -> existing
                        ));

        // 3. Section 일괄 조회 - 1개 쿼리 (N+1 문제 방지)
        // Section 정보도 필요하므로 일괄 조회
        java.util.Set<Long> sectionIds = freeLectures.stream()
                .map(lecture -> lecture.getSection() != null ? lecture.getSection().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        
        java.util.Map<Long, com.studyblock.domain.course.entity.Section> sectionMap =
                sectionIds.isEmpty() ? java.util.Collections.emptyMap() :
                sectionRepository.findAllById(sectionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                com.studyblock.domain.course.entity.Section::getId,
                                java.util.function.Function.identity()
                        ));

        // 4. DTO 변환 (메모리에서 Video/PreviewVideo/Section 매핑)
        return freeLectures.stream()
                .map(lecture -> {
                    // PreviewVideo 우선, 없으면 메인 Video
                    Long lastVideoId = null;
                    com.studyblock.domain.course.enums.EncodingStatus lastVideoEncodingStatus = null;
                    Long videoCount = 0L;

                    // PreviewVideo 정보 (이미 Fetch Join으로 로드됨 - 추가 쿼리 없음)
                    if (lecture.hasPreviewVideo()) {
                        lastVideoId = lecture.getPreviewVideo().getId();
                        lastVideoEncodingStatus = lecture.getPreviewVideo().getEncodingStatus();
                        videoCount = 1L;
                    } else {
                        // Video 정보 (일괄 조회로 로드됨 - 추가 쿼리 없음)
                        com.studyblock.domain.course.entity.Video video = videoMap.get(lecture.getId());
                        if (video != null) {
                            lastVideoId = video.getId();
                            lastVideoEncodingStatus = video.getEncodingStatus();
                            videoCount = 1L;
                        }
                    }

                    // Section 정보는 Map에서 직접 접근 (LAZY 로딩 방지)
                    com.studyblock.domain.course.entity.Section section = 
                            lecture.getSection() != null 
                                    ? sectionMap.get(lecture.getSection().getId()) 
                                    : null;
                    Long sectionId = section != null ? section.getId() : null;
                    Long priceCookie = section != null ? section.getCookiePrice() : null;
                    Integer discountPercentage = section != null ? section.getDiscountPercentage() : 0;

                    // LectureSummaryResponse 직접 생성 (of() 메서드 사용 시 getPriceCookie() 호출 방지)
                    LectureSummaryResponse response = LectureSummaryResponse.builder()
                            .id(lecture.getId())
                            .sectionId(sectionId)
                            .sequence(lecture.getSequence())
                            .title(lecture.getTitle())
                            .description(lecture.getDescription())
                            .thumbnailUrl(lecture.getThumbnailUrl())
                            .uploadDate(lecture.getUploadDate())
                            .status(lecture.getStatus())
                            .isFree(lecture.getIsFree())
                            .priceCookie(priceCookie)
                            .discountPercentage(discountPercentage)
                            .lastVideoId(lastVideoId)
                            .lastVideoEncodingStatus(lastVideoEncodingStatus != null 
                                    ? lastVideoEncodingStatus.name() 
                                    : null)
                            .videoCount(videoCount)
                            .build();
                    applyLectureThumbnail(response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void applyCourseThumbnail(CourseResponse response) {
        if (response == null) {
            return;
        }
        String original = response.getThumbnailOriginalUrl();
        if (original == null || original.isBlank()) {
            original = response.getThumbnailUrl();
            response.setThumbnailOriginalUrl(original);
        }
        String signed = generateSignedUrl(original);
        response.setThumbnailUrl(signed);
    }

    private void applyCourseDetailThumbnail(CourseDetailResponse response) {
        if (response == null) {
            return;
        }
        String original = response.getThumbnailOriginalUrl();
        if (original == null || original.isBlank()) {
            original = response.getThumbnailUrl();
            response.setThumbnailOriginalUrl(original);
        }
        String signed = generateSignedUrl(original);
        response.setThumbnailUrl(signed);
    }

    private void applyLectureThumbnail(LectureSummaryResponse response) {
        if (response == null) {
            return;
        }
        String original = response.getThumbnailOriginalUrl();
        if (original == null || original.isBlank()) {
            original = response.getThumbnailUrl();
            response.setThumbnailOriginalUrl(original);
        }
        String signed = generateSignedUrl(original);
        response.setThumbnailUrl(signed);
    }

    private void applyInstructorImage(InstructorResponse response) {
        if (response == null) {
            return;
        }
        String original = response.getProfileImageOriginalUrl();
        if (original == null || original.isBlank()) {
            original = response.getProfileImageUrl();
            response.setProfileImageOriginalUrl(original);
        }
        String signed = generateSignedUrl(original);
        response.setProfileImageUrl(signed);
    }

    private void applyRelatedCourseThumbnail(RelatedCourseResponse response) {
        if (response == null) return;
        String original = response.getThumbnailOriginalUrl();
        if (original == null || original.isBlank()) {
            original = response.getThumbnailUrl();
            response.setThumbnailOriginalUrl(original);
        }
        response.setThumbnailUrl(generateSignedUrl(original));
    }

    private String generateSignedUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) return null;
        try {
            return s3StorageService.generatePresignedUrl(originalUrl, COURSE_IMAGE_URL_EXPIRATION_MINUTES);
        } catch (RuntimeException e) {
            log.warn("코스/강의 presigned URL 생성 실패 - url: {}", originalUrl, e);
            return null;
        }
    }

    private void verifyCourseOwnership(Course course, com.studyblock.domain.user.entity.User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        if (currentUser.getInstructorProfile() == null) {
            throw new AccessDeniedException("강사 권한이 필요합니다.");
        }

        if (course.getInstructor() == null || course.getInstructor().getId() == null) {
            throw new AccessDeniedException("코스에 강사가 지정되어 있지 않습니다.");
        }

        if (!Objects.equals(course.getInstructor().getId(), currentUser.getInstructorProfile().getId())) {
            throw new AccessDeniedException("해당 코스를 수정할 권한이 없습니다.");
        }
    }
}
