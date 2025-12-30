package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.PreviewVideoResponse;
import com.studyblock.domain.course.dto.PreviewVideoStreamResponse;
import com.studyblock.domain.course.dto.PreviewVideoUploadResponse;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.event.PreviewVideoUploadedEvent;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.course.repository.PreviewVideoRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 맛보기 비디오 비즈니스 로직 처리 서비스
 * - VideoService의 로직을 재사용하되, 1:1 관계 특성 반영
 * - Lecture당 하나의 PreviewVideo만 허용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PreviewVideoService {

    private final PreviewVideoRepository previewVideoRepository;
    private final LectureRepository lectureRepository;
    private final S3StorageService s3StorageService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 맛보기 비디오 업로드 처리 (S3 업로드 + DB 저장)
     * @param lectureId 강의 ID
     * @param videoFile 비디오 파일
     * @param thumbnailFile 썸네일 파일 (선택)
     * @param targetResolution 목표 인코딩 해상도 (현재는 720p만 인코딩, 1080p, 540p는 미지원)
     * @param currentUser 현재 로그인한 사용자
     * @return 업로드된 맛보기 비디오 응답 DTO
     */
    @Transactional(readOnly = false)
    public PreviewVideoUploadResponse uploadPreviewVideo(Long lectureId, MultipartFile videoFile, 
                                                         MultipartFile thumbnailFile, User currentUser) {
        // 0. 인증 확인
        if (currentUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다. 토큰이 없거나 만료되었습니다.");
        }

        // 1. 파일 검증
        if (videoFile.isEmpty()) {
            throw new IllegalArgumentException("비디오 파일이 비어있습니다.");
        }

        // 2. 파일 크기 제한 (500MB)
        long maxFileSize = 500 * 1024 * 1024L; // 500MB
        if (videoFile.getSize() > maxFileSize) {
            throw new IllegalArgumentException("파일 크기는 500MB를 초과할 수 없습니다.");
        }

        log.info("맛보기 비디오 업로드 시작 - Lecture ID: {}, 파일명: {}",
                lectureId, videoFile.getOriginalFilename());

        // 3. Lecture 존재 확인 (기본 조회로 충분 - PreviewVideo 빌더에서 ID만 필요)
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다. ID: " + lectureId));

        // 4. 권한 검증은 @RequireInstructorOwnership AOP가 자동 처리

        // 5. 중복 체크: 이미 맛보기 비디오가 존재하는지 확인 (1:1 관계)
        if (previewVideoRepository.existsByLectureId(lectureId)) {
            throw new IllegalStateException("해당 강의에 이미 맛보기 비디오가 존재합니다. Lecture ID: " + lectureId);
        }

        // 6. 비디오 파일 S3에 업로드
        String videoUrl = s3StorageService.uploadFile(
                videoFile,
                "preview-videos/lecture-" + lectureId
        );
        log.info("맛보기 비디오 S3 업로드 완료 - Lecture ID: {}, URL: {}", lectureId, videoUrl);

        // 7. 썸네일 업로드 (선택사항)
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = s3StorageService.uploadFile(
                    thumbnailFile,
                    "thumbnails/preview-videos/lecture-" + lectureId
            );
            log.info("썸네일 S3 업로드 완료 - Lecture ID: {}, URL: {}", lectureId, thumbnailUrl);
        }

        // 8. PreviewVideo 엔티티 생성 및 DB 저장
        PreviewVideo previewVideo = PreviewVideo.builder()
                .lecture(lecture)
                .name(videoFile.getOriginalFilename())
                .originalUrl(videoUrl)
                .url1080p(null)
                .url720p(null)
                .url540p(null)
                .thumbnailUrl(thumbnailUrl)
                .fileSize(videoFile.getSize())
                .build();

        PreviewVideo savedPreviewVideo = previewVideoRepository.save(previewVideo);
        log.info("PreviewVideo 엔티티 DB 저장 완료 - PreviewVideo ID: {}, encodingStatus: {}",
                savedPreviewVideo.getId(), savedPreviewVideo.getEncodingStatus());

        // 9. 맛보기 비디오 업로드 완료 이벤트 발행
        log.info("📢 PreviewVideoUploadedEvent 발행 - PreviewVideo ID: {}", 
                savedPreviewVideo.getId());
        eventPublisher.publishEvent(new PreviewVideoUploadedEvent(savedPreviewVideo.getId()));

        // 10. DTO 변환하여 반환
        return PreviewVideoUploadResponse.from(savedPreviewVideo);
    }

    /**
     * 강의별 맛보기 비디오 조회 (1:1 관계)
     * - QueryDSL Fetch Join으로 N+1 문제 방지
     * @param lectureId 강의 ID
     * @return 맛보기 비디오 응답 DTO
     */
    public PreviewVideoResponse getPreviewVideoByLecture(Long lectureId) {
        log.info("강의별 맛보기 비디오 조회 (Fetch Join) - Lecture ID: {}", lectureId);

        // Fetch Join으로 Lecture와 Instructor를 함께 조회 (N+1 문제 방지)
        PreviewVideo previewVideo = previewVideoRepository.findByLectureIdWithLectureAndInstructor(lectureId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "맛보기 비디오를 찾을 수 없습니다. Lecture ID: " + lectureId));

        return PreviewVideoResponse.from(previewVideo);
    }

    /**
     * 맛보기 비디오 상세 조회
     * - QueryDSL Fetch Join으로 N+1 문제 방지
     * @param previewVideoId 맛보기 비디오 ID
     * @return 맛보기 비디오 상세 응답 DTO
     */
    public PreviewVideoResponse getPreviewVideo(Long previewVideoId) {
        log.info("맛보기 비디오 상세 조회 (Fetch Join) - PreviewVideo ID: {}", previewVideoId);

        // Fetch Join으로 Lecture와 Instructor를 함께 조회 (N+1 문제 방지)
        PreviewVideo previewVideo = previewVideoRepository.findByIdWithLectureAndInstructor(previewVideoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "맛보기 비디오를 찾을 수 없습니다. ID: " + previewVideoId));

        return PreviewVideoResponse.from(previewVideo);
    }

    /**
     * 맛보기 비디오 스트리밍 URL 조회
     * - 단순 조회이므로 Fetch Join 불필요 (성능 최적화)
     * @param previewVideoId 맛보기 비디오 ID
     * @return 스트리밍 URL 응답 DTO
     */
    public PreviewVideoStreamResponse getStreamUrl(Long previewVideoId) {
        log.info("맛보기 비디오 스트리밍 URL 요청 - PreviewVideo ID: {}", previewVideoId);

        // 비디오 조회 (Lecture나 Instructor 정보 불필요하므로 기본 조회 메서드 사용)
        PreviewVideo previewVideo = previewVideoRepository.findById(previewVideoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "맛보기 비디오를 찾을 수 없습니다. ID: " + previewVideoId));

        // 인코딩 완료 확인 (이전 요구사항: COMPLETED 상태만 허용)
        // 현재는 원본 URL로도 재생 가능하도록 변경됨
        // if (!previewVideo.isEncodingCompleted()) {
        //     throw new IllegalStateException("비디오 인코딩이 완료되지 않았습니다. PreviewVideo ID: " + previewVideoId);
        // }

        // 사용 가능한 최고 해상도 URL 선택 (VideoResource 인터페이스 활용)
        String videoUrl = previewVideo.getAvailableVideoUrl();

        // S3 Presigned URL 생성 (1시간 유효)
        String streamingUrl = s3StorageService.generatePresignedUrl(videoUrl, 60);
        log.info("맛보기 비디오 스트리밍 URL 생성 완료 - PreviewVideo ID: {}", previewVideoId);

        // DTO 생성 및 반환
        return PreviewVideoStreamResponse.of(previewVideoId, streamingUrl, 60);
    }

    /**
     * 맛보기 비디오 기본 해상도 변경
     * @param previewVideoId 맛보기 비디오 ID
     * @param resolution 변경할 해상도
     * @param currentUser 현재 로그인한 사용자
     */
    @Transactional
    public void updateDefaultResolution(Long previewVideoId, String resolution, User currentUser) {
        log.info("맛보기 비디오 기본 해상도 변경 - ID: {}, Resolution: {}", previewVideoId, resolution);

        if (!isValidResolution(resolution)) {
            throw new com.studyblock.domain.course.exception.InvalidResolutionException(resolution);
        }

        PreviewVideo previewVideo = verifyPreviewVideoOwnership(previewVideoId, currentUser);
        previewVideo.updateDefaultResolution(resolution);
        previewVideoRepository.save(previewVideo);
    }

    /**
     * 맛보기 비디오 삭제 (S3 파일 삭제 + DB 삭제)
     * @param previewVideoId 맛보기 비디오 ID
     * @param currentUser 현재 로그인한 사용자
     */
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public void deletePreviewVideo(Long previewVideoId, User currentUser) {
        log.info("=== 맛보기 비디오 삭제 시작 ===");
        log.info("PreviewVideo ID: {}, User ID: {}", previewVideoId, 
                currentUser != null ? currentUser.getId() : "null");

        // 인증 확인
        if (currentUser == null) {
            log.error("인증 실패 - currentUser가 null입니다");
            throw new IllegalArgumentException("로그인이 필요합니다. 토큰이 없거나 만료되었습니다.");
        }

        // 비디오 조회 및 권한 검증
        log.info("비디오 조회 및 권한 검증 시작 - PreviewVideo ID: {}", previewVideoId);
        PreviewVideo previewVideo = verifyPreviewVideoOwnership(previewVideoId, currentUser);
        Lecture lecture = previewVideo.getLecture();
        log.info("비디오 조회 및 권한 검증 완료 - PreviewVideo ID: {}, Lecture ID: {}", 
                previewVideoId, lecture.getId());

        // S3에서 모든 해상도 비디오 파일 삭제
        log.info("S3 파일 삭제 시작 - PreviewVideo ID: {}", previewVideoId);
        deleteS3Files(previewVideo);
        log.info("S3 파일 삭제 완료 - PreviewVideo ID: {}", previewVideoId);

        // DB에서 삭제
        // 양방향 관계를 끊어야 orphanRemoval이 정상 작동함
        // Lecture.previewVideo를 null로 설정하면 orphanRemoval=true가 자동으로 삭제 처리
        log.info("=== DB 삭제 시작 ===");
        log.info("PreviewVideo ID: {}, Lecture ID: {}", previewVideoId, lecture.getId());
        
        // 양방향 관계 해제: Lecture에서 previewVideo 참조 제거
        // 이렇게 하면 orphanRemoval이 자동으로 PreviewVideo를 삭제함
        log.info("Lecture의 previewVideo 참조 제거 시작");
        lecture.removePreviewVideo();
        lectureRepository.save(lecture);
        log.info("Lecture의 previewVideo 참조 제거 완료 및 저장 완료");
        
        // flush를 강제로 실행하여 즉시 DB에 반영
        lectureRepository.flush();
        log.info("Lecture flush() 완료");
        
        // 삭제 후 존재 여부 확인
        boolean 삭제_후_존재 = previewVideoRepository.existsById(previewVideoId);
        log.info("삭제 후 PreviewVideo 존재 여부 확인 - PreviewVideo ID: {}, 존재: {}", 
                previewVideoId, 삭제_후_존재);
        
        if (삭제_후_존재) {
            log.error("❌ PreviewVideo 삭제 후에도 여전히 존재합니다 - PreviewVideo ID: {}", previewVideoId);
            throw new RuntimeException("PreviewVideo DB 삭제가 실패했습니다 - PreviewVideo ID: " + previewVideoId);
        }
        
        log.info("✅ PreviewVideo 엔티티 DB 삭제 완료 - PreviewVideo ID: {}", previewVideoId);
        log.info("=== 맛보기 비디오 삭제 완료 ===");
    }

    /**
     * 맛보기 비디오 인코딩 상태 업데이트 (내부용/관리자용)
     * @param previewVideoId 맛보기 비디오 ID
     * @param status 변경할 인코딩 상태
     * @param currentUser 현재 로그인한 사용자
     * @return 업데이트된 맛보기 비디오 응답 DTO
     */
    @Transactional
    public PreviewVideoResponse updateEncodingStatus(Long previewVideoId, EncodingStatus status, User currentUser) {
        log.info("맛보기 비디오 인코딩 상태 업데이트 - PreviewVideo ID: {}, Status: {}", previewVideoId, status);

        // 비디오 조회 및 권한 검증
        PreviewVideo previewVideo = verifyPreviewVideoOwnership(previewVideoId, currentUser);

        // 상태 업데이트
        switch (status) {
            case PROCESSING -> previewVideo.startEncoding();
            case COMPLETED -> previewVideo.completeEncoding();
            case FAILED -> previewVideo.failEncoding();
            case PENDING -> log.warn("PENDING 상태로 되돌리기는 권장되지 않습니다. PreviewVideo ID: {}", previewVideoId);
        }

        // 변경사항 저장
        previewVideoRepository.save(previewVideo);

        // DTO 변환 및 반환
        return PreviewVideoResponse.fromSimple(previewVideo);
    }

    /**
     * S3 파일 삭제 (원본, 인코딩, 썸네일, 자막)
     * 개별 파일 삭제 실패가 전체 삭제를 막지 않도록 각 파일별로 예외 처리
     * 
     * @param previewVideo 맛보기 비디오 엔티티
     */
    private void deleteS3Files(PreviewVideo previewVideo) {
        int successCount = 0;
        int failureCount = 0;

        // 원본 비디오 삭제
        if (previewVideo.getOriginalUrl() != null) {
            try {
                s3StorageService.deleteFile(previewVideo.getOriginalUrl());
                log.info("S3 원본 비디오 파일 삭제 완료 - URL: {}", previewVideo.getOriginalUrl());
                successCount++;
            } catch (Exception e) {
                log.error("S3 원본 비디오 파일 삭제 실패 - URL: {}", previewVideo.getOriginalUrl(), e);
                failureCount++;
            }
        }

        // 1080p 해상도 삭제
        if (previewVideo.getUrl1080p() != null) {
            try {
                s3StorageService.deleteFile(previewVideo.getUrl1080p());
                log.info("S3 1080p 비디오 파일 삭제 완료 - URL: {}", previewVideo.getUrl1080p());
                successCount++;
            } catch (Exception e) {
                log.error("S3 1080p 비디오 파일 삭제 실패 - URL: {}", previewVideo.getUrl1080p(), e);
                failureCount++;
            }
        }

        // 720p 해상도 삭제
        if (previewVideo.getUrl720p() != null) {
            try {
                s3StorageService.deleteFile(previewVideo.getUrl720p());
                log.info("S3 720p 비디오 파일 삭제 완료 - URL: {}", previewVideo.getUrl720p());
                successCount++;
            } catch (Exception e) {
                log.error("S3 720p 비디오 파일 삭제 실패 - URL: {}", previewVideo.getUrl720p(), e);
                failureCount++;
            }
        }

        // 540p 해상도 삭제
        if (previewVideo.getUrl540p() != null) {
            try {
                s3StorageService.deleteFile(previewVideo.getUrl540p());
                log.info("S3 540p 비디오 파일 삭제 완료 - URL: {}", previewVideo.getUrl540p());
                successCount++;
            } catch (Exception e) {
                log.error("S3 540p 비디오 파일 삭제 실패 - URL: {}", previewVideo.getUrl540p(), e);
                failureCount++;
            }
        }

        // 썸네일 삭제
        if (previewVideo.getThumbnailUrl() != null) {
            try {
                s3StorageService.deleteFile(previewVideo.getThumbnailUrl());
                log.info("S3 썸네일 파일 삭제 완료 - URL: {}", previewVideo.getThumbnailUrl());
                successCount++;
            } catch (Exception e) {
                log.error("S3 썸네일 파일 삭제 실패 - URL: {}", previewVideo.getThumbnailUrl(), e);
                failureCount++;
            }
        }

        // 자막 삭제
        if (previewVideo.getSubtitleUrl() != null) {
            try {
                s3StorageService.deleteFile(previewVideo.getSubtitleUrl());
                log.info("S3 자막 파일 삭제 완료 - URL: {}", previewVideo.getSubtitleUrl());
                successCount++;
            } catch (Exception e) {
                log.error("S3 자막 파일 삭제 실패 - URL: {}", previewVideo.getSubtitleUrl(), e);
                failureCount++;
            }
        }

        // 삭제 결과 로그
        log.info("S3 파일 삭제 완료 - PreviewVideo ID: {}, 성공: {}, 실패: {}", 
                previewVideo.getId(), successCount, failureCount);
        
        // 모든 파일 삭제가 실패한 경우에만 예외 발생
        if (failureCount > 0 && successCount == 0) {
            log.warn("모든 S3 파일 삭제가 실패했습니다. - PreviewVideo ID: {}", previewVideo.getId());
            // 주의: 예외를 던지지 않고 로그만 남김 (DB 삭제는 계속 진행)
        }
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
     * PreviewVideo ID로 강의 소유자 권한 검증
     * - QueryDSL Fetch Join으로 PreviewVideo + Lecture + Instructor를 한 번에 조회
     * - N+1 문제 해결 (5개 쿼리 → 1개 쿼리)
     * - @RequireInstructorOwnership는 lectureId 파라미터가 필요하므로, 
     *   previewVideoId만 있는 경우 수동 권한 검증 필요
     * 
     * @param previewVideoId 맛보기 비디오 ID
     * @param currentUser 현재 로그인한 사용자
     * @return PreviewVideo 엔티티 (Lecture, Instructor 포함)
     */
    private PreviewVideo verifyPreviewVideoOwnership(Long previewVideoId, User currentUser) {
        log.info("PreviewVideo 조회 시작 (Fetch Join) - PreviewVideo ID: {}", previewVideoId);
        
        // QueryDSL Fetch Join으로 PreviewVideo + Lecture + Instructor를 한 번에 조회
        PreviewVideo previewVideo = previewVideoRepository.findByIdWithLectureAndInstructor(previewVideoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "맛보기 비디오를 찾을 수 없습니다. ID: " + previewVideoId));
        
        log.info("PreviewVideo 조회 완료 (Fetch Join) - PreviewVideo ID: {}, Lecture ID: {}", 
                previewVideoId, previewVideo.getLecture().getId());

        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 맛보기 비디오를 관리할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();
        
        // Fetch Join으로 이미 로드된 Lecture와 Instructor 사용
        // 별도의 쿼리 없이 메모리에서 직접 접근 (N+1 문제 해결)
        Lecture lecture = previewVideo.getLecture();
        Long lectureInstructorId = lecture.getInstructor().getId();
        Long currentUserInstructorId = instructorProfile.getId();
        
        log.info("강사 권한 검증 시작 - Lecture Instructor ID: {}, Current User Instructor ID: {}", 
                lectureInstructorId, currentUserInstructorId);
        
        // 강의의 강사와 현재 사용자의 강사 프로필이 일치하지 않으면 권한 없음
        if (!lectureInstructorId.equals(currentUserInstructorId)) {
            throw new IllegalArgumentException("해당 강의의 소유자만 맛보기 비디오를 관리할 수 있습니다.");
        }
        
        log.info("권한 검증 완료 - PreviewVideo ID: {}", previewVideoId);
        return previewVideo;
    }
}

