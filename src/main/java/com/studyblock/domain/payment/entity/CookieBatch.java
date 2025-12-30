package com.studyblock.domain.payment.entity;

import com.studyblock.domain.payment.enums.CookieSource;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.wallet.enums.CookieType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cookie_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CookieBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_items_id")
    private OrderItem orderItem;

    @Column(name = "qty_total", nullable = false)
    private Integer qtyTotal;

    @Column(name = "qty_remain", nullable = false)
    private Integer qtyRemain;

    @Enumerated(EnumType.STRING)
    @Column(name = "cookie_type", nullable = false)
    private CookieType cookieType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CookieSource source;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public CookieBatch(User user, OrderItem orderItem, Integer qtyTotal, CookieType cookieType,
                       CookieSource source, LocalDateTime expiresAt) {
        this.user = user;
        this.orderItem = orderItem;
        this.qtyTotal = qtyTotal;
        this.qtyRemain = qtyTotal;
        this.cookieType = cookieType;
        this.source = source;
        this.expiresAt = expiresAt;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void use(Integer amount) {
        if (this.qtyRemain < amount) {
            throw new IllegalStateException("Insufficient cookies in batch");
        }
        this.qtyRemain -= amount;
        if (this.qtyRemain == 0) {
            this.isActive = false;
        }
    }

    public void restore(Integer amount) {
        this.qtyRemain += amount;
        if (this.qtyRemain > 0) {
            this.isActive = true;
        }
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isAvailable() {
        return isActive && qtyRemain > 0 && !isExpired();
    }
}
