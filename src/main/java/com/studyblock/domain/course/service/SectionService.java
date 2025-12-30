package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.LectureSummaryResponse;
import com.studyblock.domain.course.dto.SectionRequest;
import com.studyblock.domain.course.dto.SectionResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.enums.LectureStatus;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.SectionRepository;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.InstructorProfileRepository;
import com.studyblock.domain.user.repository.LectureOwnershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LectureOwnershipRepository lectureOwnershipRepository;

    private static final EnumSet<LectureStatus> VISIBLE_LECTURE_STATUSES =
            EnumSet.of(LectureStatus.ACTIVE, LectureStatus.PUBLISHED);

    /**
     * 코스 ID로 섹션 목록 조회 (강의 포함)
     * 권한 검증 포함: 사용자가 구매한 섹션만 반환
     */
    public List<SectionResponse> getSectionsByCourseId(Long courseId) {
        log.info("코스 ID {}의 섹션 목록 조회", courseId);

        // 코스 존재 여부 확인
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId);
        }

        List<Section> sections = sectionRepository.findByCourseIdWithLectures(courseId);

        return sections.stream()
                .map(section -> buildSectionResponse(section, true))
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 권한에 따라 섹션 목록 조회 (권한 필터링)
     * - 전체 코스 수강: 모든 섹션 반환
     * - 섹션 단위 구매: 구매한 섹션만 반환
     * - 강사 본인: 모든 섹션 반환
     * - 권한 없음: 빈 리스트 반환
     */
    public List<SectionResponse> getSectionsByCourseIdForUser(Long courseId, Long userId) {
        log.info("코스 ID {}의 섹션 목록 조회 (사용자 ID: {})", courseId, userId);

        // 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        boolean isAuthenticated = userId != null;

        boolean isInstructorOwner = false;
        boolean hasFullCourseAccess = false;
        Set<Long> ownedSectionIds = Set.of();

        if (isAuthenticated) {
            if (course.getInstructor() != null &&
                    course.getInstructor().getUser() != null &&
                    course.getInstructor().getUser().getId().equals(userId)) {
                isInstructorOwner = true;
            }

            hasFullCourseAccess = courseEnrollmentRepository.existsByUserIdAndCourseId(userId, courseId);

            if (!hasFullCourseAccess && !isInstructorOwner) {
                List<LectureOwnership> ownerships = lectureOwnershipRepository.findActiveOwnershipsByUserId(userId);
                ownedSectionIds = ownerships.stream()
                        .filter(LectureOwnership::isActive)
                        .filter(ownership -> ownership.getSection().getCourse().getId().equals(courseId))
                        .map(ownership -> ownership.getSection().getId())
                        .collect(Collectors.toSet());
            }
        }

        List<Section> allSections = sectionRepository.findByCourseIdWithLectures(courseId);

        if (allSections.isEmpty()) {
            log.info("섹션이 없음 - 플랫 모드: userId={}, courseId={}", userId, courseId);
            return List.of();
        }

        final boolean grantAllSections = isInstructorOwner || hasFullCourseAccess;
        final Set<Long> finalOwnedSectionIds = ownedSectionIds;

        return allSections.stream()
                .map(section -> {
                    boolean sectionAccess = grantAllSections || finalOwnedSectionIds.contains(section.getId());
                    return buildSectionResponse(section, sectionAccess);
                })
                .collect(Collectors.toList());
    }

    private SectionResponse buildSectionResponse(Section section, boolean hasAccess) {
        List<LectureSummaryResponse> lectures = mapLectures(section, false, hasAccess);
        return SectionResponse.fromWithLecturesForUser(section, lectures, hasAccess);
    }

    private List<LectureSummaryResponse> mapLectures(Section section,
                                                     boolean includeHidden,
                                                     boolean hasSectionAccess) {
        return section.getLectures().stream()
                .filter(lecture -> includeHidden || isVisibleStatus(lecture))
                .map(lecture -> {
                    LectureSummaryResponse response = LectureSummaryResponse.from(lecture);

                    boolean lectureAccessible = hasSectionAccess;

                    if (!lectureAccessible) {
                        boolean isExplicitlyFree = Boolean.TRUE.equals(response.getIsFree());
                        boolean sectionIsFree = section.getCookiePrice() != null && section.getCookiePrice() <= 0;
                        boolean hasPreviewVideo = Boolean.TRUE.equals(response.getHasPreviewVideo());

                        log.debug("[SectionService] lecture access check - lectureId={}, sectionId={}, hasSectionAccess={}, isExplicitlyFree={}, sectionIsFree={}, hasPreviewVideo={}, finalAccessBeforePreview={}",
                                response.getId(), section.getId(), hasSectionAccess, isExplicitlyFree, sectionIsFree, hasPreviewVideo, lectureAccessible);

                        if (!lectureAccessible && sectionIsFree && isExplicitlyFree) {
                            lectureAccessible = true;
                        }
                    }

                    response.setHasAccess(lectureAccessible);
                    response.setLocked(!lectureAccessible);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private boolean isVisibleStatus(Lecture lecture) {
        LectureStatus status = lecture.getStatus();
        return status != null && VISIBLE_LECTURE_STATUSES.contains(status);
    }

    /**
     * 섹션 ID로 섹션 상세 조회 (강의 포함)
     */
    public SectionResponse getSectionById(Long sectionId) {
        log.info("섹션 ID {} 상세 조회", sectionId);
        
        Section section = sectionRepository.findByIdWithLectures(sectionId);
        
        if (section == null) {
            throw new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId);
        }

        return SectionResponse.fromWithLectures(section);
    }

    /**
     * 섹션 ID로 해당 섹션의 강의 목록 조회
     */
    public List<LectureSummaryResponse> getLecturesBySectionId(Long sectionId) {
        log.info("섹션 ID {}의 강의 목록 조회", sectionId);
        
        Section section = sectionRepository.findByIdWithLectures(sectionId);
        
        if (section == null) {
            throw new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId);
        }

        return section.getLectures().stream()
                .map(LectureSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 섹션 생성
     */
    @Transactional
    public SectionResponse createSection(Long courseId, SectionRequest request, User currentUser) {
        log.info("코스 ID {}에 새 섹션 생성: {}", courseId, request.getTitle());

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        // 권한 검증: 강사이면서 코스 소유자인지 확인
        verifyCourseOwnership(course, currentUser);

        Section section = Section.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .sequence(request.getSequence() != null ? request.getSequence() : getNextSequence(courseId))
                .cookiePrice(request.getCookiePrice())
                .discountPercentage(request.getDiscountPercentage())
                .build();

        Section savedSection = sectionRepository.save(section);

        return SectionResponse.from(savedSection);
    }

    /**
     * 섹션 수정
     */
    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest request, User currentUser) {
        log.info("섹션 ID {} 수정", sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId));

        // 권한 검증: 강사이면서 코스 소유자인지 확인
        verifyCourseOwnership(section.getCourse(), currentUser);

        // 모든 필드 업데이트
        section.updateInfo(request.getTitle(), request.getDescription());
        section.updateSequence(request.getSequence());
        section.updatePricing(request.getCookiePrice(), request.getDiscountPercentage());

        return SectionResponse.from(section);
    }

    /**
     * 섹션 삭제
     */
    @Transactional
    public void deleteSection(Long sectionId, User currentUser) {
        log.info("섹션 ID {} 삭제", sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId));

        // 권한 검증: 강사이면서 코스 소유자인지 확인
        verifyCourseOwnership(section.getCourse(), currentUser);

        sectionRepository.deleteById(sectionId);
    }

    /**
     * 섹션 순서 변경
     */
    @Transactional
    public void reorderSections(Long courseId, List<Long> orderedSectionIds, User currentUser) {
        log.info("코스 ID {}의 섹션 순서 변경", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        // 권한 검증: 강사이면서 코스 소유자인지 확인
        verifyCourseOwnership(course, currentUser);

        // 해당 코스의 섹션들 조회
        List<Section> sections = sectionRepository.findByCourseIdOrderBySequenceAsc(courseId);

        // 요청된 섹션 ID들이 모두 해당 코스의 섹션인지 확인
        List<Long> existingSectionIds = sections.stream()
                .map(Section::getId)
                .collect(Collectors.toList());

        if (!existingSectionIds.containsAll(orderedSectionIds) ||
            existingSectionIds.size() != orderedSectionIds.size()) {
            throw new IllegalArgumentException("유효하지 않은 섹션 ID가 포함되어 있거나 섹션 개수가 일치하지 않습니다.");
        }

        // 순서 업데이트
        for (int i = 0; i < orderedSectionIds.size(); i++) {
            Long sectionId = orderedSectionIds.get(i);
            Section section = sections.stream()
                    .filter(s -> s.getId().equals(sectionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId));

            section.updateSequence(i + 1);
        }
    }

    /**
     * 코스 소유자 권한 검증
     */
    private void verifyCourseOwnership(Course course, User currentUser) {
        // currentUser null 체크
        if (currentUser == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 섹션을 관리할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 코스의 강사와 현재 사용자의 강사 프로필이 일치하지 않으면 권한 없음
        if (course.getInstructor() == null || 
            !course.getInstructor().getId().equals(instructorProfile.getId())) {
            throw new IllegalArgumentException("해당 코스의 소유자만 섹션을 관리할 수 있습니다.");
        }
    }

    /**
     * 다음 sequence 번호 조회
     */
    private Integer getNextSequence(Long courseId) {
        List<Section> sections = sectionRepository.findByCourseIdOrderBySequenceAsc(courseId);

        if (sections.isEmpty()) {
            return 1;
        }

        return sections.get(sections.size() - 1).getSequence() + 1;
    }
}

