package com.studyblock.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.enums.InstructorChannelStatus;
import com.studyblock.domain.user.enums.InstructorPayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorProfileResponse {
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    private String career;
    private String skills;

    @JsonProperty("channel_name")
    private String channelName;

    @JsonProperty("channel_url")
    private String channelUrl;

    private String bio;

    @JsonProperty("contact_email")
    private String contactEmail;

    @JsonProperty("pay_status")
    private InstructorPayStatus payStatus;

    @JsonProperty("channel_status")
    private InstructorChannelStatus channelStatus;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static InstructorProfileResponse from(InstructorProfile p) {
        return InstructorProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .career(p.getCareer())
                .skills(p.getSkills())
                .channelName(p.getChannelName())
                .channelUrl(p.getChannelUrl())
                .bio(p.getBio())
                .contactEmail(p.getContactEmail())
                .payStatus(p.getPayStatus())
                .channelStatus(p.getChannelStatus())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

