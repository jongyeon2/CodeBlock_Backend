package com.studyblock.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/*
    AWS S3 버킷을 Bean으로 등록하는 설정 클래스
    
    로컬/배포 환경 모두 지원:
    - 로컬: application-local.yml의 credentials 설정 → StaticCredentialsProvider 사용
    - 배포: IAM Role (EC2 Instance Profile) → DefaultCredentialsProvider 사용
 */

@Configuration
public class AwsS3Config {

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${spring.cloud.aws.s3.bucket:study-block}")
    private String studyBlock;

    // 로컬 환경: application-local.yml에서 주입 (선택적)
    // 배포 환경: application-prod.yml에는 없으므로 빈 문자열
    @Value("${spring.cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:}")
    private String secretKey;

    /**
     * S3Client Bean 생성
     * 
     * 로컬 개발 환경: StaticCredentialsProvider 사용 (.env 파일의 값)
     * 배포 환경: DefaultCredentialsProvider 사용 (IAM Role 자동 탐색)
     */
    @Bean
    public S3Client s3Client() {
        // 로컬 환경: credentials가 명시된 경우 StaticCredentialsProvider 사용
        if (accessKey != null && !accessKey.isEmpty() && 
            secretKey != null && !secretKey.isEmpty()) {
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        } else {
            // 배포 환경: DefaultCredentialsProvider 사용 (IAM Role 자동 탐색)
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
    }
}
