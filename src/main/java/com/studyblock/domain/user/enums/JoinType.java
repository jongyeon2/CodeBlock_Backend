package com.studyblock.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원 가입 경로 구분
 * - LOCAL : 일반 회원가입(이메일/비밀번호)
 * - KAKAO : 카카오 소셜 로그인
 * - GOOGLE : 구글 소셜 로그인
 * - NAVER : 네이버 소셜 로그인
 */

@Getter
@AllArgsConstructor
public enum JoinType {
    LOCAL(0, "로컬"),
    KAKAO(1, "카카오"),
    GOOGLE(2, "구글"),
    NAVER(3, "네이버");

    private final int value;
    private final String description;

    /**
     * 정수 값으로 JoinType 찾기
     * fromValue: DB ↔ Enum 변환용
     */
    public static JoinType fromValue(int value) {
        for (JoinType type : JoinType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid JoinType value: " + value);
    }

    /**
     * 문자열로 JoinType 찾기 (OAuth2 Provider 이름 → JoinType)
     * fromString: OAuth2 제공자 이름 → Enum 변환용
     */
    public static JoinType fromString(String provider) {
        try {
            return JoinType.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + provider);
        }
    }
}
