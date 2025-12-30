package com.studyblock.domain.course.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.enums.OwnershipSource;
import com.studyblock.domain.course.enums.OwnershipStatus;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_ownership", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "section_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureOwnership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnershipStatus status = OwnershipStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnershipSource source;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    public LectureOwnership(User user, Section section, Order order, OwnershipSource source, LocalDateTime expiresAt) {
        this.user = user;
        this.section = section;
        this.order = order;
        this.status = OwnershipStatus.ACTIVE;
        this.source = source;
        this.expiresAt = expiresAt;
    }

    // Business methods
    public void revoke() {
        this.status = OwnershipStatus.REVOKED;
    }

    public void expire() {
        this.status = OwnershipStatus.EXPIRED;
    }

    public void activate() {
        this.status = OwnershipStatus.ACTIVE;
    }

    public boolean isActive() {
        if (this.status != OwnershipStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    public boolean isExpired() {
        return this.status == OwnershipStatus.EXPIRED ||
               (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }
}
