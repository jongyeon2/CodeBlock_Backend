package com.studyblock.domain.wallet.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_balance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"wallet_id", "currency_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletBalance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "currency_code", nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode = "KRW";

    @Column(nullable = false)
    private Long amount = 0L;

    // V16: 동결 관련
    @Column(name = "frozen_amount", nullable = false)
    private Long frozenAmount = 0L;

    // V16: 사용 가능 금액은 DB에서 계산 컬럼으로 관리되므로 여기서는 메서드로 제공
    // @Column(name = "available_amount") - GENERATED ALWAYS AS (`amount` - `frozen_amount`) STORED

    // V16: 추적 정보
    @Column(name = "last_updated_by", length = 50)
    private String lastUpdatedBy;

    @Column(name = "update_reason", length = 255)
    private String updateReason;

    // V16: 한도 관리
    @Column(name = "daily_limit")
    private Long dailyLimit;

    @Column(name = "monthly_limit")
    private Long monthlyLimit;

    @Builder
    public WalletBalance(Wallet wallet, String currencyCode, Long amount, Long frozenAmount,
                         String lastUpdatedBy, String updateReason, Long dailyLimit, Long monthlyLimit) {
        this.wallet = wallet;
        this.currencyCode = currencyCode != null ? currencyCode : "KRW";
        this.amount = amount != null ? amount : 0L;
        this.frozenAmount = frozenAmount != null ? frozenAmount : 0L;
        this.lastUpdatedBy = lastUpdatedBy;
        this.updateReason = updateReason;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
    }

    // Business methods
    public void addAmount(Long amount) {
        addAmount(amount, null, null);
    }

    public void addAmount(Long amount, String updatedBy, String reason) {
        this.amount += amount;
        this.lastUpdatedBy = updatedBy;
        this.updateReason = reason;
    }

    public void subtractAmount(Long amount) {
        subtractAmount(amount, null, null);
    }

    public void subtractAmount(Long amount, String updatedBy, String reason) {
        if (getAvailableAmount() < amount) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.amount -= amount;
        this.lastUpdatedBy = updatedBy;
        this.updateReason = reason;
    }

    public boolean hasSufficientBalance(Long requiredAmount) {
        return getAvailableAmount() >= requiredAmount;
    }

    // V16 추가 비즈니스 메서드들
    public Long getAvailableAmount() {
        return this.amount - this.frozenAmount;
    }

    public void freezeAmount(Long amountToFreeze, String updatedBy, String reason) {
        if (getAvailableAmount() < amountToFreeze) {
            throw new IllegalStateException("동결할 금액이 사용 가능 금액을 초과합니다.");
        }
        this.frozenAmount += amountToFreeze;
        this.lastUpdatedBy = updatedBy;
        this.updateReason = reason;
    }

    public void unfreezeAmount(Long amountToUnfreeze, String updatedBy, String reason) {
        if (this.frozenAmount < amountToUnfreeze) {
            throw new IllegalStateException("동결 해제할 금액이 동결된 금액을 초과합니다.");
        }
        this.frozenAmount -= amountToUnfreeze;
        this.lastUpdatedBy = updatedBy;
        this.updateReason = reason;
    }

    public void setDailyLimit(Long dailyLimit) {
        if (dailyLimit != null && dailyLimit <= 0) {
            throw new IllegalArgumentException("일일 한도는 0보다 커야 합니다.");
        }
        this.dailyLimit = dailyLimit;
    }

    public void setMonthlyLimit(Long monthlyLimit) {
        if (monthlyLimit != null && monthlyLimit <= 0) {
            throw new IllegalArgumentException("월 한도는 0보다 커야 합니다.");
        }
        this.monthlyLimit = monthlyLimit;
    }

    public boolean hasDailyLimit() {
        return this.dailyLimit != null;
    }

    public boolean hasMonthlyLimit() {
        return this.monthlyLimit != null;
    }

    public boolean exceedsDailyLimit(Long usageAmount) {
        return this.dailyLimit != null && usageAmount > this.dailyLimit;
    }

    public boolean exceedsMonthlyLimit(Long usageAmount) {
        return this.monthlyLimit != null && usageAmount > this.monthlyLimit;
    }

    public boolean hasFrozenAmount() {
        return this.frozenAmount > 0;
    }

    public Double getFrozenRatio() {
        if (this.amount == 0) {
            return 0.0;
        }
        return (double) this.frozenAmount / this.amount * 100;
    }
}
