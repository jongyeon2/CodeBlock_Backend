package com.studyblock.global.security.oauth2;

import com.studyblock.domain.auth.service.RedisRefreshTokenService;
import com.studyblock.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 JWT를 HttpOnly 쿠키로 발급하고 React로 리다이렉트
 * Redis를 사용하여 RefreshToken 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenService redisRefreshTokenService;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1. 인증된 사용자 정보 가져오기
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principal.getUser().getId();

        log.info("OAuth2 로그인 성공 - userId: {}", userId);

        // 2. JWT 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 3. RefreshToken을 Redis에 저장 (TTL 7일 자동 설정)
        redisRefreshTokenService.saveRefreshToken(userId, refreshToken);

        // 4. HttpOnly + Secure + SameSite=None 쿠키로 설정
        addTokenCookie(response, "accessToken", accessToken, 15 * 60); // 15분
        addTokenCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60); // 7일

        // 5. React 프론트엔드로 리다이렉트
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("success", "true")
                .build()
                .toUriString();

        log.info("리다이렉트 URL: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * ResponseCookie를 사용한 쿠키 생성 (HttpOnly + Secure + SameSite=None)
     * SameSite=None을 사용하려면 Secure=true가 필수이므로 환경변수로 제어
     * 서브도메인 간 쿠키 공유를 위해 COOKIE_DOMAIN 환경변수 지원
     */
    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // HTTPS 환경 여부 확인 (환경변수로 제어)
        boolean isSecure = "true".equalsIgnoreCase(System.getenv("COOKIE_SECURE"));
        // 쿠키 도메인 설정 (서브도메인 공유용, 예: .codeblock.cloud)
        String cookieDomain = System.getenv("COOKIE_DOMAIN");

        if (isSecure) {
            // HTTPS: SameSite=None; Secure 사용 (프로덕션 환경)
            ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                    .httpOnly(true)              // XSS 방어: JavaScript로 접근 불가
                    .secure(true)                // HTTPS 환경에서는 true 필수
                    .path("/")                   // 모든 경로에서 쿠키 사용 가능
                    .maxAge(maxAge)              // 만료 시간 (초 단위)
                    .sameSite("None");           // CORS 환경에서 쿠키 전송 허용

            // 쿠키 도메인이 설정된 경우 추가 (서브도메인 공유용)
            if (cookieDomain != null && !cookieDomain.isEmpty()) {
                builder.domain(cookieDomain);
                log.debug("쿠키 도메인 설정: {}", cookieDomain);
            }

            ResponseCookie cookie = builder.build();
            response.addHeader("Set-Cookie", cookie.toString());
        } else {
            // HTTP: SameSite=Lax 사용 (로컬 개발 환경)
            ResponseCookie cookie = ResponseCookie.from(name, value)
                    .httpOnly(true)              // XSS 방어: JavaScript로 접근 불가
                    .secure(false)               // 로컬 개발 환경에서는 false
                    .path("/")                   // 모든 경로에서 쿠키 사용 가능
                    .maxAge(maxAge)              // 만료 시간 (초 단위)
                    .sameSite("Lax")             // 로컬 환경에서는 Lax 사용
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        }
    }
}
