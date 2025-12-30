package com.studyblock.domain.coupon.entity;

import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.coupon.enums.CouponStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "coupon_code", nullable = false, length = 50)
    private String couponCode;

    @Column(name = "is_used", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isUsed = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponStatus status = CouponStatus.AVAILABLE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public UserCoupon(User user,
                    Coupon coupon,
                    String couponCode,
                    LocalDateTime expiresAt) {
        this.user = user;
        this.coupon = coupon;
        this.couponCode = couponCode;
        this.expiresAt = expiresAt;
    }

    public void markUsed(LocalDateTime usedAt) {
        this.isUsed = true;
        this.usedAt = usedAt != null ? usedAt : LocalDateTime.now();
        this.status = CouponStatus.USED;
    }

    // 쿠폰 사용 처리 (간편 메서드)
    public void use() {
        this.markUsed(LocalDateTime.now());
    }

    // 상태 전이: AVAILABLE → RESERVED
    public void reserve() {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("예약할 수 없는 쿠폰입니다: " + this.status);
        }
        this.status = CouponStatus.RESERVED;
    }

    // 상태 전이: RESERVED → AVAILABLE (롤백)
    public void release() {
        if (this.status == CouponStatus.RESERVED) {
            this.status = CouponStatus.AVAILABLE;
        }
    }

    // 만료 처리
    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    public boolean isAvailable() {
        return this.status == CouponStatus.AVAILABLE 
            && LocalDateTime.now().isBefore(this.expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isReserved() {
        return this.status == CouponStatus.RESERVED;
    }

    public boolean isUsedStatus() {
        return this.status == CouponStatus.USED;
    }
}


