package com.studyblock.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

/*
    로그인을 위한 DTO
    왜 LocalSignupRequest를 재사용하지 않냐?
    회원가입은 여러 필드가 필요하지만 로그인은 아이디와 비밀번호만 필요
    단일 책임 원칙을 생각해 만들었음
 */

@Getter
@Setter
public class LoginRequest {
    private String memberId;
    private String password;
}
