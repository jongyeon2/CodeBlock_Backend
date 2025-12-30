package com.studyblock.global.security.email;

/*
    이메일 인증 코드 전송 서비스
 */

import com.studyblock.global.aop.annotation.RateLimited;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailVerificationRepository verificationRepository;
    private final PasswordResetVerificationRepository passwordResetVerificationRepository;
    private static final SecureRandom random = new SecureRandom();

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail; // 발신자 이메일 주소

    /*
        6자리 랜덤 인증 코드 생성
     */
    private String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    /*
        이메일 인증 코드 전송 및 Redis 저장
     */
    public void sendVerificationEmail(String email) {
        // 1. 6자리 인증 코드 생성
        String code = generateVerificationCode();

        // 2. Redis에 저장 (5분 TTL)
        // EmailVerification 엔티티를 만들려면
        // email주소와 code, 설정 시간이 필요하다.
        EmailVerification verification = new EmailVerification(email, code, 300L);
        verificationRepository.save(verification); // CrudRepository를 사용해서 redis에 저장

        // 3. 이메일 전송
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail); // ⭐ 발신자 이메일 설정 (필수!)
            helper.setTo(email);
            helper.setSubject("[CodeBlock] 이메일 인증 코드");
            helper.setText(buildEmailContent(code), true); // HTML 형식

            mailSender.send(message);
            log.info("이메일 인증 코드 전송 완료 - email: {}", email);

        } catch (MessagingException e) {
            // 전송 실패했을 경우
            log.error("이메일 전송 실패 - email: {}", email, e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /*
        이메일 인증 코드 검증
     */
    public boolean verifyCode(String email, String code) {
        return verificationRepository.findById(email)
                .map(verification -> {
                    boolean isValid = verification.getCode().equals(code);
                    if (isValid) {
                        // 인증 성공 시 Redis에서 삭제
                        verificationRepository.delete(verification);
                        log.info("이메일 인증 성공 - email: {}", email);
                    }
                    return isValid;
                })
                .orElse(false); // Redis에 없으면 말료됨
    }

    /*
        비밀번호 찾기용 이메일 인증 코드 전송 및 Redis 저장
        - AOP @RateLimited로 재전송 제한 적용 (1분에 1회)
     */
    @RateLimited(key = "email", duration = 60, message = "1분 후 다시 시도해주세요.")
    public void sendPasswordResetEmail(String email) {
        // 1. 6자리 인증 코드 생성
        String code = generateVerificationCode();

        // 2. Redis에 저장 (5분 TTL)
        // 비밀번호 찾기용 별도 엔티티 사용 (회원가입용과 구분)
        PasswordResetVerification verification = new PasswordResetVerification(email, code, 300L);
        passwordResetVerificationRepository.save(verification);

        // 3. 이메일 전송
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("[CodeBlock] 비밀번호 찾기 인증 코드");
            helper.setText(buildPasswordResetEmailContent(code), true); // HTML 형식

            mailSender.send(message);
            log.info("비밀번호 찾기 인증 코드 전송 완료 - email: {}", email);

        } catch (MessagingException e) {
            log.error("비밀번호 찾기 이메일 전송 실패 - email: {}", email, e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /*
        비밀번호 찾기 인증 코드 검증
     */
    public boolean verifyPasswordResetCode(String email, String code) {
        return passwordResetVerificationRepository.findById(email)
                .map(verification -> {
                    boolean isValid = verification.getCode().equals(code);
                    if (isValid) {
                        // 인증 성공 시 Redis에서 삭제
                        passwordResetVerificationRepository.delete(verification);
                        log.info("비밀번호 찾기 인증 성공 - email: {}", email);
                    }
                    return isValid;
                })
                .orElse(false); // Redis에 없으면 만료됨
    }

    /*
        이메일 HTML 템플릿 (회원가입용)
     */
    private String buildEmailContent(String code) {
        return """
                 <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                  <h2 style="color: #4F46E5;">CodeBlock 이메일 인증</h2>
                                  <p>안녕하세요!</p>
                                  <p>회원가입을 위한 인증 코드는 다음과 같습니다:</p>
                                  <div style="background-color: #F3F4F6; padding: 20px; text-align: center; margin: 20px 0;">
                                      <span style="font-size: 32px; font-weight: bold; color: #4F46E5; letter-spacing: 5px;">
                                          %s
                                      </span>
                                  </div>
                                  <p>이 코드는 <strong>5분간 유효</strong>합니다.</p>
                                  <p>본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.</p>
                                  <hr style="margin-top: 30px; border: none; border-top: 1px solid #E5E7EB;">
                                  <p style="color: #6B7280; font-size: 12px;">
                                      © 2025 StudyBlock. All rights reserved.
                                  </p>
                              </div>
                """.formatted(code);
    }

    /*
        비밀번호 찾기용 이메일 HTML 템플릿
     */
    private String buildPasswordResetEmailContent(String code) {
        return """
                 <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                  <h2 style="color: #DC2626;">CodeBlock 비밀번호 찾기</h2>
                                  <p>안녕하세요!</p>
                                  <p>비밀번호 찾기를 위한 인증 코드는 다음과 같습니다:</p>
                                  <div style="background-color: #FEF2F2; padding: 20px; text-align: center; margin: 20px 0; border: 2px solid #DC2626;">
                                      <span style="font-size: 32px; font-weight: bold; color: #DC2626; letter-spacing: 5px;">
                                          %s
                                      </span>
                                  </div>
                                  <p>이 코드는 <strong>5분간 유효</strong>합니다.</p>
                                  <p style="color: #DC2626; font-weight: bold;">⚠️ 본인이 요청하지 않았다면 즉시 고객센터로 연락해주세요.</p>
                                  <hr style="margin-top: 30px; border: none; border-top: 1px solid #E5E7EB;">
                                  <p style="color: #6B7280; font-size: 12px;">
                                      © 2025 StudyBlock. All rights reserved.
                                  </p>
                              </div>
                """.formatted(code);
    }
}
