package com.studyblock.domain.course.service;

import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.entity.VideoResource;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.repository.PreviewVideoRepository;
import com.studyblock.domain.course.repository.VideoRepository;
import com.studyblock.infrastructure.encoding.FFmpegService;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 제네릭 비디오 인코딩 워크플로우 관리 서비스
 * - VideoResource 인터페이스를 구현한 모든 엔티티(Video, PreviewVideo) 지원
 * - S3 다운로드 → FFmpeg 인코딩 → S3 업로드 → DB 업데이트
 * - 비동기 처리 지원
 * 
 * AOP 관점: 인코딩 로직을 Video/PreviewVideo로부터 분리하여 재사용성 극대화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenericVideoEncodingService {

    private final FFmpegService ffmpegService;
    private final S3StorageService s3StorageService;
    private final VideoRepository videoRepository;
    private final PreviewVideoRepository previewVideoRepository;
    private final VideoEncodingStatusService encodingStatusService;

    @Value("${video.encoding.workspace:./tmp/videos}")
    private String workspacePath;

    /**
     * 비디오 인코딩 시작 (제네릭)
     * - VideoResource 인터페이스를 구현한 모든 엔티티 처리 가능
     * - S3에서 원본 다운로드
     * - FFmpeg로 다중 해상도 인코딩
     * - 인코딩된 파일들 S3 업로드
     * - VideoResource 엔티티 URL 업데이트
     *
     * @param videoResource 인코딩할 비디오 리소스 (Video 또는 PreviewVideo)
     * @param lectureId 강의 ID (S3 경로 구성용)
     * @param videoType 비디오 타입 ("video" 또는 "preview-video")
     */
    @Transactional
    public void performEncoding(VideoResource videoResource, Long lectureId, String videoType) throws IOException {
        log.info("비디오 인코딩 시작 - Type: {}, ID: {}, Lecture ID: {}",
                videoType, videoResource.getId(), lectureId);

        File localFile = null;
        Map<String, File> encodedFiles = new HashMap<>();

        try {
            // 1. 인코딩 상태를 PROCESSING으로 변경 및 DB 저장
            videoResource.startEncoding();
            videoResource.updateEncodingProgress(0); // ✅ 진행률 0%로 초기화
            saveVideoResource(videoResource, videoType);
            log.info("인코딩 상태 PROCESSING으로 변경 및 DB 저장 완료 - Type: {}, ID: {}", videoType, videoResource.getId());

            // SSE 알림: PROCESSING, progress: 0 (시작)
            encodingStatusService.notifyStatusChange(videoResource.getId(), videoType, EncodingStatus.PROCESSING, 0);

            // 2. S3에서 원본 비디오 다운로드
            log.info("S3에서 원본 비디오 다운로드 시작 - URL: {}", videoResource.getOriginalUrl());
            localFile = downloadFromS3(videoResource.getOriginalUrl(), videoResource.getId());
            log.info("원본 다운로드 완료 - 파일 크기: {} bytes", localFile.length());
            videoResource.updateEncodingProgress(10); // ✅ DB 저장
            saveVideoResource(videoResource, videoType);
            encodingStatusService.notifyStatusChange(videoResource.getId(), videoType, EncodingStatus.PROCESSING, 10);

            // 3. FFmpeg로 다중 해상도 인코딩 (720p만 인코딩됨)
            log.info("FFmpeg 인코딩 시작 - 입력 파일: {}", localFile.getName());
            Map<String, String> encodedFileNames = ffmpegService.encodeToMultipleResolutions(localFile.getName());
            
            // 인코딩된 파일들을 File 객체로 변환 (null 체크)
            String fileName1080p = encodedFileNames.get("1080p");
            String fileName720p = encodedFileNames.get("720p");
            String fileName540p = encodedFileNames.get("540p");

            if (fileName1080p != null) {
                encodedFiles.put("1080p", new File(workspacePath, fileName1080p));
            }
            if (fileName720p != null) {
                encodedFiles.put("720p", new File(workspacePath, fileName720p));
            }
            if (fileName540p != null) {
                encodedFiles.put("540p", new File(workspacePath, fileName540p));
            }
            
            videoResource.updateEncodingProgress(40); // ✅ DB 저장
            saveVideoResource(videoResource, videoType);
            encodingStatusService.notifyStatusChange(videoResource.getId(), videoType, EncodingStatus.PROCESSING, 40);

            // 4. 인코딩된 파일들을 S3에 업로드 (720p만 업로드)
            log.info("인코딩된 파일들 S3 업로드 시작");
            String url1080p = null;
            String url720p = null;
            String url540p = null;

            if (encodedFiles.get("720p") != null) {
                url720p = uploadEncodedFileToS3(encodedFiles.get("720p"), lectureId, "720p", videoType);
            } else {
                log.warn("720p 인코딩 파일이 없습니다. S3 업로드 건너뜀");
            }

            if (encodedFiles.get("1080p") != null) {
                url1080p = uploadEncodedFileToS3(encodedFiles.get("1080p"), lectureId, "1080p", videoType);
            } else {
                log.info("1080p 인코딩 파일이 없습니다. S3 업로드 건너뜀");
            }

            if (encodedFiles.get("540p") != null) {
                url540p = uploadEncodedFileToS3(encodedFiles.get("540p"), lectureId, "540p", videoType);
            } else {
                log.info("540p 인코딩 파일이 없습니다. S3 업로드 건너뜀");
            }
            
            log.info("S3 업로드 완료 - 1080p: {}, 720p: {}, 540p: {}", url1080p, url720p, url540p);

            // 5. VideoResource 엔티티 URL 업데이트 및 인코딩 완료 상태로 변경
            videoResource.updateVideoUrls(url1080p, url720p, url540p);
            videoResource.completeEncoding();
            videoResource.updateEncodingProgress(100); // ✅ 최종 100% 저장
            saveVideoResource(videoResource, videoType);
            log.info("인코딩 완료 및 DB 저장 완료 - Type: {}, ID: {}", videoType, videoResource.getId());

            // SSE 알림: COMPLETED, progress: 100
            encodingStatusService.notifyStatusChange(videoResource.getId(), videoType, EncodingStatus.COMPLETED, 100);

        } catch (Exception e) {
            log.error("인코딩 실패 - Type: {}, ID: {}", videoType, videoResource.getId(), e);
            videoResource.failEncoding();
            saveVideoResource(videoResource, videoType);

            // SSE 알림: FAILED 상태
            encodingStatusService.notifyStatusChange(videoResource.getId(), videoType, EncodingStatus.FAILED);

            throw new RuntimeException("비디오 인코딩 실패", e);

        } finally {
            // 6. 임시 파일 정리
            cleanupTempFiles(localFile);
            cleanupTempFiles(encodedFiles.values().toArray(new File[0]));
        }
    }

    /**
     * S3에서 파일 다운로드
     *
     * @param s3Url S3 URL
     * @param videoId 비디오 ID
     * @return 다운로드된 로컬 파일
     * @throws IOException 다운로드 실패 시
     */
    private File downloadFromS3(String s3Url, Long videoId) throws IOException {
        String fileName = String.format("video_%d_original.mp4", videoId);
        String localPath = workspacePath + "/" + fileName;
        return s3StorageService.downloadFile(s3Url, localPath);
    }

    /**
     * 인코딩된 파일을 S3에 업로드
     *
     * @param file 업로드할 파일
     * @param lectureId 강의 ID
     * @param resolution 해상도 (현재는 720p만 인코딩, 1080p, 540p는 미지원)
     * @param videoType 비디오 타입 ("videos" 또는 "preview-videos")
     * @return S3 URL
     * @throws IOException 업로드 실패 시
     */
    private String uploadEncodedFileToS3(File file, Long lectureId, String resolution, String videoType) 
            throws IOException {
        if (!file.exists()) {
            throw new IOException("업로드할 파일이 존재하지 않습니다: " + file.getAbsolutePath());
        }

        log.info("S3 업로드 시작 - 파일: {}, 크기: {} MB", file.getName(), file.length() / (1024 * 1024));

        String folderPath = String.format("%s/lecture-%d/encoded/%s", videoType, lectureId, resolution);
        String s3Url = s3StorageService.uploadFile(file, folderPath);

        log.info("S3 업로드 완료 - URL: {}", s3Url);
        return s3Url;
    }

    /**
     * 임시 파일 정리
     *
     * @param files 삭제할 파일들
     */
    private void cleanupTempFiles(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.debug("임시 파일 삭제 완료: {}", file.getName());
                } else {
                    log.warn("임시 파일 삭제 실패: {}", file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * VideoResource 엔티티를 적절한 Repository로 저장
     * - Video 타입이면 VideoRepository 사용
     * - PreviewVideo 타입이면 PreviewVideoRepository 사용
     *
     * @param videoResource 저장할 비디오 리소스
     * @param videoType 비디오 타입 ("video" 또는 "preview-video")
     */
    private void saveVideoResource(VideoResource videoResource, String videoType) {
        if (videoResource instanceof Video) {
            videoRepository.save((Video) videoResource);
            log.debug("Video 엔티티 저장 완료 - ID: {}", videoResource.getId());
        } else if (videoResource instanceof PreviewVideo) {
            previewVideoRepository.save((PreviewVideo) videoResource);
            log.debug("PreviewVideo 엔티티 저장 완료 - ID: {}", videoResource.getId());
        } else {
            log.warn("알 수 없는 VideoResource 타입 - Type: {}, ID: {}", videoType, videoResource.getId());
        }
    }

    /**
     * FFmpeg 상태 확인
     *
     * @return FFmpeg 버전 정보
     */
    public String checkFFmpegStatus() {
        try {
            return ffmpegService.checkFFmpegVersion();
        } catch (IOException e) {
            log.error("FFmpeg 상태 확인 실패", e);
            return "FFmpeg 상태 확인 실패: " + e.getMessage();
        }
    }
}

