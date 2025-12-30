package com.studyblock.domain.upload.service;

import com.studyblock.domain.upload.dto.ImageUploadResponse;
import com.studyblock.domain.upload.enums.ImageType;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 이미지 업로드 서비스
 * - S3StorageService를 활용한 이미지 업로드
 * - 이미지 검증, 리사이징, 썸네일 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final S3StorageService s3StorageService;

    // 허용되는 이미지 MIME 타입
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 허용되는 파일 확장자
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 기본 이미지 최대 크기
    private static final int MAX_IMAGE_WIDTH = 1200;
    private static final int MAX_IMAGE_HEIGHT = 1200;

    // 썸네일 크기
    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 200;

    /**
     * 이미지 업로드 (썸네일 자동 생성)
     * @param file 업로드할 이미지 파일
     * @param imageType 이미지 타입
     * @return 업로드 결과
     */
    public ImageUploadResponse uploadImage(MultipartFile file, ImageType imageType) {
        try {
            // 1. 파일 검증
            validateImageFile(file);

            // 2. 파일 기본 검증
            if (file.isEmpty() || file.getSize() == 0) {
                log.warn("빈 파일 업로드 시도: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("빈 파일입니다.");
            }

            // 3. 바이트 배열로 안전하게 읽기
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                log.warn("파일 크기가 0: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("파일이 비어있습니다.");
            }

            // 4. 이미지 읽기 시도 (WebP 우선 처리)
            BufferedImage originalImage = readImageWithWebPSupport(fileBytes, file.getContentType());
            if (originalImage == null) {
                log.warn("이미지 파싱 실패: filename={}, contentType={}, size={}", 
                    file.getOriginalFilename(), file.getContentType(), fileBytes.length);
                throw new IllegalArgumentException("지원하지 않는 이미지 형식이거나 손상된 파일입니다.");
            }

            log.info("이미지 파싱 성공: {}x{}", originalImage.getWidth(), originalImage.getHeight());

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 3. 이미지 리사이징 (필요한 경우)
            BufferedImage resizedImage = resizeImageIfNeeded(originalImage);

            // 4. 썸네일 생성
            BufferedImage thumbnailImage = createThumbnail(originalImage);

            // 5. 날짜별 폴더 경로 생성
            LocalDate now = LocalDate.now();
            String folderPath = imageType.getS3PathWithDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth());

            // 6. 원본 이미지 업로드
            String originalUrl = uploadImageToS3(resizedImage, file.getOriginalFilename(), folderPath, "original");

            // 7. 썸네일 업로드
            String thumbnailUrl = uploadImageToS3(thumbnailImage, file.getOriginalFilename(), folderPath, "thumbnail");

            // 8. 응답 생성
            return ImageUploadResponse.builder()
                    .url(originalUrl)
                    .originalFilename(file.getOriginalFilename())
                    .filename(generateFileName(file.getOriginalFilename()))
                    .size(file.getSize())
                    .mimeType(file.getContentType())
                    .imageType(imageType.name())
                    .thumbnailUrl(thumbnailUrl)
                    .width(originalWidth)
                    .height(originalHeight)
                    .success(true)
                    .message("이미지가 성공적으로 업로드되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("이미지 업로드 실패: {}", e.getMessage(), e);
            return ImageUploadResponse.builder()
                    .success(false)
                    .message("이미지 업로드에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 썸네일만 업로드
     * @param file 업로드할 이미지 파일
     * @param imageType 이미지 타입
     * @return 업로드 결과
     */
    public ImageUploadResponse uploadThumbnailOnly(MultipartFile file, ImageType imageType) {
        try {
            // 1. 파일 검증
            validateImageFile(file);

            // 2. 파일 기본 검증
            if (file.isEmpty() || file.getSize() == 0) {
                log.warn("빈 파일 업로드 시도: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("빈 파일입니다.");
            }

            // 3. 바이트 배열로 안전하게 읽기
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                log.warn("파일 크기가 0: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("파일이 비어있습니다.");
            }

            // 4. 이미지 읽기 시도 (WebP 우선 처리)
            BufferedImage originalImage = readImageWithWebPSupport(fileBytes, file.getContentType());
            if (originalImage == null) {
                log.warn("이미지 파싱 실패: filename={}, contentType={}, size={}", 
                    file.getOriginalFilename(), file.getContentType(), fileBytes.length);
                throw new IllegalArgumentException("지원하지 않는 이미지 형식이거나 손상된 파일입니다.");
            }

            log.info("이미지 파싱 성공: {}x{}", originalImage.getWidth(), originalImage.getHeight());

            // 3. 썸네일 생성
            BufferedImage thumbnailImage = createThumbnail(originalImage);

            // 4. 날짜별 폴더 경로 생성
            LocalDate now = LocalDate.now();
            String folderPath = imageType.getS3PathWithDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth());

            // 5. 썸네일 업로드
            String thumbnailUrl = uploadImageToS3(thumbnailImage, file.getOriginalFilename(), folderPath, "thumbnail");

            // 6. 응답 생성
            return ImageUploadResponse.builder()
                    .url(thumbnailUrl)
                    .originalFilename(file.getOriginalFilename())
                    .filename(generateFileName(file.getOriginalFilename()))
                    .size(file.getSize())
                    .mimeType(file.getContentType())
                    .imageType(imageType.name())
                    .thumbnailUrl(thumbnailUrl)
                    .width(THUMBNAIL_WIDTH)
                    .height(THUMBNAIL_HEIGHT)
                    .success(true)
                    .message("썸네일이 성공적으로 업로드되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("썸네일 업로드 실패: {}", e.getMessage(), e);
            return ImageUploadResponse.builder()
                    .success(false)
                    .message("썸네일 업로드에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * WebP 지원을 포함한 이미지 읽기
     * @param fileBytes 파일 바이트 배열
     * @param contentType MIME 타입
     * @return BufferedImage 또는 null
     */
    private BufferedImage readImageWithWebPSupport(byte[] fileBytes, String contentType) {
        try {
            // WebP 파일인 경우 - ImageIO로 읽기 시도
            if ("image/webp".equals(contentType)) {
                log.info("WebP 파일 업로드 시도 - ImageIO로 읽기 시도");
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));

                // ImageIO가 WebP를 지원하지 않으면 null 반환
                if (image == null) {
                    log.warn("WebP 파일을 읽을 수 없습니다. Java ImageIO는 기본적으로 WebP를 지원하지 않습니다.");
                    log.warn("PNG, JPEG, GIF 형식의 이미지를 사용해주세요.");
                    return null;
                }

                log.info("WebP 파일 읽기 성공!");
                return image;
            }

            // 일반 이미지 파일 처리
            return ImageIO.read(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            log.warn("이미지 읽기 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 선택되지 않았습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB 이하여야 합니다.");
        }

        // MIME 타입 검증
        String contentType = file.getContentType();
        log.info("업로드 파일 정보 - 파일명: {}, ContentType: {}, 크기: {} bytes",
                file.getOriginalFilename(), contentType, file.getSize());

        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.error("지원하지 않는 MIME 타입: {} (허용: {})", contentType, ALLOWED_MIME_TYPES);
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 허용)");
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다.");
        }
    }

    /**
     * 이미지 리사이징 (필요한 경우)
     */
    private BufferedImage resizeImageIfNeeded(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 이미지가 최대 크기보다 작으면 리사이징하지 않음
        if (originalWidth <= MAX_IMAGE_WIDTH && originalHeight <= MAX_IMAGE_HEIGHT) {
            return originalImage;
        }

        // 비율을 유지하면서 리사이징
        double widthRatio = (double) MAX_IMAGE_WIDTH / originalWidth;
        double heightRatio = (double) MAX_IMAGE_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        log.info("이미지 리사이징 완료: {}x{} -> {}x{}", originalWidth, originalHeight, newWidth, newHeight);
        return resizedImage;
    }

    /**
     * 썸네일 생성
     */
    private BufferedImage createThumbnail(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 비율을 유지하면서 썸네일 크기로 리사이징
        double widthRatio = (double) THUMBNAIL_WIDTH / originalWidth;
        double heightRatio = (double) THUMBNAIL_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        BufferedImage thumbnail = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // 중앙에 이미지 배치
        int x = (THUMBNAIL_WIDTH - newWidth) / 2;
        int y = (THUMBNAIL_HEIGHT - newHeight) / 2;
        g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
        g2d.dispose();

        log.info("썸네일 생성 완료: {}x{}", THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        return thumbnail;
    }

    /**
     * S3에 이미지 업로드
     */
    private String uploadImageToS3(BufferedImage image, String originalFilename, String folderPath, String prefix) throws IOException {
        // BufferedImage를 byte 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String format = getImageFormat(originalFilename);
        ImageIO.write(image, format, baos);
        byte[] imageBytes = baos.toByteArray();

        // MultipartFile 형태로 변환하여 S3StorageService 사용
        MultipartFile multipartFile = new ByteArrayMultipartFile(
                generateFileName(originalFilename),
                originalFilename,
                getMimeType(format),
                imageBytes
        );

        return s3StorageService.uploadFile(multipartFile, folderPath);
    }

    /**
     * 파일명 생성 (UUID 포함)
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "img_" + uuid + extension;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 이미지 포맷 추출
     */
    private String getImageFormat(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        switch (extension) {
            case ".png": return "png";
            case ".gif": return "gif";
            case ".webp": return "webp";
            default: return "jpg";
        }
    }

    /**
     * MIME 타입 반환
     */
    private String getMimeType(String format) {
        switch (format.toLowerCase()) {
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            default: return "image/jpeg";
        }
    }

    /**
     * ByteArrayMultipartFile 구현체
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public java.io.InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
