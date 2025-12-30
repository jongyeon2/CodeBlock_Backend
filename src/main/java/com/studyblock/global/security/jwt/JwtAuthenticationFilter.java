package com.studyblock.global.security.jwt;

import com.studyblock.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 쿠키 기반 인증 필터
 * - 요청 헤더의 쿠키에서 accessToken 추출
 * - 토큰 검증 후 SecurityContext에 인증 정보 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        // 1. 쿠키에서 accessToken 추출
        String token = extractTokenFromCookie(request, "accessToken");

        // 2. 토큰이 있고 유효하면 인증 처리
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                // 토큰에서 userId 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                // DB에서 사용자 조회 (UserRole, Role Fetch Join - N+1 문제 방지)
                // findByIdWithRoles()를 사용하여 userRoles를 함께 조회
                // AOP에서 userRoles 접근 시 LazyInitializationException 방지
                userRepository.findByIdWithRoles(userId).ifPresent(user -> {
                    if (user.isActive()) {
                        // Spring Security 인증 객체 생성
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        // SecurityContext에 인증 정보 저장
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT 인증 성공 - userId: {}, URI: {}", userId, requestURI);
                    } else {
                        log.warn("비활성화된 사용자 - userId: {}, URI: {}", userId, requestURI);
                    }
                });
            } catch (Exception e) {
                log.error("JWT 인증 처리 중 오류 발생 - URI: {}", requestURI, e);
            }
        } else {
            // 토큰이 없거나 유효하지 않은 경우
            if (token == null) {
                log.debug("JWT 토큰 없음 - URI: {}", requestURI);
            } else {
                log.debug("JWT 토큰 유효하지 않음 - URI: {}", requestURI);
            }
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 토큰 추출
     */
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
