package com.studyblock.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/*
    로컬 회원 가입시 유저 데이터를 가공해서 전달해줄 DTO
 */

@Getter
@Setter
public class LocalSignupRequest {
    private String memberId;
    private String email;
    private String password;
    private String name;
    private LocalDate birth; // "2000-10-15" 형식이 자동으로 LocalDate로 변환됨
    private String phone;
    private Integer jointype; // 0 = Local
    private Integer gender; // 0 = 여자, 1 = 남자

    // 강사 회원가입 여부 (true = 강사, false or null = 일반 사용자)
    private Boolean isCreator;

    // 주소 정보 (선택 사항)
    private String zipcode;
    private String baseAddress;
    private String detailAddress;
}
