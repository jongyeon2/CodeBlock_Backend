package com.studyblock.global.security.oauth2;

public interface OAuth2UserInfo {
    String getProviderId();

    String getProvider();

    String getEmail();

    String getName();

    String getProfileImageUrl();
}
