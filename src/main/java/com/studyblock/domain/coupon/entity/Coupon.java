package com.studyblock.domain.coupon.entity;

import com.studyblock.domain.coupon.enums.CouponType;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CouponType type;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "minimum_amount")
    private Integer minimumAmount;

    @Column(name = "maximum_discount")
    private Integer maximumDiscount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

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
    public Coupon(String name,
                  String description,
                  CouponType type,
                  Integer discountValue,
                  Integer minimumAmount,
                  Integer maximumDiscount,
                  LocalDateTime validFrom,
                  LocalDateTime validUntil,
                  Integer usageLimit,
                  Boolean isActive,
                  User createdBy) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.discountValue = discountValue;
        this.minimumAmount = minimumAmount;
        this.maximumDiscount = maximumDiscount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.usageLimit = usageLimit;
        this.isActive = isActive != null ? isActive : true;
        this.createdBy = createdBy;
    }
}


