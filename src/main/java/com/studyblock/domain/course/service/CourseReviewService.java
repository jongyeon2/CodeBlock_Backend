package com.studyblock.domain.course.service;

import com.studyblock.domain.activitylog.enums.ActionType;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.course.dto.CourseReviewCreateRequest;
import com.studyblock.domain.course.dto.CourseReviewResponse;
import com.studyblock.domain.course.dto.CourseReviewUpdateRequest;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.CourseReview;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.CourseReviewRepository;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseReviewService {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ActivityLogService activityLogService;

    /**
     * 수강 후기 목록 조회
     */
    public Page<CourseReviewResponse> getCourseReviews(Long courseId, Long lectureId, Pageable pageable) {
        log.info("코스 ID {} 후기 목록 조회 요청. lectureId={}", courseId, lectureId);

        Page<CourseReview> reviews = lectureId != null
                ? courseReviewRepository.findByCourseIdAndLectureId(courseId, lectureId, pageable)
                : courseReviewRepository.findByCourseId(courseId, pageable);

        return reviews.map(CourseReviewResponse::from);
    }

    /**
     * 수강 후기 등록/수정
     * - SecurityContext에서 인증된 사용자 정보를 가져옴
     * - 수강 여부 확인 (enrollment check)
     * - 기존 리뷰가 있으면 자동으로 업데이트, 없으면 새로 생성
     */
    @Transactional
    public CourseReviewResponse createReview(Long courseId, CourseReviewCreateRequest request) {
        log.info("리뷰 등록 요청: courseId={}, rating={}", courseId, request.getRating());

        // 1. Authentication에서 User 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 리뷰 작성 시도");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.warn("잘못된 인증 정보로 리뷰 작성 시도 - principal type: {}",
                    principal != null ? principal.getClass().getName() : "null");
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
        }

        User user = (User) principal;
        log.debug("리뷰 작성 사용자: userId={}, nickname={}", user.getId(), user.getNickname());

        // 2. 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        // 3. 수강 여부 확인 (enrollment check)
        boolean isEnrolled = courseEnrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
        if (!isEnrolled) {
            log.warn("수강하지 않은 코스에 리뷰 작성 시도 - userId: {}, courseId: {}", user.getId(), courseId);
            throw new IllegalArgumentException("해당 코스를 수강해야 리뷰를 작성할 수 있습니다.");
        }

        // 4. 강의 검증 (선택)
        Lecture lecture = null;
        if (request.getLectureId() != null) {
            lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID=" + request.getLectureId()));

            if (!lecture.getCourse().getId().equals(courseId)) {
                throw new IllegalArgumentException("해당 강의는 요청한 코스에 속하지 않습니다.");
            }
        }

        // 5. 기존 리뷰 확인 (duplicate check)
        // 여러 개가 있을 수 있으므로 List로 받아서 첫 번째만 사용
        java.util.List<CourseReview> existingReviews =
                courseReviewRepository.findByUserIdAndCourseId(user.getId(), courseId);

        if (!existingReviews.isEmpty()) {
            // 기존 리뷰가 있으면 최신 리뷰 하나만 업데이트
            CourseReview review = existingReviews.get(0);
            log.info("기존 리뷰 업데이트: reviewId={}, userId={}, courseId={}, 총 {}개의 리뷰 발견",
                    review.getId(), user.getId(), courseId, existingReviews.size());
            review.updateReview(request.getRating(), request.getContent(), request.getLectureSpecific());

            // Fetch Join으로 이미 연관 엔티티가 로드되어 있으므로 추가 초기화 불필요
            return CourseReviewResponse.from(review);
        } else {
            // 새로운 리뷰 생성
            log.info("새로운 리뷰 생성: userId={}, courseId={}", user.getId(), courseId);

            CourseReview review = CourseReview.builder()
                    .course(course)
                    .user(user)
                    .lecture(lecture)
                    .rating(request.getRating())
                    .content(request.getContent())
                    .lectureSpecific(Boolean.TRUE.equals(request.getLectureSpecific()))
                    .build();

            CourseReview saved = courseReviewRepository.save(review);
            log.info("리뷰 생성 완료: reviewId={}", saved.getId());

            // Fetch Join으로 연관 엔티티와 함께 다시 조회
            CourseReview reviewWithRelations = courseReviewRepository.findByIdWithRelations(saved.getId())
                    .orElseThrow(() -> new IllegalStateException("리뷰를 찾을 수 없습니다. ID=" + saved.getId()));

            // 로그 저장
            log.info("리뷰 등록 완료 - userId: {}, courseId: {}, reviewId: {}, rating: {}",
                    user.getId(), courseId, reviewWithRelations.getId(), reviewWithRelations.getRating());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("reviewId", reviewWithRelations.getId());
            metadata.put("courseId", course.getId());
            metadata.put("courseName", course.getTitle());
            metadata.put("rating", reviewWithRelations.getRating());
            metadata.put("contentLength", reviewWithRelations.getContent() != null ? reviewWithRelations.getContent().length() : 0);
            metadata.put("isLectureSpecific", reviewWithRelations.getLectureSpecific());

            // 특정 강의에 대한 리뷰인 경우
            if (reviewWithRelations.getLecture() != null) {
                metadata.put("lectureId", reviewWithRelations.getLecture().getId());
                metadata.put("lectureName", reviewWithRelations.getLecture().getTitle());
            }

            // service 호출 (로그 저장 실패가 리뷰 생성 자체를 막지 않도록 try-catch 처리)
            try {
                activityLogService.createLog(
                        user.getId(),
                        ActionType.COURSE_REVIEW,
                        "COURSE",
                        course.getId(),
                        String.format("%s 강의에 리뷰 작성 (평점: %d점)", course.getTitle(), reviewWithRelations.getRating()),
                        null,
                        metadata
                );
            } catch (Exception e) {
                log.error("리뷰 작성 활동 로그 저장 실패 - userId: {}, courseId: {}, reviewId: {}", 
                        user.getId(), courseId, reviewWithRelations.getId(), e);
                // 로그 저장 실패는 리뷰 생성에 영향을 주지 않음
            }

            // Fetch Join으로 이미 연관 엔티티가 로드되어 있으므로 추가 초기화 불필요
            return CourseReviewResponse.from(reviewWithRelations);
        }
    }

    /**
     * 수강 후기 수정
     */
    @Transactional
    public CourseReviewResponse updateReview(Long courseId, Long reviewId, CourseReviewUpdateRequest request) {
        // Fetch Join으로 연관 엔티티와 함께 조회
        CourseReview review = courseReviewRepository.findByIdWithRelations(reviewId)
                .filter(r -> r.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new IllegalArgumentException("수강 후기를 찾을 수 없습니다. ID=" + reviewId));

        Integer rating = request.getRating() != null ? request.getRating() : review.getRating();
        review.updateReview(rating, request.getContent(), request.getLectureSpecific());

        // Fetch Join으로 이미 연관 엔티티가 로드되어 있으므로 추가 초기화 불필요
        return CourseReviewResponse.from(review);
    }

    /**
     * 수강 후기 삭제
     */
    @Transactional
    public void deleteReview(Long courseId, Long reviewId) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .filter(r -> r.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new IllegalArgumentException("수강 후기를 찾을 수 없습니다. ID=" + reviewId));

        courseReviewRepository.delete(review);
    }
}
