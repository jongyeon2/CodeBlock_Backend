package com.studyblock.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignupPathDto {
    private Integer local;     // LOCAL 가입자 수
    private Integer kakao;     // KAKAO 가입자 수
    private Integer naver;     // NAVER 가입자 수
    private Integer google;    // GOOGLE 가입자 수
}

