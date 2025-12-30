package com.studyblock.domain.admin.service;

import com.studyblock.domain.admin.dto.NoticeResponse;
import com.studyblock.domain.admin.repository.AdminPostRepository;
import com.studyblock.domain.community.entity.Board;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.enums.BoardType;
import com.studyblock.domain.community.repository.BoardRepository;
import com.studyblock.domain.upload.dto.ImageUploadResponse;
import com.studyblock.domain.upload.enums.ImageType;
import com.studyblock.domain.upload.service.ImageUploadService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeService {

    private static final int NOTICE_IMAGE_URL_EXPIRATION_MINUTES = 30;

    private final AdminPostRepository adminPostRepository;
    private final BoardRepository boardRepository;
    private final ImageUploadService imageUploadService;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 공지사항 등록
    @Transactional
    public Post createPost(User user, String title, String originalContent, List<MultipartFile> files) {

        Board board = boardRepository.findByType(BoardType.NOTICE.getValue());
        String imageUrl = null;

        if (board == null) {
            throw new IllegalStateException("NOTICE 보드가 존재하지 않습니다");
        }

        // 중복 체크
        if (adminPostRepository.existsByTitleAndBoardId(title, board.getId())) {
            throw new IllegalArgumentException("같은 제목의 공지사항이 이미 존재합니다.");
        }

        // 여러 이미지 업로드
        List<String> imageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    ImageUploadResponse response = imageUploadService.uploadImage(file, ImageType.NOTICE);

                    if (response.getSuccess() && response.getUrl() != null) {
                        imageUrls.add(response.getUrl());
                        log.info("공지사항 이미지 업로드 성공: {}", response.getUrl());
                    } else {
                        log.warn("공지사항 이미지 업로드 실패: {}", response.getMessage());
                        throw new IllegalArgumentException("이미지 업로드에 실패했습니다: " + response.getMessage());
                    }
                }
            }
        }

        // URL 리스트 JSON 변환
        if (!imageUrls.isEmpty()) {
            try {
                imageUrl = objectMapper.writeValueAsString(imageUrls);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("이미지 URL JSON 변환 실패", e);
            }
        }

        Post post = new Post(board, user, title, originalContent, imageUrl);

        return adminPostRepository.save(post);
    }

    // 공지사항 리스트 조회
    public List<NoticeResponse> getNoticeList() {
        List<Post> noticeList = adminPostRepository.findByBoardType(BoardType.NOTICE.getValue());

        return noticeList.stream()
                .map(this::toNoticeResponse)
                .collect(Collectors.toList());
    }

    // 공지사항 수정
    @Transactional
    public Post editNotice(Long id, String editContent, List<MultipartFile> files, Boolean removeImage) {
        Post post = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        String imageUrl = post.getImageUrl(); // 기존 이미지 URL로 초기화 (파일이 없을 때 유지)

        // 이미지 제거 요청이 있으면 이미지 삭제
        if (removeImage != null && removeImage) {
            if (post.getImageUrl() != null) {
                // JSON 배열인 경우 파싱하여 각 이미지 삭제
                try {
                    List<String> existingUrls = objectMapper.readValue(post.getImageUrl(), new TypeReference<List<String>>() {});
                    for (String url : existingUrls) {
                        try {
                            s3StorageService.deleteFile(url);
                            log.info("공지사항 이미지 삭제 성공: {}", url);
                        } catch (Exception e) {
                            log.warn("공지사항 이미지 S3 삭제 실패: {}", url, e);
                        }
                    }
                } catch (Exception e) {
                    // JSON이 아니면 단일 URL로 처리
                    try {
                        s3StorageService.deleteFile(post.getImageUrl());
                        log.info("공지사항 이미지 삭제 성공: {}", post.getImageUrl());
                    } catch (Exception ex) {
                        log.warn("공지사항 이미지 S3 삭제 실패: {}", post.getImageUrl(), ex);
                    }
                }
            }
            imageUrl = null; // DB에서도 이미지 URL 제거
        } else if (files != null && !files.isEmpty()) {
            // 새 이미지 파일들이 있으면 업로드
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    ImageUploadResponse response = imageUploadService.uploadImage(file, ImageType.NOTICE);

                    if (response.getSuccess() && response.getUrl() != null) {
                        imageUrls.add(response.getUrl());
                        log.info("공지사항 이미지 업로드 성공: {}", response.getUrl());
                    } else {
                        log.warn("공지사항 이미지 업로드 실패: {}", response.getMessage());
                        throw new IllegalArgumentException("이미지 업로드에 실패했습니다: " + response.getMessage());
                    }
                }
            }

            // 기존 이미지가 있으면 S3에서 삭제
            if (post.getImageUrl() != null) {
                try {
                    List<String> existingUrls = objectMapper.readValue(post.getImageUrl(), new TypeReference<List<String>>() {});
                    for (String url : existingUrls) {
                        try {
                            s3StorageService.deleteFile(url);
                        } catch (Exception e) {
                            log.warn("기존 이미지 S3 삭제 실패: {}", url, e);
                        }
                    }
                } catch (Exception e) {
                    // JSON이 아니면 단일 URL로 처리
                    try {
                        s3StorageService.deleteFile(post.getImageUrl());
                    } catch (Exception ex) {
                        log.warn("기존 이미지 S3 삭제 실패: {}", post.getImageUrl(), ex);
                    }
                }
            }

            // URL 리스트 JSON 변환
            if (!imageUrls.isEmpty()) {
                try {
                    imageUrl = objectMapper.writeValueAsString(imageUrls);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("이미지 URL JSON 변환 실패", e);
                }
            }
        }
        // removeImage가 false이고 files도 없으면 기존 imageUrl 유지

        post.edit(editContent, imageUrl);

        return post;
    }

    // 공지사항 삭제(비활성화)
    @Transactional
    public void deleteNotice(Long id) {
        Post post = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));

        post.delete();
        adminPostRepository.save(post);
    }

    public NoticeResponse toNoticeResponse(Post post) {
        NoticeResponse response = NoticeResponse.from(post);
        String signedUrl = generateSignedUrl(response.getImageOriginalUrl());
        response.setImageUrl(signedUrl);
        return response;
    }

    private String generateSignedUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            return null;
        }
        
        // JSON 배열인지 확인 (다중 이미지)
        if (originalUrl.trim().startsWith("[")) {
            try {
                List<String> urls = objectMapper.readValue(originalUrl, new TypeReference<List<String>>() {});
                List<String> signedUrls = new ArrayList<>();
                
                for (String url : urls) {
                    if (url != null && !url.isBlank()) {
                        try {
                            String signedUrl = s3StorageService.generatePresignedUrl(url, NOTICE_IMAGE_URL_EXPIRATION_MINUTES);
                            signedUrls.add(signedUrl);
                        } catch (RuntimeException e) {
                            log.warn("공지사항 이미지 presigned URL 생성 실패 - url: {}", url, e);
                            // 개별 URL 생성 실패 시 원본 URL 사용
                            signedUrls.add(url);
                        }
                    }
                }
                
                // 다시 JSON 배열로 변환
                return objectMapper.writeValueAsString(signedUrls);
            } catch (Exception e) {
                log.warn("공지사항 이미지 URL JSON 파싱 실패 - url: {}", originalUrl, e);
                // 파싱 실패 시 단일 URL로 처리
            }
        }
        
        // 단일 URL 처리
        try {
            return s3StorageService.generatePresignedUrl(originalUrl, NOTICE_IMAGE_URL_EXPIRATION_MINUTES);
        } catch (RuntimeException e) {
            log.warn("공지사항 이미지 presigned URL 생성 실패 - url: {}", originalUrl, e);
            return null;
        }
    }
}
