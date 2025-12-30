package com.studyblock.domain.enrollment.service;

import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.enrollment.entity.SectionEnrollment;
import com.studyblock.domain.enrollment.repository.SectionEnrollmentRepository;
import com.studyblock.domain.enrollment.repository.LectureCompletionRepository;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionEnrollmentService {

    private final SectionEnrollmentRepository sectionEnrollmentRepository;
    private final LectureRepository lectureRepository;
    private final LectureCompletionRepository lectureCompletionRepository;

    @Transactional
    public SectionEnrollment createOrUpdateForSection(User user, Section section, Order order) {
        if (user == null || section == null) {
            throw new IllegalArgumentException("사용자 또는 섹션 정보가 올바르지 않습니다.");
        }

        SectionEnrollment enrollment = sectionEnrollmentRepository
                .findByUser_IdAndSection_Id(user.getId(), section.getId())
                .orElse(null);

        if (enrollment == null) {
            enrollment = createNewSectionEnrollment(user, section, order);
        }

        synchronizeProgress(enrollment, user.getId(), section.getCourse().getId());
        log.debug("SectionEnrollment 동기화 - userId={}, sectionId={}, progress={}%",
                user.getId(), section.getId(), enrollment.getProgressPercentage());
        return sectionEnrollmentRepository.save(enrollment);
    }

    @Transactional
    public void updateProgressByLecture(Long userId, Lecture lecture) {
        if (userId == null || lecture == null || lecture.getSection() == null) {
            return;
        }

        Optional<SectionEnrollment> optionalEnrollment = sectionEnrollmentRepository
                .findByUser_IdAndSection_Id(userId, lecture.getSection().getId());

        if (optionalEnrollment.isEmpty()) {
            return;
        }

        SectionEnrollment enrollment = optionalEnrollment.get();
        synchronizeProgress(enrollment, userId, lecture.getCourse().getId());
        sectionEnrollmentRepository.save(enrollment);
    }

    public List<SectionEnrollment> findByUserAndCourse(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return Collections.emptyList();
        }

        return sectionEnrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId);
    }

    public long countByUserAndCourse(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return 0;
        }
        return sectionEnrollmentRepository.countByUser_IdAndCourse_Id(userId, courseId);
    }

    private SectionEnrollment createNewSectionEnrollment(User user, Section section, Order order) {
        long totalLectures = lectureRepository.countByCourseId(section.getCourse().getId());
        return SectionEnrollment.builder()
                .user(user)
                .course(section.getCourse())
                .section(section)
                .order(order)
                .totalLecturesCount((int) totalLectures)
                .build();
    }

    private void synchronizeProgress(SectionEnrollment enrollment, Long userId, Long courseId) {
        long totalLectures = lectureRepository.countByCourseId(courseId);
        long completedLectures = lectureCompletionRepository.countCompletedLecturesByUserAndCourse(userId, courseId);
        enrollment.updateLectureCompletion((int) completedLectures, (int) totalLectures);
    }
}

