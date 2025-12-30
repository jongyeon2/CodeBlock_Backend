package com.studyblock.domain.user.dto;

import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.time.LocalDate;
import java.util.Optional;

@Data
@AllArgsConstructor
@Builder
public class InstructorProfileDTO {
    private String career;
    private String skills;
    private String channelName;
    private String channelUrl;
    private String bio;
    private String contactEmail;
    private String name;
    private String phone;
    private String memberId;
    private Enum gender;

    private LocalDate birth;

    // entity -> dto
    public static InstructorProfileDTO from(InstructorProfile instructorProfile) {
        User user = instructorProfile.getUser();
        return InstructorProfileDTO.builder()
                .career(instructorProfile.getCareer())
                .skills(instructorProfile.getSkills())
                .channelName(instructorProfile.getChannelName())
                .channelUrl(instructorProfile.getChannelUrl())
                .bio(instructorProfile.getBio())
                .contactEmail(instructorProfile.getContactEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .birth(user.getBirth())
                .gender(user.getGenderEnum())
                .build();
    }

}
