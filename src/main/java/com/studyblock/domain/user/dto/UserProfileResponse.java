package com.studyblock.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;           // 사용자 ID
    private LocalDate birth; //생일

    private String name;       // 사용자 이름
    private String intro;      // 자기소개
    private String memberId;   // 사용자 ID
    private String nickname;
    private String email;
    private String phone;
    private String img;        // 원본 프로필 이미지 URL (S3 절대 URL 또는 null)
    private String profileImageUrl; // presigned URL (클라이언트 표시용)
    private Enum status; //상태 1: 정상, 2: 탈퇴, 3: 휴면, 4: 정지
    private Enum jointype;
    private Enum gender;
    private String userType; // 사용자 유형: "강사" 또는 "회원"
    private LocalDateTime created_at;
}