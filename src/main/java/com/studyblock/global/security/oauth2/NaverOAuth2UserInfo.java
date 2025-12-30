package com.studyblock.global.security.oauth2;

import java.util.Map;

/**
 * 네이버 OAuth2 사용자 정보 파싱 클래스
 *
 * 네이버 응답 구조:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "12345678",
 *     "email": "user@naver.com",
 *     "name": "홍길동",
 *     "profile_image": "https://..."
 *   }
 * }
 */
public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return String.valueOf(response.get("id"));
    }

    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("profile_image");
    }
}