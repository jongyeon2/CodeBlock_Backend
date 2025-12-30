package com.studyblock.domain.payment.entity;

import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_limit_aggregate",
       uniqueConstraints = @UniqueConstraint(name = "uk_daily_limit_user_date", 
                                           columnNames = {"user_id", "date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyLimitAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "cash_sum", nullable = false)
    private Integer cashSum = 0;

    @Column(name = "cookie_sum", nullable = false)
    private Integer cookieSum = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder
    public DailyLimitAggregate(User user, LocalDate date, Integer cashSum, Integer cookieSum) {
        this.user = user;
        this.date = date;
        this.cashSum = cashSum != null ? cashSum : 0;
        this.cookieSum = cookieSum != null ? cookieSum : 0;
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void addCashAmount(Integer amount) {
        if (amount != null && amount > 0) {
            this.cashSum += amount;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void addCookieAmount(Integer amount) {
        if (amount != null && amount > 0) {
            this.cookieSum += amount;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void subtractCashAmount(Integer amount) {
        if (amount != null && amount > 0) {
            this.cashSum = Math.max(0, this.cashSum - amount);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void subtractCookieAmount(Integer amount) {
        if (amount != null && amount > 0) {
            this.cookieSum = Math.max(0, this.cookieSum - amount);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public Integer getTotalAmount() {
        return this.cashSum + this.cookieSum;
    }

    public boolean isToday() {
        return this.date.equals(LocalDate.now());
    }

    public boolean isYesterday() {
        return this.date.equals(LocalDate.now().minusDays(1));
    }

    public boolean isThisWeek() {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        return !this.date.isBefore(weekStart) && !this.date.isAfter(now);
    }

    public boolean isThisMonth() {
        LocalDate now = LocalDate.now();
        return this.date.getYear() == now.getYear() && 
               this.date.getMonth() == now.getMonth();
    }

    public boolean isHighCashUsage() {
        return this.cashSum >= 1000000; // 100만원 이상
    }

    public boolean isHighCookieUsage() {
        return this.cookieSum >= 10000; // 1만 쿠키 이상
    }

    public boolean isHighTotalUsage() {
        return getTotalAmount() >= 1000000; // 총 100만원 이상
    }

    public String getUsageTier() {
        int total = getTotalAmount();
        if (total >= 1000000) {
            return "HIGH";
        } else if (total >= 100000) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    public boolean hasCashUsage() {
        return this.cashSum > 0;
    }

    public boolean hasCookieUsage() {
        return this.cookieSum > 0;
    }

    public boolean hasAnyUsage() {
        return this.cashSum > 0 || this.cookieSum > 0;
    }

    public Double getCashRatio() {
        int total = getTotalAmount();
        if (total == 0) {
            return 0.0;
        }
        return (double) this.cashSum / total * 100;
    }

    public Double getCookieRatio() {
        int total = getTotalAmount();
        if (total == 0) {
            return 0.0;
        }
        return (double) this.cookieSum / total * 100;
    }

    public void reset() {
        this.cashSum = 0;
        this.cookieSum = 0;
        this.updatedAt = LocalDateTime.now();
    }
}
