package com.studyblock.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordVerifyResponse {
    //비밀번호 변경 시 사용
    private String tempToken;
}
