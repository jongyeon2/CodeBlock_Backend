package com.studyblock.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class UserUpdateDTO {

    private LocalDate birth;
    private String nickname;
    private String phone;
    private String intro;
//    private String Img;
    //이미지는 별도의 /upload-profile-image 엔드포인트를  통해서만 업데이트
}