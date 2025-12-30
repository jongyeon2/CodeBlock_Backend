package com.studyblock.domain.auth.controller;

import com.studyblock.domain.auth.dto.LocalSignupRequest;
import com.studyblock.domain.auth.dto.LoginRequest;
import com.studyblock.domain.auth.dto.UserInfo;
import com.studyblock.domain.auth.service.AuthService;
import com.studyblock.domain.auth.service.RedisRefreshTokenService;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.global.security.email.EmailService;
import com.studyblock.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 API 컨트롤러 (Redis 버전)
 * - /auth/refresh: RefreshToken으로 AccessToken 재발급
 * - /auth/logout: 로그아웃 (Redis에서 RefreshToken 삭제 + 쿠키 제거)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider; // 토큰 생성 및 검증
    private final RedisRefreshTokenService redisRefreshTokenService; // Redis 전용 RefreshToken
    private final AuthService authService;
    private final EmailService emailService; // 이메일 인증 코드 전송
    private final UserRepository userRepository; // 사용자 조회

    @PostMapping("login")
    public ResponseEntity<?> login(
            // Login을 위한 DTD와 쿠키 설정을 위한 response 객체를 파라미터로 받는다.
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            log.info("로그인 요청 - memberId: {}", request.getMemberId());

            // 1. AuthService에서 로그인 처리 (토큰 생성 + RefreshToken Redis 저장)
            Map<String, Object> result = authService.loginLocalUser(request);

            // 2. AccessToken을 HttpOnly 쿠키로 설정 (15분)
            addTokenCookie(response, "accessToken",
                    (String) result.get("accessToken"), 15 * 60);

            // 3. RefreshToken을 HttpOnly 쿠키로 설정 (7일)
            addTokenCookie(response, "refreshToken",
                    (String) result.get("refreshToken"), 7 * 24 * 60 * 60);

            log.info("로그인 성공 - userId: {}", result.get("userId"));

            // 4. 성공 응답 (쿠키는 자동 전송, body에는 사용자 정보만)
            return ResponseEntity.ok(Map.of(
                    "message", "로그인에 성공했습니다.",
                    "userId", result.get("userId"),
                    "memberId", result.get("memberId"),
                    "name", result.get("name"),
                    "roles", result.get("roles")
            ));
        } catch (IllegalArgumentException e) {
            log.warn("로그인 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("로그인 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "로그인 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * RefreshToken으로 AccessToken 재발급
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 쿠키에서 RefreshToken 추출
            String refreshToken = extractTokenFromCookie(request, "refreshToken");

            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "RefreshToken이 없습니다."));
            }

            // 2. RefreshToken 유효성 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "유효하지 않은 RefreshToken입니다."));
            }

            // 3. JWT에서 userId 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

            // 4. Redis에서 저장된 RefreshToken 조회 및 비교
            String storedToken = redisRefreshTokenService.getRefreshToken(userId)
                    .orElseThrow(() -> new RuntimeException("Redis에 저장된 RefreshToken을 찾을 수 없습니다."));

            if (!storedToken.equals(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "RefreshToken이 일치하지 않습니다."));
            }

            // 5. 새로운 AccessToken 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(userId);

            // 6. 새로운 AccessToken을 쿠키로 설정
            addTokenCookie(response, "accessToken", newAccessToken, 15 * 60); // 15분

            log.info("AccessToken 재발급 성공 - userId: {}", userId);

            return ResponseEntity.ok(Map.of(
                    "message", "AccessToken이 재발급되었습니다.",
                    "userId", userId
            ));

        } catch (Exception e) {
            log.error("AccessToken 재발급 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "토큰 재발급 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃 (Redis에서 RefreshToken 삭제 + 쿠키 제거)
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 쿠키에서 RefreshToken 추출
            String refreshToken = extractTokenFromCookie(request, "refreshToken");

            if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                // 2. JWT에서 userId 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

                // 3. Redis에서 RefreshToken 삭제
                redisRefreshTokenService.deleteRefreshToken(userId);
            }

            // 4. 쿠키 삭제 (maxAge=0)
            deleteCookie(response, "accessToken");
            deleteCookie(response, "refreshToken");

            log.info("로그아웃 성공");

            return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));

        } catch (Exception e) {
            log.error("로그아웃 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "로그아웃 중 오류가 발생했습니다."));
        }
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

    /**
     * 쿠키 생성
     * HTTP 환경에서 cross-origin 쿠키 지원을 위해 SameSite 속성을 제거
     */
    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // HTTPS 환경 여부 확인 (환경변수로 제어)
        boolean isSecure = "true".equalsIgnoreCase(System.getenv("COOKIE_SECURE"));

        if (isSecure) {
            // HTTPS: SameSite=None; Secure 사용
            ResponseCookie cookie = ResponseCookie.from(name, value)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(maxAge)
                    .sameSite("None")
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        } else {
            // HTTP: SameSite 속성 없이 쿠키 설정 (cross-origin 요청 허용)
            String cookieHeader = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly",
                    name, value, maxAge);
            response.addHeader("Set-Cookie", cookieHeader);
        }
    }

    /**
     * 쿠키 삭제
     */
    private void deleteCookie(HttpServletResponse response, String name) {
        // HTTPS 환경 여부 확인 (환경변수로 제어)
        boolean isSecure = "true".equalsIgnoreCase(System.getenv("COOKIE_SECURE"));

        if (isSecure) {
            // HTTPS: SameSite=None; Secure 사용
            ResponseCookie cookie = ResponseCookie.from(name, "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(0)
                    .sameSite("None")
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        } else {
            // HTTP: SameSite 속성 없이 쿠키 삭제
            String cookieHeader = String.format("%s=; Path=/; Max-Age=0; HttpOnly", name);
            response.addHeader("Set-Cookie", cookieHeader);
        }
    }

    /*
        로컬 회원가입 (일반 회원가입)
        POST /auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @ModelAttribute LocalSignupRequest request,
            HttpServletResponse response) {
        log.info("로컬 회원가입 요청 - memberId: {}, email: {}",
                request.getMemberId(), request.getEmail());

        try {
            // 1. 회원 가입 처리 및 JWT 토큰 발급
            Map<String, String> tokens = authService.registerLocalUser(request);

            // 2. AccessToken을 HttpOnly 쿠키로 설정 (15분)
            addTokenCookie(response, "accessToken",
                    tokens.get("accessToken"), 15 * 60);

            // 3. RefreshToken을 HttpOnly 쿠키로 설정 (7일)
            addTokenCookie(response, "refreshToken",
                    tokens.get("refreshToken"), 7 * 24 * 60 * 60);

            // 4. Redis에 RefreshToken 저장
            Long userId = jwtTokenProvider.getUserIdFromToken(tokens.get("refreshToken"));
            redisRefreshTokenService.saveRefreshToken(userId, tokens.get("refreshToken"));

            log.info("회원가입 성공 - userId: {}, 쿠키 설정 완료", userId);

            // 5. 성공 응답 (쿠키는 자동 전송, body에는 사용자 정보만)
            return ResponseEntity.ok(Map.of(
                    "message", "회원가입이 완료되었습니다.",
                    "userId", userId
            ));
        } catch (IllegalArgumentException e) {
            log.error("회원가입 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    /*
        아이디 중복 확인
        /auth/check-member-id?memberId=xxx
     */
    @GetMapping("/check-member-id")
    public ResponseEntity<?> checkMemberId(@RequestParam String memberId) {
        log.info("아이디 중복 확인 - memberId: {}", memberId);

        // 서비스 로직에 있는 아이디 중복확인 메서드를 사용해 검증
        boolean available = authService.isMemberIdAvailable(memberId);

        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "사용 가능한 아이디입니다" : "이미 사용중인 아이디입니다."
        ));
    }

    // AuthController에 추가할 메서드
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // 1. 쿠키에서 토큰 추출
            String accessToken = extractTokenFromCookie(request, "accessToken");

            if (accessToken == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "error", "인증이 필요합니다"));
            }

            // 2. Service에서 사용자 정보 조회
            UserInfo userInfo = authService.getCurrentUserInfo(accessToken);

            // 3. 응답 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", userInfo
            ));

        } catch (IllegalArgumentException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "error", e.getMessage()));

        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "서버 오류가 발생했습니다"));
        }
    }

    /**
     * 비밀번호 찾기 인증 코드 전송
     * POST /api/auth/send-reset-code
     * Body: { "email": "user@example.com" }
     *
     * - AOP @RateLimited로 1분에 1회만 전송 가능
     * - 가입된 이메일인지 확인 (보안상 존재 여부는 명확히 알리지 않음)
     * - Redis에 인증 코드 저장 (5분 TTL)
     */
    @PostMapping("/send-reset-code")
    public ResponseEntity<?> sendResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 1. 이메일 유효성 검증
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        log.info("비밀번호 찾기 인증 코드 전송 요청 - email: {}", email);

        try {
            // 2. 가입된 이메일인지 확인
            // 보안상: 존재하지 않는 이메일이라고 명확히 알리지 않음
            boolean emailExists = userRepository.existsByEmail(email);

            if (!emailExists) {
                // 가입되지 않은 이메일이어도 동일한 성공 메시지 반환 (보안)
                log.warn("비밀번호 찾기 요청 - 가입되지 않은 이메일: {}", email);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "인증 코드가 이메일로 전송되었습니다."
                ));
            }

            // 3. 비밀번호 찾기용 인증 코드 전송
            // AOP @RateLimited가 적용되어 1분에 1회만 전송 가능
            emailService.sendPasswordResetEmail(email);

            log.info("비밀번호 찾기 인증 코드 전송 성공 - email: {}", email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증 코드가 이메일로 전송되었습니다."
            ));

        } catch (IllegalArgumentException e) {
            // 재전송 제한 등 AOP에서 발생한 예외
            log.warn("비밀번호 찾기 인증 코드 전송 제한 - email: {}, message: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("비밀번호 찾기 인증 코드 전송 실패 - email: {}", email, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "인증 코드 전송에 실패했습니다."
            ));
        }
    }

    /**
     * 비밀번호 찾기 인증 코드 검증
     * POST /api/auth/verify-reset-code
     * Body: { "email": "user@example.com", "code": "123456" }
     *
     * - Redis에 저장된 비밀번호 찾기용 인증 코드 검증
     * - 인증 성공 시 Redis에서 인증 코드 삭제 (일회성)
     * - 인증 실패 시 적절한 에러 메시지 반환
     */
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        // 1. 이메일과 인증 코드 유효성 검증
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "message", "이메일과 인증 코드를 입력해주세요."
            ));
        }

        log.info("비밀번호 찾기 인증 코드 검증 요청 - email: {}", email);

        try {
            // 2. Redis에서 비밀번호 찾기용 인증 코드 검증
            boolean isValid = emailService.verifyPasswordResetCode(email, code);

            if (isValid) {
                log.info("비밀번호 찾기 인증 코드 검증 성공 - email: {}", email);
                return ResponseEntity.ok(Map.of(
                        "verified", true,
                        "message", "인증 코드가 확인되었습니다."
                ));
            } else {
                log.warn("비밀번호 찾기 인증 코드 검증 실패 - email: {}, code: {}", email, code);
                return ResponseEntity.ok(Map.of(
                        "verified", false,
                        "message", "인증 코드가 올바르지 않거나 만료되었습니다."
                ));
            }

        } catch (Exception e) {
            log.error("비밀번호 찾기 인증 코드 검증 중 오류 발생 - email: {}", email, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "verified", false,
                    "message", "인증 코드 검증 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 비밀번호 찾기 - 비밀번호 재설정
     * POST /api/auth/reset-password
     * Body: { "email": "user@example.com", "newPassword": "newPassword123!", "confirmPassword": "newPassword123!" }
     *
     * - 이메일로 User 찾기 (이메일 통합 정책: 1 이메일 = 1 계정)
     * - 비밀번호와 비밀번호 확인 일치 검증
     * - 비밀번호 암호화 후 업데이트
     * - DB 저장
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        // 1. 이메일, 새 비밀번호, 비밀번호 확인 유효성 검증
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "새 비밀번호를 입력해주세요."
            ));
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "비밀번호 확인을 입력해주세요."
            ));
        }

        // 2. 비밀번호와 비밀번호 확인 일치 검증
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            ));
        }

        // 3. 비밀번호 길이 검증 (최소 8자 이상 권장, 프로젝트 정책에 따라 조정)
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "비밀번호는 최소 8자 이상이어야 합니다."
            ));
        }

        log.info("비밀번호 재설정 요청 - email: {}", email);

        try {
            // 4. 비밀번호 재설정 (이메일로 User 찾기 → 비밀번호 암호화 → 업데이트 → DB 저장)
            authService.resetPassword(email, newPassword);

            log.info("비밀번호 재설정 성공 - email: {}", email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "비밀번호가 성공적으로 변경되었습니다."
            ));

        } catch (IllegalArgumentException e) {
            // 가입되지 않은 이메일 등
            log.warn("비밀번호 재설정 실패 - email: {}, message: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("비밀번호 재설정 중 오류 발생 - email: {}", email, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "비밀번호 재설정 중 오류가 발생했습니다."
            ));
        }
    }

    /*
        이메일 관련 API는 EmailVerificationController로 이동
        - GET  /email/check        (이메일 중복 확인)
        - POST /email/send-code    (인증 코드 전송 - 회원가입용)
        - POST /email/verify       (인증 코드 검증 - 회원가입용)
     */
}