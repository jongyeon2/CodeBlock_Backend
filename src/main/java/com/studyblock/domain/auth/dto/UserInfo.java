package com.studyblock.domain.auth.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private Long id;
    private String memberId;
    private String name;
    private String email;
    private String img; // 원본 프로필 이미지 URL (S3 절대 URL 또는 null)
    private String profileImageUrl; // presigned URL (클라이언트 노출용)
    private List<String> roles; // ["USER"], ["INSTRUCTOR"], ["ADMIN"] 등

}
