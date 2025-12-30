package com.studyblock.domain.user.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.user.enums.InstructorPayStatus;
import com.studyblock.domain.user.enums.InstructorChannelStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "instructor_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstructorProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 강사 경력
    private String career;

    // 강사 스킬
    private String skills;

    @Column(name = "channel_name", nullable = false, length = 70)
    private String channelName;

    @Column(name = "channel_url", nullable = false, unique = true)
    private String channelUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "contact_email")
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_status", nullable = false)
    private InstructorPayStatus payStatus = InstructorPayStatus.READY;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_status", nullable = false)
    private InstructorChannelStatus channelStatus = InstructorChannelStatus.INACTIVE;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "instructor", fetch = FetchType.LAZY)
    private List<com.studyblock.domain.course.entity.Lecture> lectures = new ArrayList<>();

    @Builder
    public InstructorProfile(User user, String career, String skills, String channelName, String channelUrl, 
                           String bio, String contactEmail) {
        this.user = user;
        this.career = career;
        this.skills = skills;
        this.channelName = channelName;
        this.channelUrl = channelUrl;
        this.bio = bio;
        this.contactEmail = contactEmail;
        this.payStatus = InstructorPayStatus.READY;
        this.channelStatus = InstructorChannelStatus.INACTIVE;
        this.isActive = true;
    }

    // Business methods
    public void updateChannelInfo(String channelName, String channelUrl, String bio) {
        this.channelName = channelName;
        this.channelUrl = channelUrl;
        this.bio = bio;
    }

    public void updateContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public void updateCareer(String career) {
        this.career = career;
    }

    public void updateSkills(String skills) {
        this.skills = skills;
    }

    public void updateCareerAndSkills(String career, String skills) {
        this.career = career;
        this.skills = skills;
    }

    public void activatePay() {
        this.payStatus = InstructorPayStatus.ACTIVE;
    }

    public void suspendPay() {
        this.payStatus = InstructorPayStatus.SUSPENDED;
    }

    public void setPayStatus(InstructorPayStatus status) {
        this.payStatus = status;
    }

    public void activateChannel() {
        this.channelStatus = InstructorChannelStatus.ACTIVE;
    }

    public void deactivateChannel() {
        this.channelStatus = InstructorChannelStatus.INACTIVE;
    }

    public void setChannelStatus(InstructorChannelStatus status) {
        this.channelStatus = status;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    // Helper methods
    public boolean isPayActive() {
        return this.payStatus == InstructorPayStatus.ACTIVE;
    }

    public boolean isChannelActive() {
        return this.channelStatus == InstructorChannelStatus.ACTIVE;
    }

    public boolean isFullyActive() {
        return this.isActive && this.isPayActive() && this.isChannelActive();
    }

    public String getInstructorName() {
        return this.user != null ? this.user.getName() : null;
    }

    public String getInstructorNickname() {
        return this.user != null ? this.user.getNickname() : null;
    }

    public String getInstructorEmail() {
        return this.user != null ? this.user.getEmail() : null;
    }
}