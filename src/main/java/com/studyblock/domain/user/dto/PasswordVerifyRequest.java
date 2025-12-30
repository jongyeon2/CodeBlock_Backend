package com.studyblock.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordVerifyRequest {
    //비밀번호 변경 시 사용
    private String currentPassword;
}
