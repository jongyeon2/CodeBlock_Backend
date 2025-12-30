package com.studyblock.domain.enrollment.service;

import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.course.service.LectureOwnershipService;
import com.studyblock.domain.enrollment.dto.EnrollmentResponse;
import com.studyblock.domain.enrollment.entity.CourseEnrollment;
import com.studyblock.domain.enrollment.entity.LectureCompletion;
import com.studyblock.domain.enrollment.enums.EnrollmentSource;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.enrollment.repository.LectureCompletionRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private LectureCompletionRepository lectureCompletionRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LectureOwnershipService lectureOwnershipService;

    @Mock
    private SectionEnrollmentService sectionEnrollmentService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("코스 수강신청이 없고 섹션만 보유한 경우 섹션 스냅샷을 반환한다")
    void getEnrollmentByUserAndCourse_returnsSectionSnapshotWhenCourseEnrollmentMissing() {
        Long userId = 10L;
        Long courseId = 20L;

        when(courseEnrollmentRepository.findWithCourseByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.empty());

        Course course = Course.builder()
                .title("테스트 코스")
                .summary("섹션 전용 코스")
                .thumbnailUrl("https://cdn.test/thumb.png")
                .price(0L)
                .discountPercentage(0)
                .categories(Collections.emptyList())
                .build();
        ReflectionTestUtils.setField(course, "id", courseId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(sectionEnrollmentService.findByUserAndCourse(userId, courseId))
                .thenReturn(Collections.emptyList());
        when(lectureRepository.countByCourseId(courseId)).thenReturn(10L);
        when(lectureCompletionRepository.countCompletedLecturesByUserAndCourse(userId, courseId)).thenReturn(3L);

        LectureCompletion completion1 = mock(LectureCompletion.class);
        var lecture1 = mock(com.studyblock.domain.course.entity.Lecture.class);
        when(lecture1.getId()).thenReturn(101L);
        when(completion1.getLecture()).thenReturn(lecture1);

        LectureCompletion completion2 = mock(LectureCompletion.class);
        var lecture2 = mock(com.studyblock.domain.course.entity.Lecture.class);
        when(lecture2.getId()).thenReturn(102L);
        when(completion2.getLecture()).thenReturn(lecture2);

        when(lectureCompletionRepository.findCompletedLecturesByUserAndCourse(userId, courseId))
                .thenReturn(List.of(completion1, completion2));

        when(lectureOwnershipService.getPurchasedSectionIds(userId, courseId))
                .thenReturn(List.of(1L, 2L));

        EnrollmentResponse response = enrollmentService.getEnrollmentByUserAndCourse(userId, courseId);

        assertThat(response.getHasFullCourseAccess()).isFalse();
        assertThat(response.getSnapshotType()).isEqualTo("SECTION");
        assertThat(response.getProgressPercentage()).isEqualByComparingTo("30.00");
        assertThat(response.getCompletedLecturesCount()).isEqualTo(3);
        assertThat(response.getTotalLecturesCount()).isEqualTo(10);
        assertThat(response.getPurchasedSectionIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.getCompletedLectureIds()).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    @DisplayName("코스 수강신청이 존재하면 기존 응답을 유지한다")
    void getEnrollmentByUserAndCourse_returnsCourseEnrollmentWhenExists() {
        Long userId = 30L;
        Long courseId = 40L;

        Course course = Course.builder()
                .title("풀코스")
                .summary("전체 수강")
                .thumbnailUrl("https://cdn.test/full.png")
                .price(1000L)
                .discountPercentage(0)
                .categories(Collections.emptyList())
                .build();
        ReflectionTestUtils.setField(course, "id", courseId);

        User user = User.builder()
                .id(userId)
                .name("수강생")
                .memberId("member-30")
                .password("encoded")
                .phone("010-0000-0000")
                .email("user@test.com")
                .birth(LocalDate.of(1990, 1, 1))
                .gender(1)
                .jointype(1)
                .build();

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .order(null)
                .enrollmentSource(EnrollmentSource.PURCHASE_COOKIE)
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 555L);
        enrollment.updateLectureCompletion(5, 10);
        enrollment.updateQuizCompletion(1, 3);
        ReflectionTestUtils.setField(enrollment, "progressPercentage", new BigDecimal("50.00"));

        when(courseEnrollmentRepository.findWithCourseByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(enrollment));
        when(lectureCompletionRepository.findCompletedLectureIdsByEnrollmentId(555L))
                .thenReturn(List.of(201L, 202L));
        when(lectureOwnershipService.getPurchasedSectionIds(userId, courseId))
                .thenReturn(List.of(11L, 12L));

        EnrollmentResponse response = enrollmentService.getEnrollmentByUserAndCourse(userId, courseId);

        assertThat(response.getHasFullCourseAccess()).isTrue();
        assertThat(response.getSnapshotType()).isEqualTo("COURSE");
        assertThat(response.getProgressPercentage()).isEqualByComparingTo("50.00");
        assertThat(response.getCompletedLecturesCount()).isEqualTo(5);
        assertThat(response.getTotalLecturesCount()).isEqualTo(10);
        assertThat(response.getCompletedLectureIds()).containsExactlyInAnyOrder(201L, 202L);
        assertThat(response.getPurchasedSectionIds()).containsExactlyInAnyOrder(11L, 12L);
    }
}

