package com.studyblock.domain.payment.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cookie_bundle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CookieBundle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_krw", nullable = false)
    private Long price;

    @Column(name = "base_cookie_amount", nullable = false)
    private Integer baseCookieAmount;

    @Column(name = "bonus_cookie_amount", nullable = false)
    private Integer bonusCookieAmount = 0;

    @Column(name = "total_cookie_amount", insertable = false, updatable = false)
    private Integer totalCookieAmount;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive = true;

    @Builder
    public CookieBundle(String name, Long price, Integer baseCookieAmount, Integer bonusCookieAmount) {
        this.name = name;
        this.price = price;
        this.baseCookieAmount = baseCookieAmount;
        this.bonusCookieAmount = bonusCookieAmount != null ? bonusCookieAmount : 0;
        this.isActive = true;
    }

    // Business methods
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateBundle(String name, Long price, Integer baseCookieAmount, Integer bonusCookieAmount) {
        this.name = name;
        this.price = price;
        this.baseCookieAmount = baseCookieAmount;
        this.bonusCookieAmount = bonusCookieAmount;
    }

    public Integer calculateTotalCookies() {
        return baseCookieAmount + bonusCookieAmount;
    }
}
