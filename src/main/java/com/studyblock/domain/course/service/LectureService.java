package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.LectureRequest;
import com.studyblock.domain.course.dto.LectureSummaryResponse;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.course.repository.SectionRepository;
import com.studyblock.domain.course.repository.VideoRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 강의 비즈니스 로직을 처리하는 서비스 클래스
 *
 * 개선 사항:
 * - 강의 메타 정보 수정·삭제 기능 추가
 * - Video API와 분리하여 메타 정보만 관리
 * - 권한 검증 로직 통합 (INSTRUCTOR + 소유자)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LectureService {

    private final LectureRepository lectureRepository;
    private final VideoRepository videoRepository;
    private final S3StorageService s3StorageService;
    private final SectionRepository sectionRepository;

    /**
     * 코스 ID로 강의 목록 조회
     * @param courseId 코스 ID
     * @return 강의 목록
     */
    public List<Lecture> getLecturesByCourseId(Long courseId) {
        log.info("코스 ID {}로 강의 목록 조회 요청", courseId);

        List<Lecture> lectures = lectureRepository.findByCourseIdOrderBySequenceAsc(courseId);

        log.info("코스 ID {}의 강의 개수: {}", courseId, lectures.size());

        return lectures;
    }

    /**
     * 강의 생성
     * @param request 강의 생성 요청 데이터 (sectionId, title, description, sequence, status)
     * @param currentUser 현재 로그인한 사용자
     * @return 생성된 강의 응답 DTO
     */
    @Transactional
    public LectureSummaryResponse createLecture(LectureRequest request, User currentUser) {
        log.info("강의 생성 시작 - Section ID: {}, Title: {}", request.getSectionId(), request.getTitle());

        // 1. 인증 확인
        if (currentUser == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 2. 강사 프로필 검증
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 강의를 생성할 수 있습니다.");
        }
        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 3. sectionId 검증 (생성 시 필수)
        if (request.getSectionId() == null) {
            throw new IllegalArgumentException("섹션 ID는 필수입니다.");
        }

        // 4. Section 조회 및 검증
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID: " + request.getSectionId()));

        // 5. Section이 속한 Course 조회
        Course course = section.getCourse();
        if (course == null) {
            throw new IllegalStateException("섹션에 연결된 코스를 찾을 수 없습니다. Section ID: " + request.getSectionId());
        }

        // 6. 권한 검증: 강사이면서 코스 소유자인지 확인
        verifyCourseOwnership(course, currentUser);

        // 7. 강의 생성
        Lecture lecture = Lecture.builder()
                .course(course)
                .section(section)
                .instructor(instructorProfile)
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(null)  // 썸네일은 별도 API로 업로드
                .uploadDate(LocalDate.now())
                .sequence(request.getSequence())
                .isFree(false)  // 기본값: 무료 강의 아님
                .build();

        // 8. 상태 설정 (Builder에서 기본값 ACTIVE로 설정되지만, 요청된 상태로 변경)
        lecture.updateStatus(request.getStatus());

        // 9. 저장
        Lecture savedLecture = lectureRepository.save(lecture);

        log.info("강의 생성 완료 - Lecture ID: {}, Section ID: {}, Title: {}", 
                savedLecture.getId(), section.getId(), savedLecture.getTitle());

        // 10. DTO 변환하여 반환
        return LectureSummaryResponse.from(savedLecture);
    }

    /**
     * 강의 메타 정보 수정
     * @param lectureId 강의 ID
     * @param request 수정 요청 데이터 (title, description, sequence, status)
     *                주의: request.sectionId는 무시됨 (섹션 변경 불가)
     * @param currentUser 현재 로그인한 사용자
     * @return 수정된 강의 응답 DTO
     */
    @Transactional
    public LectureSummaryResponse updateLecture(Long lectureId, LectureRequest request, User currentUser) {
        log.info("강의 메타 정보 수정 시작 - Lecture ID: {}", lectureId);

        // 1. 인증 확인
        if (currentUser == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 2. 강의 조회
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID: " + lectureId));

        // 3. 권한 검증: 강사이면서 강의 소유자인지 확인
        verifyLectureOwnership(lecture, currentUser);

        // 4. 메타 정보 업데이트 (Video 관련 정보는 별도 API 사용)
        // 주의: request.sectionId는 무시됨 (섹션 변경은 불가능)
        lecture.updateInfo(request.getTitle(), request.getDescription(), lecture.getThumbnailUrl());
        lecture.updateSequence(request.getSequence());
        lecture.updateStatus(request.getStatus());

        // 5. 변경사항 저장 (@Transactional로 자동 저장되지만 명시적으로 save 호출)
        lectureRepository.save(lecture);

        log.info("강의 메타 정보 수정 완료 - Lecture ID: {}, 새 상태: {}", lectureId, request.getStatus());

        // 6. DTO 변환하여 반환
        return LectureSummaryResponse.from(lecture);
    }

    /**
     * 강의 삭제 (연관된 비디오 및 S3 파일 모두 삭제)
     * @param lectureId 강의 ID
     * @param currentUser 현재 로그인한 사용자
     */
    @Transactional
    public void deleteLecture(Long lectureId, User currentUser) {
        log.info("강의 삭제 시작 - Lecture ID: {}", lectureId);

        // 1. 인증 확인
        if (currentUser == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 2. 강의 조회
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID: " + lectureId));

        // 3. 권한 검증: 강사이면서 강의 소유자인지 확인
        verifyLectureOwnership(lecture, currentUser);

        // 4. 연관된 모든 비디오 조회 및 S3 파일 삭제
        List<Video> videos = videoRepository.findByLectureId(lectureId);
        for (Video video : videos) {
            // S3에서 모든 해상도 비디오 파일 삭제
            deleteVideoFiles(video);
        }

        // 5. DB에서 강의 삭제 (cascade로 Video, Quiz, LectureResource도 함께 삭제됨)
        lectureRepository.delete(lecture);

        log.info("강의 삭제 완료 - Lecture ID: {}, 삭제된 비디오 개수: {}", lectureId, videos.size());
    }

    /**
     * 강의 소유자 권한 검증
     * @param lecture 강의 엔티티
     * @param currentUser 현재 로그인한 사용자
     */
    private void verifyLectureOwnership(Lecture lecture, User currentUser) {
        // currentUser null 체크 (이미 상위 메서드에서 체크하지만 방어적으로 추가)
        if (currentUser == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 강의를 관리할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 강의의 강사와 현재 사용자의 강사 프로필이 일치하지 않으면 권한 없음
        if (lecture.getInstructor() == null ||
            !lecture.getInstructor().getId().equals(instructorProfile.getId())) {
            throw new IllegalArgumentException("해당 강의의 소유자만 강의를 관리할 수 있습니다.");
        }
    }

    /**
     * 코스 소유자 권한 검증
     * @param course 코스 엔티티
     * @param currentUser 현재 로그인한 사용자
     */
    private void verifyCourseOwnership(Course course, User currentUser) {
        // currentUser null 체크 (이미 상위 메서드에서 체크하지만 방어적으로 추가)
        if (currentUser == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 강의를 관리할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 코스의 강사와 현재 사용자의 강사 프로필이 일치하지 않으면 권한 없음
        if (course.getInstructor() == null ||
            !course.getInstructor().getId().equals(instructorProfile.getId())) {
            throw new IllegalArgumentException("해당 코스의 소유자만 강의를 관리할 수 있습니다.");
        }
    }

    /**
     * S3에서 비디오 관련 파일들 삭제
     * @param video 비디오 엔티티
     */
    private void deleteVideoFiles(Video video) {
        // 원본 비디오 삭제
        if (video.getOriginalUrl() != null) {
            s3StorageService.deleteFile(video.getOriginalUrl());
            log.info("S3 원본 비디오 파일 삭제 완료 - URL: {}", video.getOriginalUrl());
        }

        // 인코딩된 비디오들 삭제
        if (video.getUrl1080p() != null) {
            s3StorageService.deleteFile(video.getUrl1080p());
            log.info("S3 1080p 비디오 파일 삭제 완료 - URL: {}", video.getUrl1080p());
        }
        if (video.getUrl720p() != null) {
            s3StorageService.deleteFile(video.getUrl720p());
            log.info("S3 720p 비디오 파일 삭제 완료 - URL: {}", video.getUrl720p());
        }
        if (video.getUrl540p() != null) {
            s3StorageService.deleteFile(video.getUrl540p());
            log.info("S3 540p 비디오 파일 삭제 완료 - URL: {}", video.getUrl540p());
        }

        // 썸네일 삭제
        if (video.getThumbnailUrl() != null) {
            s3StorageService.deleteFile(video.getThumbnailUrl());
            log.info("S3 썸네일 파일 삭제 완료 - URL: {}", video.getThumbnailUrl());
        }

        // 자막 삭제
        if (video.getSubtitleUrl() != null) {
            s3StorageService.deleteFile(video.getSubtitleUrl());
            log.info("S3 자막 파일 삭제 완료 - URL: {}", video.getSubtitleUrl());
        }
    }
}
