package com.studyblock.domain.mylearning.service;

import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.entity.SectionEnrollment;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.enrollment.repository.SectionEnrollmentRepository;
import com.studyblock.domain.mylearning.dto.MyLearningItemResponse;
import com.studyblock.domain.user.repository.LectureOwnershipRepository;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 내 학습 통합 서비스
 * CourseEnrollment(보유코스)와 SectionEnrollment(보유섹션)를 통합하여 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyLearningService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final SectionEnrollmentRepository sectionEnrollmentRepository;
    private final LectureOwnershipRepository lectureOwnershipRepository;
    private final S3StorageService s3StorageService;
    /**
     *
     * 사용자의 모든 학습 콘텐츠 조회 (통합 - 코스 + 섹션)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 통합된 학습 아이템 페이지
     */
    public Page<MyLearningItemResponse> getMyLearningItems(Long userId, Pageable pageable) {
        log.info("통합 학습 콘텐츠 조회 - userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // 1. CourseEnrollment 조회 (ACTIVE, COMPLETED만)
        List<CourseEnrollment> enrollments = courseEnrollmentRepository
                .findActiveEnrollmentsByUserId(userId, Pageable.unpaged())
                .getContent();

        log.debug("CourseEnrollment 조회 완료 - count: {}", enrollments.size());

        // 2. SectionEnrollment 조회 (진도 정보 포함)
        List<SectionEnrollment> sectionEnrollments = sectionEnrollmentRepository
                .findByUser_Id(userId);

        log.debug("SectionEnrollment 조회 완료 - count: {}", sectionEnrollments.size());

        // 3. LectureOwnership 조회 (ACTIVE만, SectionEnrollment에 없는 경우를 위해)
        List<LectureOwnership> ownerships = lectureOwnershipRepository
                .findActiveOwnershipsByUserId(userId);

        log.debug("LectureOwnership 조회 완료 - count: {}", ownerships.size());

        // 4. 통합 리스트 생성
        List<MyLearningItemResponse> allItems = new ArrayList<>();

        // CourseEnrollment → MyLearningItemResponse 변환
        allItems.addAll(enrollments.stream()
                .map(MyLearningItemResponse::fromCourseEnrollment)
                .collect(Collectors.toList()));

        // SectionEnrollment → MyLearningItemResponse 변환 (우선)
        allItems.addAll(sectionEnrollments.stream()
                .map(MyLearningItemResponse::fromSectionEnrollment)
                .collect(Collectors.toList()));

        // LectureOwnership → MyLearningItemResponse 변환 (SectionEnrollment에 없는 섹션만)
        List<Long> sectionEnrollmentIds = sectionEnrollments.stream()
                .map(se -> se.getSection().getId())
                .collect(Collectors.toList());

        allItems.addAll(ownerships.stream()
                .filter(LectureOwnership::isActive)
                .filter(ownership -> !sectionEnrollmentIds.contains(ownership.getSection().getId()))
                .map(MyLearningItemResponse::fromLectureOwnership)
                .collect(Collectors.toList()));

        log.debug("통합 리스트 생성 완료 - total count: {}", allItems.size());
        // 썸네일에 presigned 적용
        allItems.forEach(item -> {
            if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isEmpty()){
                try {
                    String presignedUrl = s3StorageService.generatePresignedUrl(item.getThumbnailUrl(), 30);
                    item.setThumbnailUrl(presignedUrl);
                } catch (Exception e) {
                    log.warn("Presigned URL 생성 실패");
                }
            }
        });

        // 4. 최근 활동 날짜 기준 정렬 (최신순)
        allItems.sort(Comparator.comparing(MyLearningItemResponse::getLastActivityDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 5. 페이징 처리 (수동)
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allItems.size());

        List<MyLearningItemResponse> pagedItems;
        if (start >= allItems.size()) {
            pagedItems = List.of(); // 페이지 범위를 벗어나면 빈 리스트
        } else {
            pagedItems = allItems.subList(start, end);
        }

        log.info("페이징 처리 완료 - start: {}, end: {}, total: {}, pagedSize: {}",
                start, end, allItems.size(), pagedItems.size());

        // 6. Page 객체 생성 및 반환
        return new PageImpl<>(pagedItems, pageable, allItems.size());
    }

    /**
     * 사용자의 통계 정보 조회
     *
     * @param userId 사용자 ID
     * @return 통계 정보 맵
     */
    public MyLearningStatsResponse getMyLearningStats(Long userId) {
        log.info("학습 통계 조회 - userId: {}", userId);

        // CourseEnrollment 통계
        List<CourseEnrollment> enrollments = courseEnrollmentRepository
                .findActiveEnrollmentsByUserId(userId, Pageable.unpaged())
                .getContent();

        long totalCourses = enrollments.size();
        long completedCourses = enrollments.stream()
                .filter(CourseEnrollment::isCompleted)
                .count();

        // SectionEnrollment 통계
        List<SectionEnrollment> sectionEnrollments = sectionEnrollmentRepository
                .findByUser_Id(userId);

        long totalSections = sectionEnrollments.size();
        long completedSections = sectionEnrollments.stream()
                .filter(SectionEnrollment::isCompleted)
                .count();

        // LectureOwnership 통계 (SectionEnrollment에 없는 섹션만)
        List<LectureOwnership> ownerships = lectureOwnershipRepository
                .findActiveOwnershipsByUserId(userId);

        List<Long> sectionEnrollmentIds = sectionEnrollments.stream()
                .map(se -> se.getSection().getId())
                .collect(Collectors.toList());

        long additionalSections = ownerships.stream()
                .filter(LectureOwnership::isActive)
                .filter(ownership -> !sectionEnrollmentIds.contains(ownership.getSection().getId()))
                .count();

        totalSections += additionalSections;

        log.info("학습 통계 - totalCourses: {}, completedCourses: {}, totalSections: {}, completedSections: {}",
                totalCourses, completedCourses, totalSections, completedSections);

        return MyLearningStatsResponse.builder()
                .totalCourses(totalCourses)
                .completedCourses(completedCourses)
                .activeCourses(totalCourses - completedCourses)
                .totalSections(totalSections)
                .completedSections(completedSections)
                .totalItems(totalCourses + totalSections)
                .build();
    }

    /**
     * 통계 응답 DTO
     */
    @lombok.Getter
    @lombok.Builder
    public static class MyLearningStatsResponse {
        private Long totalCourses;       // 총 코스 수
        private Long completedCourses;   // 완료한 코스 수
        private Long activeCourses;      // 진행 중인 코스 수
        private Long totalSections;      // 총 섹션 수
        private Long completedSections;  // 완료한 섹션 수
        private Long totalItems;         // 전체 아이템 수 (코스 + 섹션)
    }
}
