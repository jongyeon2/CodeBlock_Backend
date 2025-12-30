package com.studyblock.domain.enrollment.service;

import com.studyblock.domain.activitylog.enums.ActionType;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.course.service.LectureOwnershipService;
import com.studyblock.domain.enrollment.dto.EnrollmentResponse;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.entity.LectureCompletion;
import com.studyblock.domain.enrollment.entity.SectionEnrollment;
import com.studyblock.domain.enrollment.enums.CompletionType;
import com.studyblock.domain.enrollment.enums.EnrollmentSource;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.enrollment.repository.LectureCompletionRepository;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 수강신청 서비스
 * Course enrollment business logic
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final LectureCompletionRepository lectureCompletionRepository;
    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LectureOwnershipService lectureOwnershipService;
    private final SectionEnrollmentService sectionEnrollmentService;
    private final ActivityLogService activityLogService;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    /**
     * 결제 완료 후 수강신청 및 섹션 소유권 생성
     * @param order 주문 정보
     * @return 생성된 수강신청 목록
     */
    @Transactional
    public List<CourseEnrollment> createEnrollmentsFromOrder(Order order) {
        log.info("Creating enrollments from order: {}", order.getId());

        List<CourseEnrollment> enrollments = new ArrayList<>();
        int sectionOwnershipCount = 0;

        for (OrderItem item : order.getOrderItems()) {
            // COURSE 타입: CourseEnrollment 생성
            if (item.getItemType() == ItemType.COURSE && item.getCourse() != null) {
                try {
                    CourseEnrollment enrollment = createEnrollment(
                            order.getUser(),
                            item.getCourse(),
                            order
                    );
                    enrollments.add(enrollment);
                    log.info("CourseEnrollment 생성 성공 - User: {}, Course: {}, Order: {}",
                            order.getUser().getId(), item.getCourse().getId(), order.getOrderNumber());
                } catch (IllegalStateException e) {
                    log.warn("CourseEnrollment 생성 실패 (중복 가능): {}", e.getMessage());
                }
            }
            // SECTION 타입: LectureOwnership 생성
            else if (item.getItemType() == ItemType.SECTION && item.getSection() != null) {
                try {
                    lectureOwnershipService.createOwnership(
                            order.getUser(),
                            item.getSection(),
                            order
                    );
                    sectionOwnershipCount++;
                    log.info("LectureOwnership 생성 성공 - User: {}, Section: {}, Order: {}",
                            order.getUser().getId(), item.getSection().getId(), order.getOrderNumber());
                } catch (IllegalStateException e) {
                    log.warn("LectureOwnership 생성 실패 (중복 가능): {}", e.getMessage());
                }
            }
        }

        log.info("Order {} 처리 완료 - CourseEnrollment: {}, LectureOwnership: {}",
                order.getId(), enrollments.size(), sectionOwnershipCount);

        // 로그 저장
        List<OrderItem> orderItems = order.getOrderItems();
        for (OrderItem item : orderItems) {
            if (item.getCourse() != null) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("courseId", item.getCourse().getId());
                metadata.put("courseName", item.getCourse().getTitle());
                metadata.put("orderId", order.getId());
                metadata.put("paymentType", order.getPaymentType().name());
                metadata.put("totalAmount", order.getTotalAmount());
                metadata.put("cookieSpent", order.getCookieSpent());

                activityLogService.createLog(
                        order.getUser().getId(),
                        ActionType.COURSE_PURCHASE,
                        "COURSE",
                        item.getCourse().getId(),
                        String.format("%s 강의 구매", item.getCourse().getTitle()),
                        null,
                        metadata
                );
            }
        }

        return enrollments;
    }

    /**
     * 수강신청 생성
     * @param user 사용자
     * @param course 강좌
     * @param order 주문 (null일 경우 관리자 부여)
     * @return 생성된 수강신청
     */
    @Transactional
    public CourseEnrollment createEnrollment(User user, Course course, Order order) {
        // 중복 수강신청 체크
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            throw new IllegalStateException("이미 수강 중인 강좌입니다.");
        }

        // 수강신청 출처 결정
        EnrollmentSource source;
        if (order == null) {
            source = EnrollmentSource.ADMIN_GRANT;
        } else if (order.getPaymentType() == PaymentType.CASH) {
            source = EnrollmentSource.PURCHASE_CASH;
        } else {
            source = EnrollmentSource.PURCHASE_COOKIE;
        }

        // 수강신청 생성
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .order(order)
                .enrollmentSource(source)
                .expiresAt(null) // 영구 접근
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        // 강좌 수강생 수 증가
        course.increaseEnrollmentCount();
        courseRepository.save(course);

        // 강의 완료 추적 초기화
        initializeLectureCompletions(enrollment);

        log.info("Enrollment created: userId={}, courseId={}, enrollmentId={}",
                user.getId(), course.getId(), enrollment.getId());

        return enrollment;
    }

    /**
     * 무료 강좌 수강신청 (프론트엔드에서 직접 호출)
     * @param userId 사용자 ID
     * @param courseId 강좌 ID
     * @return 생성된 수강신청
     * @throws IllegalArgumentException 사용자/강좌를 찾을 수 없음
     * @throws IllegalStateException 이미 수강 중이거나 유료 강좌인 경우
     */
    @Transactional
    public CourseEnrollment enrollFreeCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 중복 수강신청 체크
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("이미 수강 중인 강좌입니다.");
        }

        // 가격 확인 - 0원이 아니면 결제 필요
        if (course.getPrice() != null && course.getPrice() > 0) {
            throw new IllegalStateException("유료 강좌는 결제가 필요합니다.");
        }

        // 무료 강좌 수강신청 생성
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .order(null)
                .enrollmentSource(EnrollmentSource.PROMOTIONAL)
                .expiresAt(null) // 영구 접근
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        // 강좌 수강생 수 증가
        course.increaseEnrollmentCount();
        courseRepository.save(course);

        // 강의 완료 추적 초기화
        initializeLectureCompletions(enrollment);

        log.info("Free course enrolled: userId={}, courseId={}, enrollmentId={}",
                userId, courseId, enrollment.getId());

        return enrollment;
    }

    /**
     * 관리자가 수강신청 부여
     * @param userId 사용자 ID
     * @param courseId 강좌 ID
     * @param expiresAt 만료 날짜 (null = 영구)
     * @return 생성된 수강신청
     */
    @Transactional
    public CourseEnrollment grantEnrollmentByAdmin(Long userId, Long courseId, LocalDateTime expiresAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 중복 체크
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("이미 수강 중인 강좌입니다.");
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .order(null)
                .enrollmentSource(EnrollmentSource.ADMIN_GRANT)
                .expiresAt(expiresAt)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        // 강좌 수강생 수 증가
        course.increaseEnrollmentCount();
        courseRepository.save(course);

        // 강의 완료 추적 초기화
        initializeLectureCompletions(enrollment);

        log.info("Enrollment granted by admin: userId={}, courseId={}", userId, courseId);
        return enrollment;
    }

    /**
     * 강의 완료 추적 초기화
     * @param enrollment 수강신청
     */
    private void initializeLectureCompletions(CourseEnrollment enrollment) {
        Course course = enrollment.getCourse();
        User user = enrollment.getUser();

        int totalLectures = 0;

        // 모든 섹션의 강의에 대해 LectureCompletion 생성
        for (Section section : course.getSections()) {
            for (Lecture lecture : section.getLectures()) {
                // 기존에 없는 경우만 생성
                if (!lectureCompletionRepository.existsByUserIdAndLectureId(user.getId(), lecture.getId())) {
                    LectureCompletion completion = LectureCompletion.builder()
                            .user(user)
                            .lecture(lecture)
                            .courseEnrollment(enrollment)
                            .build();
                    lectureCompletionRepository.save(completion);
                }
                totalLectures++;
            }
        }

        // 강좌의 전체 퀴즈 수 조회
        // TODO: QuizRepository에 적절한 조회 메서드 추가 후 구현
        // 현재는 0으로 초기화하고, 실제 퀴즈 진행 시 updateQuizCompletion으로 업데이트
        int totalQuizzes = 0;

        // 전체 강의/퀴즈 수 업데이트
        enrollment.updateLectureCompletion(0, totalLectures);
        enrollment.updateQuizCompletion(0, totalQuizzes);
        enrollmentRepository.save(enrollment);

        log.info("Initialized {} lecture completions for enrollment {}", totalLectures, enrollment.getId());
    }

    /**
     * 사용자의 수강신청 목록 조회
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    public Page<CourseEnrollment> getMyEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findActiveEnrollmentsByUserId(userId, pageable);
    }

    /**
     * 특정 수강신청 조회
     * @param enrollmentId 수강신청 ID
     * @param userId 사용자 ID (권한 확인용)
     * @return 수강신청
     */
    public CourseEnrollment getEnrollment(Long enrollmentId, Long userId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강신청을 찾을 수 없습니다."));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        initializeEnrollmentAssociations(enrollment);
        return enrollment;
    }

    /**
     * 사용자가 특정 강좌에 수강 중인지 확인
     * @param userId 사용자 ID
     * @param courseId 강좌 ID
     * @return 수강 여부
     */
    public boolean isEnrolled(Long userId, Long courseId) {
        log.info("Checking enrollment: userId={}, courseId={}", userId, courseId);
        long count = enrollmentRepository.countAccessibleEnrollments(userId, courseId, LocalDateTime.now());
        boolean hasAccess = count > 0;
        log.info("Enrollment check result: userId={}, courseId={}, count={}, hasAccess={}",
                userId, courseId, count, hasAccess);

        // 디버깅을 위해 실제 enrollment 데이터도 확인
        var enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (enrollment.isPresent()) {
            CourseEnrollment e = enrollment.get();
            log.info("Found enrollment: id={}, status={}, expiresAt={}, isAccessible={}",
                    e.getId(), e.getStatus(), e.getExpiresAt(), e.isAccessible());
        } else {
            log.info("No enrollment found for userId={}, courseId={}", userId, courseId);
        }

        return hasAccess;
    }

    /**
     * 수강신청 정보 조회 (없으면 null)
     * @param userId 사용자 ID
     * @param courseId 강좌 ID
     * @return 수강신청 (없으면 null)
     */
    public EnrollmentResponse getEnrollmentByUserAndCourse(Long userId, Long courseId) {
        CourseEnrollment enrollment = enrollmentRepository.findWithCourseByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        List<Long> purchasedSectionIds = getPurchasedSectionIds(userId, courseId);

        if (enrollment != null) {
            initializeEnrollmentAssociations(enrollment);
            List<Long> completedLectureIds = getCompletedLectureIds(enrollment.getId());
            return EnrollmentResponse.from(enrollment, completedLectureIds, purchasedSectionIds);
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        List<SectionEnrollment> sectionEnrollments = sectionEnrollmentService.findByUserAndCourse(userId, courseId);

        long completedLectureCount = lectureCompletionRepository
                .countCompletedLecturesByUserAndCourse(userId, courseId);
        long totalLectureCount = lectureRepository.countByCourseId(courseId);

        BigDecimal progress = calculateProgress(completedLectureCount, totalLectureCount);

        List<Long> completedLectureIds = lectureCompletionRepository
                .findCompletedLecturesByUserAndCourse(userId, courseId)
                .stream()
                .map(lectureCompletion -> lectureCompletion.getLecture().getId())
                .distinct()
                .toList();

        LocalDateTime enrolledAt = sectionEnrollments.stream()
                .map(SectionEnrollment::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime startedAt = sectionEnrollments.stream()
                .map(SectionEnrollment::getStartedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime completedAt = sectionEnrollments.stream()
                .map(SectionEnrollment::getCompletedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastAccessedAt = sectionEnrollments.stream()
                .map(SectionEnrollment::getLastAccessedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return EnrollmentResponse.sectionSnapshot(
                userId,
                course,
                progress,
                (int) completedLectureCount,
                (int) totalLectureCount,
                completedLectureIds,
                purchasedSectionIds,
                enrolledAt,
                startedAt,
                completedAt,
                lastAccessedAt
        );
    }

    /**
     * 강의 완료 처리
     * @param userId 사용자 ID
     * @param lectureId 강의 ID
     * @param completionType 완료 타입
     */
    @Transactional
    public void markLectureAsCompleted(Long userId, Long lectureId, CompletionType completionType) {
        LectureCompletion completion = lectureCompletionRepository
                .findByUserIdAndLectureId(userId, lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의 완료 정보를 찾을 수 없습니다."));

        if (completion.isCompleted()) {
            return; // 이미 완료됨
        }

        completion.markAsCompleted(completionType);
        lectureCompletionRepository.save(completion);

        sectionEnrollmentService.updateProgressByLecture(userId, completion.getLecture());

        // 수강신청 진도율 업데이트
        if (completion.getCourseEnrollment() != null) {
            updateEnrollmentProgress(completion.getCourseEnrollment().getId());
        }

        log.info("Lecture completed: userId={}, lectureId={}, type={}",
                userId, lectureId, completionType);
    }

    /**
     * 비디오 시청 진도 업데이트
     * @param userId 사용자 ID
     * @param lectureId 강의 ID
     * @param watchPercentage 시청률
     * @param timeSpentSeconds 시청 시간 (초)
     */
    @Transactional
    public void updateVideoProgress(Long userId, Long lectureId,
                                     BigDecimal watchPercentage, int timeSpentSeconds) {
        LectureCompletion completion = getOrCreateLectureCompletion(userId, lectureId);

        completion.updateVideoProgress(watchPercentage, timeSpentSeconds);
        lectureCompletionRepository.save(completion);

        sectionEnrollmentService.updateProgressByLecture(userId, completion.getLecture());

        // 수강신청의 마지막 접속 시간 및 콘텐츠 시청 업데이트
        if (completion.getCourseEnrollment() != null) {
            CourseEnrollment enrollment = completion.getCourseEnrollment();
            enrollment.markContentViewed();

            // 콘텐츠 시청률 재계산
            updateContentViewPercentage(enrollment.getId());

            // 진도율을 콘텐츠 시청률과 동기화 (실시간 반영)
            enrollment = enrollmentRepository.findById(enrollment.getId())
                    .orElseThrow(() -> new IllegalArgumentException("수강신청을 찾을 수 없습니다."));
            enrollment.updateProgress(enrollment.getContentViewPercentage());

            enrollmentRepository.save(enrollment);

            log.info("비디오 진도율 업데이트 완료 - userId: {}, lectureId: {}, watchPercentage: {}%, courseProgress: {}%",
                    userId, lectureId, watchPercentage, enrollment.getProgressPercentage());
        }

        // 90% 이상 시청 시 자동으로 진도율 업데이트
        if (completion.isCompleted() && completion.getCourseEnrollment() != null) {
            updateEnrollmentProgress(completion.getCourseEnrollment().getId());
        }
    }

    /**
     * 퀴즈 결과 업데이트
     * @param userId 사용자 ID
     * @param lectureId 강의 ID
     * @param score 점수
     * @param passed 합격 여부
     */
    @Transactional
    public void updateQuizResult(Long userId, Long lectureId, BigDecimal score, boolean passed) {
        LectureCompletion completion = lectureCompletionRepository
                .findByUserIdAndLectureId(userId, lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의 완료 정보를 찾을 수 없습니다."));

        completion.updateQuizScore(score, passed);
        lectureCompletionRepository.save(completion);

        sectionEnrollmentService.updateProgressByLecture(userId, completion.getLecture());

        // 퀴즈 통과 시 진도율 업데이트
        if (passed && completion.getCourseEnrollment() != null) {
            updateEnrollmentProgress(completion.getCourseEnrollment().getId());
        }

        log.info("Quiz result updated: userId={}, lectureId={}, score={}, passed={}",
                userId, lectureId, score, passed);
    }

    private LectureCompletion getOrCreateLectureCompletion(Long userId, Long lectureId) {
        return lectureCompletionRepository.findByUserIdAndLectureId(userId, lectureId)
                .orElseGet(() -> createLectureCompletionWithBackfill(userId, lectureId));
    }

    private LectureCompletion createLectureCompletionWithBackfill(Long userId, Long lectureId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, lecture.getCourse().getId())
                .orElse(null);

        if (enrollment != null) {
            ensureLectureCompletionsForCourse(enrollment);
            return lectureCompletionRepository.findByUserIdAndLectureId(userId, lectureId)
                    .orElseThrow(() -> new IllegalStateException("강의 완료 정보를 생성하지 못했습니다."));
        }

        LectureCompletion completion = LectureCompletion.builder()
                .user(user)
                .lecture(lecture)
                .build();

        LectureCompletion saved = lectureCompletionRepository.save(completion);

        log.info("LectureCompletion 생성 (enrollment 없음) - userId={}, lectureId={}", userId, lectureId);

        return saved;
    }

    private void ensureLectureCompletionsForCourse(CourseEnrollment enrollment) {
        Long userId = enrollment.getUser().getId();
        Long courseId = enrollment.getCourse().getId();

        List<Lecture> lectures = lectureRepository.findByCourseIdOrderBySequenceAsc(courseId);
        List<LectureCompletion> newCompletions = new ArrayList<>();

        for (Lecture lecture : lectures) {
            if (!lectureCompletionRepository.existsByUserIdAndLectureId(userId, lecture.getId())) {
                newCompletions.add(
                        LectureCompletion.builder()
                                .user(enrollment.getUser())
                                .lecture(lecture)
                                .courseEnrollment(enrollment)
                                .build()
                );
            }
        }

        if (!newCompletions.isEmpty()) {
            lectureCompletionRepository.saveAll(newCompletions);
            log.info("LectureCompletion 보정 - userId={}, courseId={}, 추가된 강의 수={}",
                    userId, courseId, newCompletions.size());
        }

        synchronizeEnrollmentLectureCounts(enrollment);
    }

    private void synchronizeEnrollmentLectureCounts(CourseEnrollment enrollment) {
        long totalLectures = lectureCompletionRepository.countTotalByEnrollmentId(enrollment.getId());
        long completedLectures = lectureCompletionRepository.countCompletedByEnrollmentId(enrollment.getId());
        enrollment.updateLectureCompletion((int) completedLectures, (int) totalLectures);
        enrollmentRepository.save(enrollment);
    }

    /**
     * 수강신청 진도율 업데이트
     * @param enrollmentId 수강신청 ID
     */
    @Transactional
    public void updateEnrollmentProgress(Long enrollmentId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강신청을 찾을 수 없습니다."));

        // 완료된 강의 수 조회
        long completedLectures = lectureCompletionRepository
                .countCompletedByEnrollmentId(enrollmentId);

        // 완료된 퀴즈 수 조회 (퀴즈 점수가 있고 통과한 경우)
        List<LectureCompletion> completions = lectureCompletionRepository
                .findByCourseEnrollmentId(enrollmentId);

        long completedQuizzes = completions.stream()
                .filter(c -> c.getQuizScore() != null && c.isCompleted())
                .count();

        // 카운트 업데이트
        enrollment.updateLectureCompletion((int) completedLectures, enrollment.getTotalLecturesCount());
        enrollment.updateQuizCompletion((int) completedQuizzes, enrollment.getTotalQuizzesCount());

        enrollmentRepository.save(enrollment);

        log.debug("Progress updated for enrollment {}: lectures={}/{}, quizzes={}/{}",
                enrollmentId, completedLectures, enrollment.getTotalLecturesCount(),
                completedQuizzes, enrollment.getTotalQuizzesCount());
    }

    /**
     * 콘텐츠 시청률 재계산
     * @param enrollmentId 수강신청 ID
     */
    @Transactional
    public void updateContentViewPercentage(Long enrollmentId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강신청을 찾을 수 없습니다."));

        List<LectureCompletion> completions = lectureCompletionRepository
                .findByCourseEnrollmentId(enrollmentId);

        if (completions.isEmpty()) {
            return;
        }

        // 평균 시청률 계산
        double avgViewPercentage = completions.stream()
                .mapToDouble(c -> c.getVideoWatchPercentage().doubleValue())
                .average()
                .orElse(0.0);

        enrollment.updateContentViewPercentage(BigDecimal.valueOf(avgViewPercentage));
        enrollmentRepository.save(enrollment);
    }

    private BigDecimal calculateProgress(long completedLectures, long totalLectures) {
        if (totalLectures <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(completedLectures)
                .divide(BigDecimal.valueOf(totalLectures), 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 수강신청의 완료된 강의 ID 목록 조회
     * @param enrollmentId 수강신청 ID
     * @return 완료된 강의 ID 목록
     */
    public List<Long> getCompletedLectureIds(Long enrollmentId) {
        List<Long> lectureIds = lectureCompletionRepository.findCompletedLectureIdsByEnrollmentId(enrollmentId);
        return lectureIds != null ? lectureIds : Collections.emptyList();
    }

    /**
     * 사용자가 특정 코스에서 구매한 섹션 ID 목록 조회
     * @param userId 사용자 ID
     * @param courseId 코스 ID
     * @return 구매한 섹션 ID 목록
     */
    public List<Long> getPurchasedSectionIds(Long userId, Long courseId) {
        List<Long> sectionIds = lectureOwnershipService.getPurchasedSectionIds(userId, courseId);
        return sectionIds != null ? sectionIds : Collections.emptyList();
    }

    /**
     * 사용자가 구매한 모든 섹션 ID 목록 조회 (코스 필터 없음)
     * @param userId 사용자 ID
     * @return 구매한 모든 섹션 ID 목록
     */
    public List<Long> getAllPurchasedSectionIds(Long userId) {
        List<Long> sectionIds = lectureOwnershipService.getPurchasedSectionIdsByUser(userId);
        return sectionIds != null ? sectionIds : Collections.emptyList();
    }

    /**
     * 수강신청 취소 (환불)
     * @param enrollmentId 수강신청 ID
     * @param userId 사용자 ID
     * @return 취소된 수강신청
     */
    @Transactional
    public CourseEnrollment cancelEnrollment(Long enrollmentId, Long userId) {
        CourseEnrollment enrollment = getEnrollment(enrollmentId, userId);

        if (!enrollment.isRefundable()) {
            String reason = enrollment.getRefundIneligibilityReason();
            throw new IllegalStateException("환불이 불가능합니다: " + reason);
        }

        enrollment.revoke();
        enrollmentRepository.save(enrollment);

        // 강좌 수강생 수 감소
        Course course = enrollment.getCourse();
        course.decreaseEnrollmentCount();
        courseRepository.save(course);

        log.info("Enrollment cancelled: enrollmentId={}, userId={}", enrollmentId, userId);
        return enrollment;
    }

    /**
     * 만료된 수강신청 일괄 처리 (배치용)
     * @return 만료 처리된 수강신청 수
     */
    @Transactional
    public int expireEnrollments() {
        List<CourseEnrollment> expiredEnrollments = enrollmentRepository
                .findExpiredEnrollments(LocalDateTime.now());

        for (CourseEnrollment enrollment : expiredEnrollments) {
            enrollment.expire();
            enrollmentRepository.save(enrollment);
        }

        log.info("Expired {} enrollments", expiredEnrollments.size());
        return expiredEnrollments.size();
    }

    /**
     * 강좌의 수강생 목록 조회 (강사용)
     * @param courseId 강좌 ID
     * @param instructorId 강사 ID (권한 확인용)
     * @param pageable 페이징 정보
     * @return 수강생 목록
     */
    public Page<CourseEnrollment> getCourseStudents(Long courseId, Long instructorId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new IllegalStateException("해당 강좌의 강사가 아닙니다.");
        }

        return enrollmentRepository.findByCourseId(courseId, pageable);
    }

    /**
     * 사용자의 통계 조회
     * @param userId 사용자 ID
     * @return 통계 정보
     */
    public EnrollmentStats getUserStats(Long userId) {
        long activeEnrollments = enrollmentRepository.countActiveByUserId(userId);
        long completedCourses = enrollmentRepository.countCompletedByUserId(userId);

        return EnrollmentStats.builder()
                .activeEnrollments(activeEnrollments)
                .completedCourses(completedCourses)
                .build();
    }

    private void initializeEnrollmentAssociations(CourseEnrollment enrollment) {
        if (enrollment == null) {
            return;
        }
        Hibernate.initialize(enrollment.getCourse());
        if (enrollment.getCourse() != null) {
            Hibernate.initialize(enrollment.getCourse().getInstructor());
            if (enrollment.getCourse().getInstructor() != null) {
                Hibernate.initialize(enrollment.getCourse().getInstructor().getUser());
            }
        }
    }

    /**
     * 통계 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class EnrollmentStats {
        private long activeEnrollments;
        private long completedCourses;
    }
}
