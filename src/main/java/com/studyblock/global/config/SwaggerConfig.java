package com.studyblock.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        // API 기본 정보 설정
        Info info = new Info()
                .title("StudyBlock API Document") // 문서 제목
                .version("1.0.0") // API 버전
                .description( // API 문서 설명
                        "환영합니다! StudyBlock은 e-running을 위한 누구나 자유롭게 사용할 수 있는 인터넷 강의 플랫폼입니다.\n\n" +
                        "이 API 문서는 StudyBlock의 모든 API를 설명합니다.\n\n" +
                        "### 주요 기능\n" +
                        "- 🎓 **Course Management**: 강좌 생성, 조회, 수정, 삭제\n" +
                        "- 📹 **Video Management**: 강의 비디오 업로드, 스트리밍, 관리\n" +
                        "- 👤 **User Management**: 사용자 관리 및 프로필\n" +
                        "- 💳 **Payment**: 결제 및 쿠키 시스템\n" +
                        "- 💬 **Community**: 게시판, 댓글, 리포트\n\n" +
                        "### 인증 방식\n" +
                        "- JWT Bearer Token 방식을 사용합니다.\n" +
                        "- 아래의 'Authorize' 버튼을 클릭하여 토큰을 입력하세요."
                )
                .contact(new io.swagger.v3.oas.models.info.Contact() // 개발팀의 연락정보
                        .name("StudyBlock Team")
                        .email("qorwhddus12@naver.com")
                        .url("https://github.com/studyblock"));

        return new OpenAPI()
                .info(info) // OpenAPI 객체 생성 후 위에서 만든 info를 적용함
                // Swagger에서 “Authorize” 버튼을 눌러 JWT 토큰을 넣을 수 있게 하는 설정.
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("Bearer Authentication"))

                // Swagger 문서에 사용할 인증방식을 정의
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", // Bearer Authentication 이름으로 인증 스키마 등록
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP) // HTTP 인증 방식
                                        .scheme("bearer") // Bearer Token 인증을 상요
                                        .bearerFormat("JWT") // 토큰 포맷이 JWT
                                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER) // 인증정보를 Header에 포함
                                        .name("Authorization") // Header이름이 Authorization
                                        .description("JWT 토큰을 입력하세요 (Bearer 제외)") // 입력창 아래에 안내문 표시
                        )
                );
    }
}
