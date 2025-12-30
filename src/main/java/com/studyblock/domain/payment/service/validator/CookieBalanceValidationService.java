package com.studyblock.domain.payment.service.validator;

import com.studyblock.domain.wallet.entity.Wallet;
import com.studyblock.domain.wallet.entity.WalletBalance;
import com.studyblock.domain.wallet.repository.WalletBalanceRepository;
import com.studyblock.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 쿠키 잔액 검증 전담 서비스
// 단일 책임: 쿠키 잔액 확인
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieBalanceValidationService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;

    // 쿠키 잔액 확인 (쿠키 결제 시)
    public void validateCookieBalance(Long userId, Long cookieAmount) {
        // 쿠키 결제를 사용하지 않는 경우
        if (cookieAmount == null || cookieAmount <= 0L) {
            return; // 검증 통과
        }

        // 사용자의 지갑 조회
        Wallet wallet = walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalStateException(
                    "지갑을 찾을 수 없습니다. 사용자 ID: " + userId
                ));

        // 쿠키(KRW) 잔액 조회
        WalletBalance balance = walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), "KRW")
                .orElseThrow(() -> new IllegalStateException(
                    "쿠키 잔액 정보를 찾을 수 없습니다."
                ));

        // 충분한 잔액이 있는지 확인
        if (!balance.hasSufficientBalance(cookieAmount)) {
            throw new IllegalArgumentException(
                String.format("쿠키 잔액이 부족합니다. 필요: %d, 보유: %d", 
                    cookieAmount, balance.getAvailableAmount())
            );
        }
    }
}

