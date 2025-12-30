package com.studyblock.global.security.email;

/*
    이메일 인증 API 컨트롤러
 */

import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:NOT_SET}")
    private String emailUsername;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.password:NOT_SET}")
    private String emailPassword;

    // 테스트용 엔드포인트 - 설정 확인
    @GetMapping("/test-config")
    public ResponseEntity<?> testConfig() {
        return ResponseEntity.ok(Map.of(
                "emailUsername", emailUsername,
                "emailPasswordSet", emailPassword != null && !emailPassword.equals("NOT_SET") && emailPassword.length() > 0,
                "emailPasswordLength", emailPassword.length()
        ));
    }

    /*
        1. 이메일 중복 확인
        GET /api/email/check?email=test@example.com
     */

    @GetMapping("/check")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "이미 사용 중인 이메일입니다."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "available", true,
                "message", "사용 가능한 이메일입니다."
        ));
    }

    /*
        2. 이메일 인증 코드 전송
        POST /api/email/send-code
        Body: { "email": "test@example.com" }
     */

    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        try {
            log.info("이메일 인증 코드 전송 요청 - email: {}", email);
            emailService.sendVerificationEmail(email);
            log.info("이메일 인증 코드 전송 성공 - email: {}", email);
            return ResponseEntity.ok(Map.of(
                "success", true,
                    "message", "인증 코드가 이메일로 전송되었습니다."
            ));
        }  catch (Exception e) {
            log.error("❌ 이메일 인증 코드 전송 실패 - email: {}, 에러 타입: {}, 에러 메시지: {}",
                    email, e.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "인증 코드 전송에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /*
        3. 이메일 인증 코드 검증
        POST /api/email/verify
        Body: { "email": "test@example.com", "code": "123456" }
     */

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "verified", false,
                    "message", "이메일과 인증 코드를 입력해주세요"
            ));
        }

        boolean isValid = emailService.verifyCode(email, code);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "message", "이메일 인증이 완료되었습니다."
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "verified", false,
                    "message", "인증 코드가 올바르지 않거나 만료되었습니다."
            ));
        }
    }
}
