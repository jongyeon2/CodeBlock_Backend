package com.studyblock.domain.coupon.entity;

import com.studyblock.domain.coupon.enums.CouponTargetUsers;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_users", nullable = false, length = 16)
    private CouponTargetUsers targetUsers;

    @Column(name = "target_user_ids", columnDefinition = "JSON")
    private String targetUserIds;

    @Column(name = "issued_count", nullable = false)
    private Integer issuedCount = 0;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public CouponEvent(String name,
                       String description,
                       Coupon coupon,
                       CouponTargetUsers targetUsers,
                       String targetUserIds,
                       Boolean isActive,
                       User createdBy) {
        this.name = name;
        this.description = description;
        this.coupon = coupon;
        this.targetUsers = targetUsers;
        this.targetUserIds = targetUserIds;
        this.isActive = isActive != null ? isActive : true;
        this.createdBy = createdBy;
    }
}


