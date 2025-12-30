package com.studyblock.global.util;

import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

// 인증 관련 공통 유틸리티
// Controller에서 반복되는 인증 처리 로직을 통합
@Component
@RequiredArgsConstructor
public class AuthenticationUtils {

    private final UserRepository userRepository;

    // 인증된 사용자 엔티티 추출
    // authentication null 체크 및 User 타입 캐스팅을 안전하게 처리
    public User extractAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("인증 정보가 없습니다");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        }

        throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다");
    }

    // 인증된 사용자 ID 추출 (확장 버전)
    // User 엔티티, UserDetails, String(username/email) 모두 지원
    public Long extractAuthenticatedUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("인증 정보가 없습니다");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return parseUserIdOrLookup(username);
        }

        if (principal instanceof String) {
            return parseUserIdOrLookup((String) principal);
        }

        throw new IllegalStateException("인증 정보에서 사용자 ID를 추출할 수 없습니다");
    }

    // 인증 여부 확인
    // Controller에서 반복되는 인증 체크를 간소화
    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User;
    }

    // 문자열을 Long으로 파싱하거나 이메일로 조회
    private Long parseUserIdOrLookup(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("인증 사용자 식별값이 비어있습니다");
        }

        if (isNumeric(value)) {
            return Long.parseLong(value);
        }

        // 숫자가 아니면 이메일로 가정하고 조회
        User user = userRepository.findByEmail(value)
                .orElseThrow(() -> new IllegalStateException("이메일로 사용자를 찾을 수 없습니다: " + value));
        return user.getId();
    }

    // 숫자 문자열 판별
    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0 && (c == '+' || c == '-')) {
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
