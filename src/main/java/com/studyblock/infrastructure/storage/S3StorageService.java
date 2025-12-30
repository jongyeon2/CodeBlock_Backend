package com.studyblock.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * AWS S3 비즈니스 로직을 수행하는 Service 클래스.
 *
 * <p>버킷 운영 정책</p>
 * <ul>
 *   <li>Object Ownership: <strong>Bucket owner enforced</strong> (ACL 사용 불가)</li>
 *   <li>Block public access: <strong>ON</strong></li>
 *   <li>공개 URL 제공은 <strong>presigned URL</strong>을 통해서만 수행</li>
 * </ul>
 */

@Service
@Slf4j
public class S3StorageService {

    // AWS SDK의 S3Client — 파일 업로드/삭제 등 실제 요청을 수행하는 핵심 객체.
    private final S3Client s3Client;

    // application.yml에 정의된 버킷 이름(study-block)을 주입받음
    @Value("${spring.cloud.aws.s3.bucket}")
    private String studyBlock;

    // AWS Region 설정 (Presigned URL 생성 시 필요)
    @Value("${spring.cloud.aws.region.static}")
    private String region;

    // AWS Credentials (로컬 개발 환경에서만 사용, 배포 환경은 IAM Role 사용)
    // required = false: application-prod.yml에는 credentials 설정이 없으므로 선택적
    @Value("${spring.cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:}")
    private String secretKey;

    // 생성자 주입
    // AwsS3Config에서 만든 S3Client Bean을 스프링이 주입해줌.
    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /*
    - 파일 업로드
    @Param file 업로드할 파일
    @Param folderPath S3 내 폴더 경로 (EX: "video/lecture-1")
    @return S3에 저장된 파일 URL
     */
    public String uploadFile(MultipartFile file, String folderPath){

        // 업로드할 파일 이름을 고유하게 생성(20251010_23819_uuid.mp4)
        String fileName = generateFileName(file.getOriginalFilename());

        // key는 버킷 내부 경로를 의미(video/lecture-1/20251010_uuid.mp4)
        String key = folderPath + "/" + fileName;

        // S3에 올릴 객체의 메타정보를 설정
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(studyBlock) // S3 버킷 이름
                    .key(key) // S3 내 파일 경로
                    .contentType(file.getContentType()) //MIME 타입 (EX: image/png)
                    .build();

            // S3에 실제 파일 업로드 수행
            s3Client.putObject(putObjectRequest,
                    // RequestBody로 변환해서 바이트 단위로 업로드
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드 후 S3 URL 변환
            // - https://sutdy-block.s3.ap-northeast-2.amazonaws.com/video/...
            String fileUrl = getFileUrl(key);
            log.info("파일 업로드 완료 - key: {}", key);
            return fileUrl;

            // 예외 발생 시 로그 찍고 런타입 예외로 던져버림
        } catch (Exception e) {
            log.error("S3 업로드 실패 - key: {}", key, e);
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }

    /*
    File 객체를 S3에 업로드 (인코딩된 비디오용)
    @Param file 업로드할 File 객체
    @Param folderPath S3 내 폴더 경로
    @return S3에 저장된 파일 URL
     */
    public String uploadFile(File file, String folderPath) {
        // 파일명 생성
        String fileName = file.getName();
        String key = folderPath + "/" + fileName;

        try {
            // S3에 올릴 객체의 메타정보 설정
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(studyBlock)
                    .key(key)
                    .contentType("video/mp4")  // 비디오 파일 MIME 타입
                    .build();

            // S3에 파일 업로드
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromFile(file));

            // 업로드 후 S3 URL 반환
            String fileUrl = getFileUrl(key);
            log.info("파일 업로드 완료 - key: {}", key);
            return fileUrl;

        } catch (Exception e) {
            log.error("S3 업로드 실패 - key: {}", key, e);
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }

    /*
    파일 삭제
     */
    public void deleteFile(String fileUrl){
        try {
            // URL만 추출
            // https://...amazonaws.com/video/lecture-1/file.mp4)에서
            // key(video/lecture-1/file.mp4)만 추출
            String key = extractKeyFromUrl(fileUrl);

            // 삭제할 객체 지정 후 S3에 삭제 요청 전송
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(studyBlock)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);

            // 삭제 완료 로그 출력
            log.info("파일 삭제 완료 - key: {}", key);

            // 위와 마찬가지로 삭제 실패시 로그로 출력하고
            // 런타임 예외로 던져버림
        } catch (Exception e) {
            log.error("S3 삭제 실패 - key: {}", extractKeyFromUrl(fileUrl), e);
            throw new RuntimeException("S3 파일 삭제 실패", e);
        }
    }

    /*
    S3에서 파일 다운로드
    @Param s3Url S3 파일 URL
    @Param localPath 로컬 저장 경로 (파일명 포함)
    @return 다운로드된 로컬 파일
     */
    public File downloadFile(String s3Url, String localPath) throws IOException {
        try {
            log.info("localPath: {}",  localPath);
            // URL에서 key 추출
            String key = extractKeyFromUrl(s3Url);

            // S3에서 파일 다운로드 요청 객체 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(studyBlock)
                    .key(key)
                    .build();

            // 로컬 파일 객체 생성
            File outputFile = new File(localPath);

            // 부모 디렉토리가 없으면 생성
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            // S3에서 파일 다운로드 (AWS SDK가 직접 파일로 저장)
            s3Client.getObject(getObjectRequest, outputFile.toPath());

            log.info("파일 다운로드 완료 - S3 URL: {}, 로컬 경로: {}, 크기: {} MB",
                    s3Url, localPath, outputFile.length() / (1024 * 1024));

            return outputFile;

        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패 - URL: {}", s3Url, e);
            throw new IOException("S3 파일 다운로드 실패: " + e.getMessage(), e);
        }
    }

    /*
    다운로드 링크 생성
     */
    public String generatePresignedUrl(String fileUrl, int expirationMinutes) {
        try {
            // URL만 추출
            String key = extractKeyFromUrl(fileUrl);

            // AWS SDK의 S3Presigner 객체 생성
            // 기본: DefaultCredentialsProvider (IAM Role 등)
            // 로컬 개발: application-local.yml에 access/secret key가 있는 경우 StaticCredentialsProvider 사용
            S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                    .region(Region.of(region));
            
            if (accessKey != null && !accessKey.isEmpty() &&
                secretKey != null && !secretKey.isEmpty()) {
                presignerBuilder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
            } else {
                presignerBuilder.credentialsProvider(DefaultCredentialsProvider.create());
            }
            
            S3Presigner presigner = presignerBuilder.build();

            // 다운로드할 파일 지정
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(studyBlock)
                    .key(key)
                    .build();

            // 링크 유효시간 설정
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest =
                    presigner.presignGetObject(presignRequest);

            // Presigner 리소스 정리 (메모리 누수 방지)
            presigner.close();

            // 생성된 presigned URL 반환 (예: 60분만 유효한 다운로드 링크)
            log.info("Presigned URL 생성 완료 - Key: {}, 유효시간: {}분", key, expirationMinutes);
            return presignedRequest.url().toString();

            // URL 생성 실패시 로그 찍고 런타임 예외로 던져버림
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패 - key: {}", extractKeyFromUrl(fileUrl), e);
            throw new RuntimeException("Presigned URL 생성 실패", e);
        }
    }

    /*
        헬퍼 메서드들
     */

    // 현재 시간 + 랜덤 UUID + 원본 확장자를 붙여 중복 없는 파일명 생성
    private String generateFileName(String originalFileName) {

        // 현재 시간을 기반으로 문자열 생성
        // timestamp = "20251010_123456"
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // 랜덤값 고유 UUID 생성
        String uuid = UUID.randomUUID().toString().substring(0, 8); // 앞의 8자리만사용(너무 길면 지저분해짐)
        String extension = getFileExtension(originalFileName); // 파일 확장자 추출 -> .mp4
        return timestamp + "_" + uuid + extension; // "20251010_123456_d8e6b9f4.mp4"
    }

    // 파일 이름에서 확장자(.png .jpg .mp4등) 부분만 추출하는 메서드
    private String getFileExtension(String fileName){
        if (fileName == null || !fileName.contains(".")) { // 확장자가 없는경우
            return ""; // "" 반환 -> NullPointException 방지
        }
        // 문자열 내에서 마지막 점(.)이 등장하는 위치를 찾는다.
        // .이 있는 위치부터 문자열 끝까지 잘라냄 즉 -> .mp4
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * S3 객체의 정식 URL을 생성한다.
     *
     * <p><strong>주의:</strong> 이 URL은 내부 추적/보관용으로만 사용하고,
     * 클라이언트에는 직접 노출하지 않는다. 외부에는 반드시 presigned URL을 사용한다.</p>
     */
    private String getFileUrl(String key) {

        // format() -> 문자열 포맷팅함수
        // 즉 %s자리에는 각각뒤에 명시했떤 값이 들어간다.
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                studyBlock, s3Client.serviceClientConfiguration().region(), key);
    }

    // 	URL에서 https://...amazonaws.com/ 이후 부분만 잘라냄
    //	즉 "video/lecture-1/20251010_...uuid.mp4"
    // 이 key는 deletObject()나 presignedUrl 생성시 꼭 필요하다.
    private String extractKeyFromUrl(String fileUrl) {
        int index = fileUrl.indexOf(".com/");
        if (index == -1) {
            // 이미 key 형태라면 그대로 반환
            return fileUrl;
        }
        return fileUrl.substring(index + 5);
    }


}
