package com.studyblock.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.studyblock.domain.user.enums.InstructorChannelStatus;
import com.studyblock.domain.user.enums.InstructorPayStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstructorProfilePatchRequest {

    private String career;
    private String skills;

    @JsonProperty("channel_name")
    private String channelName;

    @JsonProperty("channel_url")
    @Pattern(regexp = "^(https?)://.+$", message = "channel_url은 http(s) URL이어야 합니다")
    private String channelUrl;

    private String bio;

    @JsonProperty("contact_email")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String contactEmail;

    @JsonProperty("pay_status")
    private InstructorPayStatus payStatus;

    @JsonProperty("channel_status")
    private InstructorChannelStatus channelStatus;

    @JsonProperty("is_active")
    private Boolean isActive;
}

