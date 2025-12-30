package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.VideoResponse;
import com.studyblock.domain.course.dto.VideoStreamResponse;
import com.studyblock.domain.course.dto.VideoUploadResponse;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.enums.LectureStatus;
import com.studyblock.domain.course.enums.OwnershipStatus;
import com.studyblock.domain.course.event.VideoUploadedEvent;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.course.repository.VideoRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository;
import com.studyblock.domain.user.repository.LectureOwnershipRepository;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.EnumSet;
import org.springframework.security.access.AccessDeniedException;

/**
 * 비디오 비즈니스 로직을 처리하는 통합 서비스 클래스
 *
 * 개선 사항:
 * - VideoUploadService와 통합하여 불필요한 래퍼 제거
 * - S3 업로드/삭제, DB 조회/수정, DTO 변환을 한 곳에서 관리
 * - Controller에서 분리된 모든 비즈니스 로직을 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 (성능 최적화)
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final LectureRepository lectureRepository;
    private final S3StorageService s3StorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LectureOwnershipRepository lectureOwnershipRepository;

    private static final EnumSet<LectureStatus> VISIBLE_LECTURE_STATUSES =
            EnumSet.of(LectureStatus.ACTIVE, LectureStatus.PUBLISHED);

    /**
     * 비디오 업로드 처리 (S3 업로드 + DB 저장)
     * @param lectureId 강의 ID
     * @param videoFile 비디오 파일
     * @param thumbnailFile 썸네일 파일 (선택)
     * @param targetResolution 목표 인코딩 해상도 (현재는 720p만 인코딩, 1080p, 540p는 미지원)
     * @param currentUser 현재 로그인한 사용자
     * @return 업로드된 비디오 응답 DTO
     */
    @Transactional // 쓰기 작업이므로 트랜잭션 활성화
    public VideoUploadResponse uploadVideo(Long lectureId, MultipartFile videoFile, MultipartFile thumbnailFile,
                                          User currentUser) {
        // 1. 파일 검증
        if (videoFile.isEmpty()) {
            throw new IllegalArgumentException("비디오 파일이 비어있습니다.");
        }

        log.info("비디오 업로드 시작 - Lecture ID: {}, 파일명: {}",
                lectureId, videoFile.getOriginalFilename());

        // 2. Lecture 존재 확인
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID: " + lectureId));

        // 3. 권한 검증: 강사이면서 강의 소유자인지 확인
        verifyLectureOwnership(lecture, currentUser);

        // 4. 비디오 파일 S3에 업로드
        String videoUrl = s3StorageService.uploadFile(
                videoFile,
                "videos/lecture-" + lectureId
        );
        log.info("비디오 S3 업로드 완료 - Lecture ID: {}, URL: {}", lectureId, videoUrl);

        // 5. 썸네일 업로드 (선택사항)
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = s3StorageService.uploadFile(
                    thumbnailFile,
                    "thumbnails/lecture-" + lectureId
            );
            log.info("썸네일 S3 업로드 완료 - Lecture ID: {}, URL: {}", lectureId, thumbnailUrl);
        }

        // 6. Video 엔티티 생성 및 DB 저장
        Video video = Video.builder()
                .lecture(lecture)
                .name(videoFile.getOriginalFilename())
                .originalUrl(videoUrl)
                .url1080p(null)  // 인코딩 후 업데이트 예정
                .url720p(null)   // 인코딩 후 업데이트 예정
                .url540p(null)   // 인코딩 후 업데이트 예정
                .thumbnailUrl(thumbnailUrl)
                .fileSize(videoFile.getSize())
                .build();

        Video savedVideo = videoRepository.save(video);
        log.info("Video 엔티티 DB 저장 완료 - Video ID: {}, encodingStatus: {}",
                savedVideo.getId(), savedVideo.getEncodingStatus());

        // 7. 비디오 업로드 완료 이벤트 발행
        log.info("📢 VideoUploadedEvent 발행 - Video ID: {}", savedVideo.getId());
        eventPublisher.publishEvent(new VideoUploadedEvent(savedVideo.getId()));

        // 8. DTO 변환하여 반환
        VideoUploadResponse response = VideoUploadResponse.builder()
                .id(savedVideo.getId())
                .lectureId(lectureId)  // 이미 조회한 lectureId 직접 사용
                .originalUrl(savedVideo.getOriginalUrl())
                .thumbnailUrl(savedVideo.getThumbnailUrl())
                .fileName(savedVideo.getName())
                .fileSize(savedVideo.getFileSize())
                .encodingStatus(savedVideo.getEncodingStatus())
                .createdAt(savedVideo.getCreatedAt())
                .build();
        
        log.info("📤 VideoUploadResponse 생성 완료 - Video ID: {}, Lecture ID: {}", response.getId(), response.getLectureId());
        return response;
    }

    /**
     * 자막 파일 업로드
     * @param videoId 비디오 ID
     * @param subtitleFile 자막 파일
     * @param currentUser 현재 로그인한 사용자
     */
    @Transactional
    public void uploadSubtitle(Long videoId, MultipartFile subtitleFile, User currentUser) {
        log.info("자막 업로드 시작 - Video ID: {}", videoId);

        // 비디오 조회
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        // 권한 검증: 강사이면서 강의 소유자인지 확인
        verifyLectureOwnership(video.getLecture(), currentUser);

        // S3에 자막 파일 업로드
        String subtitleUrl = s3StorageService.uploadFile(
                subtitleFile,
                "subtitles/lecture-" + video.getLecture().getId()
        );

        log.info("자막 S3 업로드 완료 - Video ID: {}, URL: {}", videoId, subtitleUrl);
        // TODO: Video 엔티티에 subtitleUrl 업데이트 메서드 추가 필요
    }

    /**
     * 비디오 스트리밍 URL 조회
     * @param videoId 비디오 ID
     * @return 스트리밍 URL 응답 DTO
     */
    public VideoStreamResponse getStreamUrl(Long videoId, User currentUser) {
        log.info("스트리밍 URL 요청 - Video ID: {}", videoId);

        if (currentUser == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        Video video = videoRepository.findByIdWithLecture(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        Lecture lecture = video.getLecture();
        if (lecture == null) {
            throw new IllegalStateException("비디오에 연결된 강의를 찾을 수 없습니다. Video ID: " + videoId);
        }

        boolean isInstructorOwner = isInstructorOfLecture(currentUser, lecture);
        if (!isInstructorOwner) {
            ensureLectureVisibleForLearner(currentUser, lecture);
        }

        if (video.getEncodingStatus() != EncodingStatus.COMPLETED) {
            log.warn("인코딩이 완료되지 않은 비디오에 대한 스트리밍 URL 요청 - Video ID: {}, 현재 상태: {}",
                    videoId, video.getEncodingStatus());
            throw new com.studyblock.domain.course.exception.VideoEncodingNotCompletedException(
                    videoId, video.getEncodingStatus()
            );
        }

        String videoUrl = getAvailableVideoUrl(video);

        String streamingUrl = s3StorageService.generatePresignedUrl(videoUrl, 60);
        log.info("스트리밍 URL 생성 완료 - Video ID: {}", videoId);

        return VideoStreamResponse.of(videoId, streamingUrl, 60);
    }

    /**
     * 사용 가능한 비디오 URL 조회 (우선순위: 720p -> 1080p -> 540p -> 원본)
     * 변경 사항: 720p가 기본 인코딩 해상도이므로 우선순위 최상위로 변경
     * @param video 비디오 엔티티
     * @return 사용 가능한 비디오 URL
     */
    private String getAvailableVideoUrl(Video video) {
        if (video.getUrl720p() != null) {
            return video.getUrl720p();
        } else if (video.getUrl1080p() != null) {
            return video.getUrl1080p();
        } else if (video.getUrl540p() != null) {
            return video.getUrl540p();
        } else if (video.getOriginalUrl() != null) {
            return video.getOriginalUrl();
        } else {
            throw new IllegalStateException("사용 가능한 비디오 URL이 없습니다. Video ID: " + video.getId());
        }
    }

    private boolean isInstructorOfLecture(User currentUser, Lecture lecture) {
        if (currentUser.getInstructorProfile() == null || lecture.getInstructor() == null) {
            return false;
        }
        return lecture.getInstructor().getId().equals(currentUser.getInstructorProfile().getId());
    }

    private void ensureLectureVisibleForLearner(User currentUser, Lecture lecture) {
        LectureStatus status = lecture.getStatus();
        if (status == null || !VISIBLE_LECTURE_STATUSES.contains(status)) {
            throw new AccessDeniedException("시청 권한이 없습니다.");
        }

        Long userId = currentUser.getId();
        Long courseId = lecture.getCourse().getId();

        boolean hasCourseEnrollment = courseEnrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
        if (hasCourseEnrollment) {
            return;
        }

        boolean ownsSection = lectureOwnershipRepository.existsByUserAndSectionAndStatus(
                currentUser,
                lecture.getSection(),
                OwnershipStatus.ACTIVE
        );

        if (!ownsSection) {
            throw new AccessDeniedException("시청 권한이 없습니다.");
        }
    }

    /**
     * 특정 강의의 비디오 목록 조회 (페이징 지원)
     * @param lectureId 강의 ID
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 비디오 목록
     */
    public Page<VideoResponse> getVideosByLecture(Long lectureId, Pageable pageable) {
        log.info("강의 비디오 목록 조회 (페이징) - Lecture ID: {}, Page: {}, Size: {}",
                lectureId, pageable.getPageNumber(), pageable.getPageSize());

        // Repository에서 페이징 조회
        Page<Video> videoPage = videoRepository.findByLectureId(lectureId, pageable);

        // DTO 변환 (Page.map()을 사용하여 효율적으로 변환)
        return videoPage.map(VideoResponse::fromSimple);
    }

    /**
     * 특정 강의의 비디오 목록 조회 (전체 목록)
     * @param lectureId 강의 ID
     * @return 비디오 응답 DTO 리스트
     */
    public List<VideoResponse> getAllVideosByLecture(Long lectureId) {
        log.info("강의 비디오 전체 목록 조회 - Lecture ID: {}", lectureId);

        List<Video> videos = videoRepository.findByLectureId(lectureId);

        // DTO 변환
        return videos.stream()
                .map(VideoResponse::fromSimple)
                .collect(Collectors.toList());
    }

    /**
     * 비디오 상세 조회
     * @param videoId 비디오 ID
     * @return 비디오 상세 응답 DTO
     */
    public VideoResponse getVideo(Long videoId) {
        log.info("비디오 상세 조회 - Video ID: {}", videoId);

        // Lecture Fetch Join으로 N+1 문제 방지
        Video video = videoRepository.findByIdWithLecture(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        // DTO 변환
        return VideoResponse.from(video);
    }

    /**
     * 비디오 삭제 (S3 파일 삭제 + DB 삭제)
     * @param videoId 비디오 ID
     * @param currentUser 현재 로그인한 사용자
     */
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public void deleteVideo(Long videoId, User currentUser) {
        log.info("=== 비디오 삭제 시작 ===");
        log.info("Video ID: {}, User ID: {}", videoId, 
                currentUser != null ? currentUser.getId() : "null");

        // 인증 확인
        if (currentUser == null) {
            log.error("인증 실패 - currentUser가 null입니다");
            throw new IllegalArgumentException("로그인이 필요합니다. 토큰이 없거나 만료되었습니다.");
        }

        // 비디오 조회 및 권한 검증
        log.info("비디오 조회 및 권한 검증 시작 - Video ID: {}", videoId);
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        // 권한 검증: 강사이면서 강의 소유자인지 확인
        Lecture lecture = video.getLecture();
        verifyLectureOwnership(lecture, currentUser);
        log.info("비디오 조회 및 권한 검증 완료 - Video ID: {}, Lecture ID: {}", 
                videoId, lecture.getId());

        // S3에서 모든 해상도 비디오 파일 삭제
        log.info("S3 파일 삭제 시작 - Video ID: {}", videoId);
        if (video.getOriginalUrl() != null) {
            s3StorageService.deleteFile(video.getOriginalUrl());
            log.info("S3 원본 비디오 파일 삭제 완료 - URL: {}", video.getOriginalUrl());
        }
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

        // 썸네일이 있으면 삭제
        if (video.getThumbnailUrl() != null) {
            s3StorageService.deleteFile(video.getThumbnailUrl());
            log.info("S3 썸네일 파일 삭제 완료 - URL: {}", video.getThumbnailUrl());
        }

        // 자막이 있으면 삭제
        if (video.getSubtitleUrl() != null) {
            s3StorageService.deleteFile(video.getSubtitleUrl());
            log.info("S3 자막 파일 삭제 완료 - URL: {}", video.getSubtitleUrl());
        }
        log.info("S3 파일 삭제 완료 - Video ID: {}", videoId);

        // DB에서 삭제
        // 양방향 관계를 끊어야 orphanRemoval이 정상 작동함
        // Lecture.video를 null로 설정하면 orphanRemoval=true가 자동으로 삭제 처리
        log.info("=== DB 삭제 시작 ===");
        log.info("Video ID: {}, Lecture ID: {}", videoId, lecture.getId());
        
        // 양방향 관계 해제: Lecture에서 video 참조 제거
        // 이렇게 하면 orphanRemoval이 자동으로 Video를 삭제함
        log.info("Lecture의 video 참조 제거 시작");
        lecture.removeVideo();
        lectureRepository.save(lecture);
        log.info("Lecture의 video 참조 제거 완료 및 저장 완료");
        
        // flush를 강제로 실행하여 즉시 DB에 반영
        lectureRepository.flush();
        log.info("Lecture flush() 완료");
        
        // 삭제 후 존재 여부 확인
        boolean 삭제_후_존재 = videoRepository.existsById(videoId);
        log.info("삭제 후 Video 존재 여부 확인 - Video ID: {}, 존재: {}", 
                videoId, 삭제_후_존재);
        
        if (삭제_후_존재) {
            log.error("❌ Video 삭제 후에도 여전히 존재합니다 - Video ID: {}", videoId);
            throw new RuntimeException("Video DB 삭제가 실패했습니다 - Video ID: " + videoId);
        }
        
        log.info("✅ Video 엔티티 DB 삭제 완료 - Video ID: {}", videoId);
        log.info("=== 비디오 삭제 완료 ===");
    }

    /**
     * 비디오 인코딩 상태 업데이트
     * @param videoId 비디오 ID
     * @param status 변경할 인코딩 상태 (Enum)
     * @param currentUser 현재 로그인한 사용자
     * @return 업데이트된 비디오 응답 DTO
     */
    @Transactional
    public VideoResponse updateEncodingStatus(Long videoId, EncodingStatus status, User currentUser) {
        log.info("인코딩 상태 업데이트 - Video ID: {}, Status: {}", videoId, status);

        // 비디오 조회
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        // 권한 검증: 강사이면서 강의 소유자인지 확인
        verifyLectureOwnership(video.getLecture(), currentUser);

        // Enum 기반 상태 업데이트 (switch expression 사용)
        switch (status) {
            case PROCESSING -> video.startEncoding();
            case COMPLETED -> video.completeEncoding();
            case FAILED -> video.failEncoding();
            case PENDING -> {
                // PENDING 상태로 되돌리는 것은 비즈니스 로직상 허용하지 않을 수도 있음
                log.warn("PENDING 상태로 되돌리기는 권장되지 않습니다. Video ID: {}", videoId);
            }
        }

        // 변경사항 저장 (@Transactional로 자동 저장되지만 명시적으로 save 호출)
        videoRepository.save(video);

        // DTO 변환 및 반환
        return VideoResponse.fromSimple(video);
    }

    /**
     * 비디오 기본 해상도 변경
     * @param videoId 비디오 ID
     * @param resolution 변경할 해상도
     * @param currentUser 현재 로그인한 사용자
     */
    @Transactional
    public void updateDefaultResolution(Long videoId, String resolution, User currentUser) {
        log.info("비디오 기본 해상도 변경 - ID: {}, Resolution: {}", videoId, resolution);

        if (!isValidResolution(resolution)) {
            throw new com.studyblock.domain.course.exception.InvalidResolutionException(resolution);
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        verifyLectureOwnership(video.getLecture(), currentUser);

        video.updateDefaultResolution(resolution);
        videoRepository.save(video);
    }

    /**
     * 해상도 유효성 검증
     * @param resolution 해상도 문자열
     * @return 유효하면 true
     */
    private boolean isValidResolution(String resolution) {
        if (resolution == null) {
            return false;
        }
        return resolution.equals("1080p") || resolution.equals("720p") || resolution.equals("540p");
    }

    /**
     * 강의 소유자 권한 검증
     * @param lecture 강의 엔티티
     * @param currentUser 현재 로그인한 사용자
     */
    private void verifyLectureOwnership(Lecture lecture, User currentUser) {
        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 비디오를 관리할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 강의의 강사와 현재 사용자의 강사 프로필이 일치하지 않으면 권한 없음
        if (!lecture.getInstructor().getId().equals(instructorProfile.getId())) {
            throw new IllegalArgumentException("해당 강의의 소유자만 비디오를 관리할 수 있습니다.");
        }
    }
}