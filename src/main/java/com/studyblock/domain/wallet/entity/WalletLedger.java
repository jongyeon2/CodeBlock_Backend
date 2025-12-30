package com.studyblock.domain.wallet.entity;

import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 강한 결합 제거: order/payment/batch FK는 제거하고, reference_type/id로 추적

    @Column(name = "type", nullable = false, length = 20)
    private String type; // CHARGE, DEBIT, REFUND, EXPIRE

    @Column(name = "cookie_amount", nullable = false)
    private Integer cookieAmount;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency = "KRW";

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public WalletLedger(User user,
                        String type, Integer cookieAmount, Integer balanceAfter, String notes) {
        this.user = user;
        this.type = type;
        this.cookieAmount = cookieAmount;
        this.balanceAfter = balanceAfter;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isCharge() {
        return "CHARGE".equals(this.type);
    }

    public boolean isDebit() {
        return "DEBIT".equals(this.type);
    }

    public boolean isRefund() {
        return "REFUND".equals(this.type);
    }

    public boolean isExpire() {
        return "EXPIRE".equals(this.type);
    }

    // 느슨한 참조 설정 helper
    public void setReference(String referenceType, Long referenceId, String currency) {
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        if (currency != null) {
            this.currency = currency;
        }
    }
}
