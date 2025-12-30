package com.studyblock.global.security.oauth2;

import com.studyblock.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

// Oauth2 로그인(카카오, 구글 등)을 통해 인증된 사용자의 정보를
// SpringSecurity가 인식할 수 있게 감싸주는 "인증 객체" 역할이다.
@Getter
public class PrincipalDetails implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    // SpringSecurity가 "OAuth2 인증된 사용자"로 인식할 수 있게 하는 인터페이스
    public PrincipalDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    // OAuth2User 인터페이스의 메서드 (반드시 구현해야 함)
    // 카카오, 구글 같은 OAuth2 서버에서 내려주는 사용자 프로필 정보를 JSON으로 반환
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // 로그인한 사용자가 가지고 있는 권한을 반환하는 메서드
    // Security는 내부적으로 이 메서드 권한을 확인함
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(

                // 여기서 모든 OAuth2 로그인 사용자를 기본적으로 USER 권한으로 부여.
                new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    @Override
    public String getName() {
        return user.getName();
    }
}
