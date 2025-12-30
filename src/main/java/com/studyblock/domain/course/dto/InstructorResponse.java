package com.studyblock.domain.course.dto;

import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.enums.InstructorChannelStatus;
import com.studyblock.domain.user.enums.InstructorPayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사 프로필 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class InstructorResponse {

    private Long id;
    private String name;
    private String nickname;
    private String bio;
    private String intro;
    private String channelName;
    private String channelUrl;
    private String contactEmail;
    private String profileImageUrl;             // presigned URL (노출용)
    private String profileImageOriginalUrl;     // 원본 프로필 이미지 URL
    private String career;
    private String skills;
    private InstructorPayStatus payStatus;
    private InstructorChannelStatus channelStatus;
    private Boolean isActive;

    /**
     * Entity -> DTO 변환
     */
    public static InstructorResponse from(InstructorProfile instructorProfile) {
        if (instructorProfile == null) {
            throw new IllegalArgumentException("강사 정보가 존재하지 않습니다.");
        }

        return InstructorResponse.builder()
                .id(instructorProfile.getId())
                .name(instructorProfile.getInstructorName())
                .nickname(instructorProfile.getInstructorNickname())
                .bio(instructorProfile.getBio())
                .intro(instructorProfile.getUser() != null ? instructorProfile.getUser().getIntro() : null)
                .channelName(instructorProfile.getChannelName())
                .channelUrl(instructorProfile.getChannelUrl())
                .contactEmail(instructorProfile.getContactEmail())
                .profileImageUrl(instructorProfile.getUser() != null ? instructorProfile.getUser().getImg() : null)
                .profileImageOriginalUrl(instructorProfile.getUser() != null ? instructorProfile.getUser().getImg() : null)
                .career(instructorProfile.getCareer())
                .skills(instructorProfile.getSkills())
                .payStatus(instructorProfile.getPayStatus())
                .channelStatus(instructorProfile.getChannelStatus())
                .isActive(instructorProfile.getIsActive())
                .build();
    }
}
