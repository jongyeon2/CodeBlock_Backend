package com.studyblock.global.security;

import com.studyblock.global.security.jwt.JwtAuthenticationFilter;
import com.studyblock.global.security.oauth2.CustomOAuth2UserService;
import com.studyblock.global.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 * - JWT 쿠키 기반 인증
 * - OAuth2 카카오 로그인
 * - CORS 설정 (React 5173 포트)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 비활성화 (JWT Stateless 방식)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 인증 불필요한 경로 (회원가입/로그인 관련)
                        .requestMatchers(
                                "/api/auth/check-member-id",     // 아이디 중복 확인
                                "/api/auth/signup",              // 회원가입
                                "/api/auth/login",               // 로그인 (추후)
                                "/api/auth/refresh",             // 토큰 갱신
                                "/api/auth/send-reset-code",     // 비밀번호 찾기 인증 코드 전송
                                "/api/auth/verify-reset-code",   // 비밀번호 찾기 인증 코드 검증
                                "/api/auth/reset-password",      // 비밀번호 찾기 - 비밀번호 재설정
                                "/api/email/check",              // 이메일 중복 확인
                                "/api/email/send-code",          // 인증 코드 전송
                                "/api/email/verify"              // 인증 코드 검증
                        ).permitAll()

                        // OAuth2 관련
                        .requestMatchers("/oauth2/**").permitAll()

                        // 모니터링 및 문서
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()

                        // 개발 중: 모든 API 접근 허용 (추후 .authenticated()로 변경 필요)
                        // TODO: 배포 전 .requestMatchers("/api/**").authenticated()로 변경
                        .requestMatchers("/api/**").permitAll()

                        // 그 외 모든 요청 허용 (마지막에 한 번만!)
                        .anyRequest().permitAll()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // Form 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * CORS 설정: React(5173 포트)에서 백엔드(8080 포트)로 쿠키 포함 요청 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경 변수로부터 동적으로 읽기 (개발/배포 모두 지원)
        String allowedOrigins = System.getenv().getOrDefault(
                "CORS_ALLOWED_ORIGINS",
                "http://localhost:5173,http://localhost:5174"
        );
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 인증 정보(쿠키) 포함 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // 비밀번호 암호화를 위한 Bean객체 생성
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
