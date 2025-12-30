package com.studyblock.domain.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 *  OAuth2 인증 URL 제공 컨트롤러
 * - 프론트엔드에서 SNS 로그인 URL을 요청하면 반환
 * - Spring Security가 자동 생성하는 /oauth2/authorization/{provider} URL을 알려줌
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/authorization")
public class OAuth2AuthorizationController {

    @Value("${server.domain:http://localhost:8080}")
    private String serverDomain;

    /**
     * SNS 로그인 URL 반환
     * GET /api/auth/authorization/{provider}
     *
     * @param provider SNS 제공자 (kakao, google, naver)
     * @return OAuth2 인증 URL
     *
     * @apiNote 프론트엔드 호출 예시:
     * axios.get('/auth/authorization/kakao')
     * → baseURL '/api' 때문에 실제로는 /api/auth/authorization/kakao 호출됨
     */
    @GetMapping("/{provider}")
    public ResponseEntity<?> getAuthorizationUrl(@PathVariable String provider) {

        // Spring Security OAuth2가 자동으로 만드는 URL
        // 이 URL은 컨트롤러 없이 Spring Security Filter가 처리함
        // server.domain에 이미 포트가 포함되어 있으므로 포트를 별도로 추가하지 않음
        String authUrl = serverDomain + "/oauth2/authorization/" + provider;

        log.info("OAuth2 인증 URL 요청 - provider: {}, url: {}", provider, authUrl);

        return ResponseEntity.ok(Map.of(
                "url", authUrl,
                "provider", provider
        ));
    }
}
