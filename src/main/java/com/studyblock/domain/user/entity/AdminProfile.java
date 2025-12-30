package com.studyblock.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(length = 60)
    private String department;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive = true;

    @Builder
    public AdminProfile(User user, String displayName, String department, String phone) {
        this.user = user;
        this.userId = user.getId();
        this.displayName = displayName;
        this.department = department;
        this.phone = phone;
        this.isActive = true;
    }

    // Business methods
    public void updateInfo(String displayName, String department, String phone) {
        this.displayName = displayName;
        this.department = department;
        this.phone = phone;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
