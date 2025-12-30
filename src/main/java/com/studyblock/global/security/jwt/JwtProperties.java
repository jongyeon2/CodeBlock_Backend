package com.studyblock.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
    JWT 관련 설정값들을 외부 설정 파일
    application.yml에서 주입받아 관리하는 설정 클래스
 */

@Getter
@Setter
@Component // 스프링의 관리 대상

// application.yml에서 jwt. 로 시작하는 속성을 이 클래스에 자동 매핑
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;
}
