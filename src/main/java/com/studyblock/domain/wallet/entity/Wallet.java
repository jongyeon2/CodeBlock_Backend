package com.studyblock.domain.wallet.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WalletBalance> balances = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private List<WalletLedger> ledgers = new ArrayList<>();

    @Builder
    public Wallet(User user, Boolean isActive) {
        this.user = user;
        this.isActive = isActive != null ? isActive : true;
    }

    // Business methods
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public WalletBalance getBalanceByCurrency(String currencyCode) {
        return balances.stream()
                .filter(b -> b.getCurrencyCode().equals(currencyCode))
                .findFirst()
                .orElse(null);
    }

    public void addLedger(WalletLedger ledger) {
        this.ledgers.add(ledger);
    }
}
