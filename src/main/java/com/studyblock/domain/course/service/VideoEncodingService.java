package com.studyblock.domain.course.service;

import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.enums.EncodingStatus;
import com.studyblock.domain.course.repository.VideoRepository;
import com.studyblock.infrastructure.encoding.FFmpegService;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 비디오 인코딩 워크플로우 관리 서비스
 * - S3 다운로드 → FFmpeg 인코딩 → S3 업로드 → DB 업데이트
 * - 비동기 처리 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEncodingService {

    private final VideoRepository videoRepository;
    private final FFmpegService ffmpegService;
    private final S3StorageService s3StorageService;
    private final VideoEncodingStatusService encodingStatusService;

    @Value("${video.encoding.workspace:./tmp/videos}")
    private String workspacePath;

    /**
     * 비디오 인코딩 시작 (비동기)
     * - S3에서 원본 다운로드
     * - FFmpeg로 다중 해상도 인코딩
     * - 인코딩된 파일들 S3 업로드
     * - Video 엔티티 URL 업데이트
     *
     * @param videoId 인코딩할 비디오 ID
     */
    @Async
    @Transactional
    public void startEncodingAsync(Long videoId) {
        log.info("비동기 인코딩 시작 - Video ID: {}", videoId);

        try {
            // 1. Video 엔티티 조회
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

            // 2. 인코딩 상태를 PROCESSING으로 변경
            video.startEncoding();
            videoRepository.save(video);
            log.info("인코딩 상태 PROCESSING으로 변경 - Video ID: {}", videoId);
            
            // SSE 알림: PROCESSING 상태
            encodingStatusService.notifyStatusChange(videoId, "video", EncodingStatus.PROCESSING);

            // 3. 실제 인코딩 수행
            performEncoding(video);

            // 4. 인코딩 완료 상태로 변경
            video.completeEncoding();
            videoRepository.save(video);
            log.info("인코딩 완료 - Video ID: {}", videoId);
            
            // SSE 알림: COMPLETED 상태
            encodingStatusService.notifyStatusChange(videoId, "video", EncodingStatus.COMPLETED);

        } catch (Exception e) {
            log.error("인코딩 실패 - Video ID: {}", videoId, e);

            // 실패 상태로 변경
            videoRepository.findById(videoId).ifPresent(video -> {
                video.failEncoding();
                videoRepository.save(video);
                
                // SSE 알림: FAILED 상태
                encodingStatusService.notifyStatusChange(videoId, "video", EncodingStatus.FAILED);
            });

            throw new RuntimeException("비디오 인코딩 실패", e);
        }
    }

    /**
     * 실제 인코딩 수행 (동기)
     *
     * @param video Video 엔티티
     * @throws IOException 인코딩 실패 시
     */
    private void performEncoding(Video video) throws IOException {
        File localFile = null;
        File encodedFile1080p = null;
        File encodedFile720p = null;
        File encodedFile540p = null;

        try {
            // 1. S3에서 원본 비디오 다운로드
            log.info("S3에서 원본 비디오 다운로드 시작 - URL: {}", video.getOriginalUrl());
            localFile = downloadFromS3(video.getOriginalUrl(), video.getId());
            log.info("원본 다운로드 완료 - 파일 크기: {} bytes", localFile.length());

            // 2. FFmpeg로 다중 해상도 인코딩
            log.info("FFmpeg 인코딩 시작 - 입력 파일: {}", localFile.getName());
            Map<String, String> encodedFileNames = ffmpegService.encodeToMultipleResolutions(localFile.getName());

            // 3. 인코딩된 파일들을 File 객체로 변환 (720p만 인코딩됨)
            String fileName1080p = encodedFileNames.get("1080p");
            String fileName720p = encodedFileNames.get("720p");
            String fileName540p = encodedFileNames.get("540p");

            if (fileName1080p != null) {
                encodedFile1080p = new File(workspacePath, fileName1080p);
            }
            if (fileName720p != null) {
                encodedFile720p = new File(workspacePath, fileName720p);
            }
            if (fileName540p != null) {
                encodedFile540p = new File(workspacePath, fileName540p);
            }

            // 4. 인코딩된 파일들을 S3에 업로드 (720p만 업로드)
            log.info("인코딩된 파일들 S3 업로드 시작");
            String url1080p = null;
            String url720p = null;
            String url540p = null;

            if (encodedFile720p != null) {
                url720p = uploadEncodedFileToS3(encodedFile720p, video.getLecture().getId(), "720p");
            } else {
                log.warn("720p 인코딩 파일이 없습니다. S3 업로드 건너뜀");
            }

            if (encodedFile1080p != null) {
                url1080p = uploadEncodedFileToS3(encodedFile1080p, video.getLecture().getId(), "1080p");
            } else {
                log.info("1080p 인코딩 파일이 없습니다. S3 업로드 건너뜀");
            }

            if (encodedFile540p != null) {
                url540p = uploadEncodedFileToS3(encodedFile540p, video.getLecture().getId(), "540p");
            } else {
                log.info("540p 인코딩 파일이 없습니다. S3 업로드 건너뜀");
            }

            log.info("S3 업로드 완료 - 1080p: {}, 720p: {}, 540p: {}", url1080p, url720p, url540p);

            // 5. Video 엔티티 URL 업데이트
            video.updateVideoUrls(url1080p, url720p, url540p);
            videoRepository.save(video);
            log.info("Video 엔티티 URL 업데이트 완료 - Video ID: {}", video.getId());

        } finally {
            // 6. 임시 파일 정리
            cleanupTempFiles(localFile, encodedFile1080p, encodedFile720p, encodedFile540p);
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
        // 파일명 생성 (video_{videoId}_original.mp4)
        String fileName = String.format("video_%d_original.mp4", videoId);
        String localPath = workspacePath + "/" + fileName;

        // S3StorageService를 통한 직접 다운로드
        return s3StorageService.downloadFile(s3Url, localPath);
    }

    /**
     * 인코딩된 파일을 S3에 업로드
     *
     * @param file 업로드할 파일
     * @param lectureId 강의 ID
     * @param resolution 해상도 (현재는 720p만 인코딩, 1080p, 540p는 미지원)
     * @return S3 URL
     * @throws IOException 업로드 실패 시
     */
    private String uploadEncodedFileToS3(File file, Long lectureId, String resolution) throws IOException {
        if (!file.exists()) {
            throw new IOException("업로드할 파일이 존재하지 않습니다: " + file.getAbsolutePath());
        }

        log.info("S3 업로드 시작 - 파일: {}, 크기: {} MB", file.getName(), file.length() / (1024 * 1024));

        String folderPath = String.format("videos/lecture-%d/encoded/%s", lectureId, resolution);

        // S3StorageService의 File 업로드 메서드 사용
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