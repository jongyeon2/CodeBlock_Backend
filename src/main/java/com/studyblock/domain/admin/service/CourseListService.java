package com.studyblock.domain.admin.service;

import com.studyblock.domain.admin.dto.CourseListResponse;
import com.studyblock.domain.admin.repository.CourseListRepository;
import com.studyblock.domain.course.entity.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseListService {

    private final CourseListRepository courseListRepository;

    // 코스 리스트 불러오기
    public List<CourseListResponse> getCourseList() {
        // Course 엔티티 DTO로 변환
        List<Course> courses = courseListRepository.findAllWithInstructor();

        return courses.stream()
                .map(course -> {
                    // Category가 없는 Course 처리 (null 체크)
                    var primaryCategory = course.getPrimaryCategory();
                    return CourseListResponse.builder()
                            .id(course.getId())
                            .title(course.getTitle())
                            .summary(course.getSummary())
                            .name(course.getInstructorName())
                            .categoryName(primaryCategory != null ? primaryCategory.getName() : "미분류")
                            .categoryId(primaryCategory != null ? primaryCategory.getId() : null)
                            .level(course.getLevel())
                            .durationMinutes(course.getDurationMinutes())
                            .price(course.getPrice())
                            .enrollmentCount(course.getEnrollmentCount())
                            .isPublished(course.getIsPublished())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 강의 공개/비공개 상태 변경
    @Transactional
    public void updateCoursePublishStatus(Long courseId, Boolean isPublished) {
        Course course = courseListRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID=" + courseId));

        if (isPublished != null && isPublished) {
            course.publish();
        } else {
            course.unpublish();
        }

        courseListRepository.save(course); // 변경사항 저장
        log.info("강의 공개/비공개 상태 변경 성공: courseId={}, isPublished={}", courseId, isPublished);
    }

    // 차단된 강의 조회 (isPublished = false)
    public List<CourseListResponse> getBlockedCourses() {
        List<Course> blockedCourses = courseListRepository.findByIsPublishedFalse();
        return blockedCourses.stream()
                .map(course -> {
                    var primaryCategory = course.getPrimaryCategory();
                    return CourseListResponse.builder()
                            .id(course.getId())
                            .title(course.getTitle())
                            .summary(course.getSummary())
                            .name(course.getInstructorName())
                            .categoryName(primaryCategory != null ? primaryCategory.getName() : "미분류")
                            .categoryId(primaryCategory != null ? primaryCategory.getId() : null)
                            .level(course.getLevel())
                            .durationMinutes(course.getDurationMinutes())
                            .price(course.getPrice())
                            .enrollmentCount(course.getEnrollmentCount())
                            .isPublished(course.getIsPublished())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
