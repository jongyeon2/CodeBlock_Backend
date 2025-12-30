package com.studyblock.global.security.oauth2;

import java.util.Map;

/**
 * 구글 OAuth2 사용자 정보 파싱 클래스
 *
 * 구글 응답 구조:
 * {
 *   "sub": "1234567890",
 *   "email": "user@gmail.com",
 *   "name": "홍길동",
 *   "picture": "https://...",
 *   "email_verified": true
 * }
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getProvider() {
        return "GOOGLE";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }
}