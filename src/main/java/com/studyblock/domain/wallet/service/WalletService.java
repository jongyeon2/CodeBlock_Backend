package com.studyblock.domain.wallet.service;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.entity.Payment;
import com.studyblock.domain.payment.entity.CookieBatch;
import com.studyblock.domain.payment.repository.CookieBatchRepository;
import com.studyblock.domain.payment.enums.CookieSource;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.wallet.entity.Wallet;
import com.studyblock.domain.wallet.entity.WalletBalance;
import com.studyblock.domain.wallet.entity.WalletLedger;
import com.studyblock.domain.wallet.enums.CookieType;
import com.studyblock.domain.wallet.repository.WalletBalanceRepository;
import com.studyblock.domain.wallet.repository.WalletLedgerRepository;
import com.studyblock.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final CookieBatchRepository cookieBatchRepository;
    private final UserRepository userRepository;

private static final String CURRENCY_KRW = "KRW";

    // 사용자 지갑 조회 (없으면 생성)
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        return walletRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    // 지갑 생성
                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .isActive(true)
                            .build();
                    walletRepository.save(wallet);

                    // 기본 통화(KRW) 잔액 생성
                    WalletBalance balance = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(CURRENCY_KRW)
                            .amount(0L)
                            .frozenAmount(0L)
                            .build();
                    walletBalanceRepository.save(balance);

                    log.info("새 지갑 생성 - userId: {}, walletId: {}", userId, wallet.getId());
                    return wallet;
                });
    }

    // 쿠키 잔액 조회 (없으면 0원 잔액 레코드 생성)
    // REQUIRES_NEW: readOnly 트랜잭션에서 호출되어도 별도 쓰기 트랜잭션으로 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getCookieBalance(Long userId) {
        Wallet wallet = walletRepository.findByUser_Id(userId)
                .orElse(null);

        if (wallet == null) {
            // 지갑 없으면 생성까지 수행
            wallet = getOrCreateWallet(userId);
        }

        WalletBalance balance = walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), CURRENCY_KRW)
                .orElse(null);

        if (balance == null) {
            WalletBalance newBalance = WalletBalance.builder()
                    .wallet(wallet)
                    .currencyCode(CURRENCY_KRW)
                    .amount(0L)
                    .frozenAmount(0L)
                    .build();
            walletBalanceRepository.save(newBalance);
            log.info("🎯 지갑 잔액이 없어 새로 생성(getCookieBalance) - walletId: {}, currency: {}", wallet.getId(), CURRENCY_KRW);
            return newBalance.getAvailableAmount();
        }

        return balance.getAvailableAmount();
    }

    // 사용 가능한 쿠키 잔액 확인
    // readOnly = false: getCookieBalance()가 지갑 생성 시 INSERT를 수행할 수 있음
    @Transactional(readOnly = false)
    public boolean hasSufficientBalance(Long userId, Long requiredAmount) {
        Long balance = getCookieBalance(userId);
        return balance >= requiredAmount;
    }

    // 쿠키 차감 (결제 시 사용)
    // 보너스 쿠키(FREE)부터 차감하고, 그 다음 유료 쿠키(PAID) 차감
    @Transactional
    public void deductCookies(Long userId, Integer cookieAmount, Order order, Payment payment, String notes) {
        if (cookieAmount == null || cookieAmount <= 0) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 지갑 조회
        Wallet wallet = getOrCreateWallet(userId);

        // 잔액 조회 (없으면 생성)
        WalletBalance balance = walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), CURRENCY_KRW)
                .orElseGet(() -> {
                    WalletBalance newBalance = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(CURRENCY_KRW)
                            .amount(0L)
                            .frozenAmount(0L)
                            .build();
                    walletBalanceRepository.save(newBalance);
                    log.info("🎯 지갑 잔액이 없어 새로 생성 - walletId: {}, currency: {}", wallet.getId(), CURRENCY_KRW);
                    return newBalance;
                });

        // 잔액 확인
        if (!balance.hasSufficientBalance(cookieAmount.longValue())) {
            throw new IllegalStateException(
                String.format("쿠키 잔액이 부족합니다. 필요: %d, 보유: %d", 
                    cookieAmount, balance.getAvailableAmount())
            );
        }

        LocalDateTime now = LocalDateTime.now();
        int remainingAmount = cookieAmount;
        int freeBatchesUsed = 0;
        int paidBatchesUsed = 0;

        // 1단계: 보너스 쿠키(FREE)부터 차감 - 유효기간이 짧은 것부터
        List<CookieBatch> freeBatches = cookieBatchRepository.findAvailableFreeBatchesByUser(user, now);
        for (CookieBatch batch : freeBatches) {
            if (remainingAmount <= 0) {
                break;
            }

            int batchAvailable = batch.getQtyRemain();
            int deductFromBatch = Math.min(remainingAmount, batchAvailable);

            batch.use(deductFromBatch);
            cookieBatchRepository.save(batch);
            remainingAmount -= deductFromBatch;
            freeBatchesUsed++;

            log.info("보너스 쿠키 차감 - batchId: {}, cookieType: {}, deduct: {}, qtyRemain: {} -> {}", 
                    batch.getId(), batch.getCookieType(), deductFromBatch, 
                    batchAvailable, batch.getQtyRemain());
        }

        // 2단계: 남은 양이 있으면 유료 쿠키(PAID) 차감 - FIFO 순서
        if (remainingAmount > 0) {
            List<CookieBatch> paidBatches = cookieBatchRepository.findAvailablePaidBatchesByUser(user, now);
            for (CookieBatch batch : paidBatches) {
                if (remainingAmount <= 0) {
                    break;
                }

                int batchAvailable = batch.getQtyRemain();
                int deductFromBatch = Math.min(remainingAmount, batchAvailable);

                batch.use(deductFromBatch);
                cookieBatchRepository.save(batch);
                remainingAmount -= deductFromBatch;
                paidBatchesUsed++;

                log.info("유료 쿠키 차감 - batchId: {}, cookieType: {}, deduct: {}, qtyRemain: {} -> {}", 
                        batch.getId(), batch.getCookieType(), deductFromBatch, 
                        batchAvailable, batch.getQtyRemain());
            }
        }

        // 잔액이 부족한 경우 (이론적으로는 발생하지 않아야 함)
        if (remainingAmount > 0) {
            throw new IllegalStateException(
                String.format("쿠키 배치가 부족합니다. 남은 필요량: %d", remainingAmount)
            );
        }

        // WalletBalance 차감
        balance.subtractAmount(
            cookieAmount.longValue(), 
            "SYSTEM", 
            notes != null ? notes : "강의 구매"
        );
        walletBalanceRepository.save(balance);

        // WalletLedger 기록
        WalletLedger ledger = WalletLedger.builder()
                .user(user)
                .type("DEBIT")
                .cookieAmount(-cookieAmount) // 차감은 음수로 기록
                .balanceAfter(balance.getAmount().intValue())
                .notes(notes != null ? notes : "강의 구매로 쿠키 차감")
                .build();
        // 느슨한 참조 이중 기록
        if (order != null) {
            ledger.setReference("ORDER", order.getId(), "KRW");
        } else if (payment != null) {
            ledger.setReference("PAYMENT", payment.getId(), "KRW");
        }
        walletLedgerRepository.save(ledger);

        log.info("쿠키 차감 완료 - userId: {}, amount: {}, balanceAfter: {}, 사용배치수: FREE={}, PAID={}", 
                userId, cookieAmount, balance.getAmount(), freeBatchesUsed, paidBatchesUsed);
    }

    // 쿠키 환불 (환불 시 사용)
    @Transactional
    public void refundCookies(Long userId, Integer cookieAmount, Order order, String notes) {
        if (cookieAmount == null || cookieAmount <= 0) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 지갑 조회
        Wallet wallet = getOrCreateWallet(userId);

        // 잔액 조회 (없으면 생성)
        WalletBalance balance = walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), CURRENCY_KRW)
                .orElseGet(() -> {
                    WalletBalance newBalance = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(CURRENCY_KRW)
                            .amount(0L)
                            .frozenAmount(0L)
                            .build();
                    walletBalanceRepository.save(newBalance);
                    log.info("🎯 지갑 잔액이 없어 새로 생성 - walletId: {}, currency: {}", wallet.getId(), CURRENCY_KRW);
                    return newBalance;
                });

        // 1) WalletBalance 증가(쿠키양 증가)
        balance.addAmount(
            cookieAmount.longValue(),
            "SYSTEM",
            notes != null ? notes : "환불"
        );
        walletBalanceRepository.save(balance);

        // 2) CookieBatch FIFO 롤백: 소비가 FIFO였다면 환불은 역방향(LIFO)로 복구
        int remaining = cookieAmount;
        // 최신 배치부터 역순으로(최근에 소비된 것으로 가정) qtyRemain을 복구
        for (CookieBatch batch : cookieBatchRepository.findByUserOrderByCreatedAtDesc(user)) {
            if (remaining <= 0) break;
            int currentRemain = batch.getQtyRemain() != null ? batch.getQtyRemain() : 0;
            int capacity = (batch.getQtyTotal() != null ? batch.getQtyTotal() : 0) - currentRemain;
            if (capacity <= 0) continue;
            int toRestore = Math.min(capacity, remaining);
            batch.restore(toRestore);
            cookieBatchRepository.save(batch);
            remaining -= toRestore;
        }
        // 남는 양이 있으면(배치 용량 초과) 무료쿠키 배치로 신규 생성하여 복구
        if (remaining > 0) {
            CookieBatch overflow = CookieBatch.builder()
                    .user(user)
                    .orderItem(null)
                    .qtyTotal(remaining)
                    .cookieType(CookieType.FREE)
                    .source(CookieSource.BONUS)
                    .expiresAt(LocalDateTime.now().plusYears(3))
                    .build();
            cookieBatchRepository.save(overflow);
        }

        // 3) WalletLedger 기록
        WalletLedger ledger = WalletLedger.builder()
                .user(user)
                .type("REFUND")
                .cookieAmount(cookieAmount) // 환불은 양수로 기록
                .balanceAfter(balance.getAmount().intValue())
                .notes(notes != null ? notes : "쿠키 환불")
                .build();
        if (order != null) {
            ledger.setReference("ORDER", order.getId(), "KRW");
        }
        walletLedgerRepository.save(ledger);

        log.info("쿠키 환불 완료 - userId: {}, amount: {}, balanceAfter: {}", 
                userId, cookieAmount, balance.getAmount());
    }

    // 쿠키 충전 (현금으로 쿠키 구매)
    @Transactional
    public void chargeCookies(Long userId, Integer cookieAmount, Order order, Payment payment, OrderItem orderItem, String notes) {
        // 하위호환: 기존 호출은 보너스 0으로 처리
        chargeCookies(userId, cookieAmount, 0, order, payment, orderItem, notes);
    }

    // 쿠키 충전 (유료/보너스 분리 적립)
    @Transactional
    public void chargeCookies(Long userId, Integer paidCookieAmount, Integer bonusCookieAmount,
                                Order order, Payment payment, OrderItem orderItem, String notes) {
        int safePaid = paidCookieAmount != null ? paidCookieAmount : 0;
        int safeBonus = bonusCookieAmount != null ? bonusCookieAmount : 0;
        int totalToAdd = safePaid + safeBonus;

        log.info("🎯 chargeCookies 시작 - userId: {}, paid: {}, bonus: {}, orderId: {}, paymentId: {}",
                userId, safePaid, safeBonus, order != null ? order.getId() : "null", payment != null ? payment.getId() : "null");

        if (totalToAdd <= 0) {
            throw new IllegalArgumentException("충전할 쿠키 수량이 올바르지 않습니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        log.info("🎯 사용자 조회 완료 - userId: {}", userId);

        // 지갑 조회 또는 생성
        Wallet wallet = getOrCreateWallet(userId);
        log.info("🎯 지갑 조회/생성 완료 - walletId: {}", wallet.getId());

        // 잔액 조회 (없으면 생성)
        WalletBalance balance = walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), CURRENCY_KRW)
                .orElseGet(() -> {
                    WalletBalance newBalance = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(CURRENCY_KRW)
                            .amount(0L)
                            .frozenAmount(0L)
                            .build();
                    walletBalanceRepository.save(newBalance);
                    log.info("🎯 지갑 잔액이 없어 새로 생성 - walletId: {}, currency: {}", wallet.getId(), CURRENCY_KRW);
                    return newBalance;
                });
        log.info("🎯 지갑 잔액 조회 완료 - balanceId: {}, 현재 잔액: {}", balance.getId(), balance.getAmount());

        // 총 쿠키(유료+보너스) 잔액 증가
        balance.addAmount(
            (long) totalToAdd,
            "SYSTEM",
            notes != null ? notes : "쿠키 충전"
        );
        walletBalanceRepository.save(balance);
        log.info("🎯 지갑 잔액 업데이트 완료 - 증가: {}, 새로운 잔액: {}", totalToAdd, balance.getAmount());

        // WalletLedger 기록 (유료 분)
        if (safePaid > 0) {
            WalletLedger paidLedger = WalletLedger.builder()
                    .user(user)
                    .type("CHARGE")
                    .cookieAmount(safePaid)
                    .balanceAfter(balance.getAmount().intValue())
                    .notes(notes != null ? notes : "쿠키 충전")
                    .build();
            if (order != null) {
                paidLedger.setReference("ORDER", order.getId(), "KRW");
            } else if (payment != null) {
                paidLedger.setReference("PAYMENT", payment.getId(), "KRW");
            }
            walletLedgerRepository.save(paidLedger);
            log.info("🎯 WalletLedger 저장 완료(유료) - ledgerId: {}", paidLedger.getId());
        }

        // WalletLedger 기록 (보너스 분)
        if (safeBonus > 0) {
            WalletLedger bonusLedger = WalletLedger.builder()
                    .user(user)
                    .type("CHARGE")
                    .cookieAmount(safeBonus)
                    .balanceAfter(balance.getAmount().intValue())
                    .notes("보너스 쿠키 지급")
                    .build();
            if (order != null) {
                bonusLedger.setReference("ORDER", order.getId(), "KRW");
            } else if (payment != null) {
                bonusLedger.setReference("PAYMENT", payment.getId(), "KRW");
            }
            walletLedgerRepository.save(bonusLedger);
            log.info("🎯 WalletLedger 저장 완료(보너스) - ledgerId: {}", bonusLedger.getId());
        }

        // CookieBatch 생성 (유료 분)
        if (safePaid > 0) {
            CookieBatch paidBatch = CookieBatch.builder()
                    .user(user)
                    .orderItem(orderItem)
                    .qtyTotal(safePaid)
                    .cookieType(CookieType.PAID)
                    .source(CookieSource.PURCHASE)
                    .expiresAt(null) // 유료 쿠키는 만료 없음
                    .build();
            cookieBatchRepository.save(paidBatch);
            log.info("🎯 CookieBatch 저장 완료(유료) - batchId: {}", paidBatch.getId());
        }

        // CookieBatch 생성 (보너스 분, 무료 쿠키)
        if (safeBonus > 0) {
            CookieBatch bonusBatch = CookieBatch.builder()
                    .user(user)
                    .orderItem(orderItem)
                    .qtyTotal(safeBonus)
                    .cookieType(CookieType.FREE)
                    .source(CookieSource.BONUS)
                    .expiresAt(LocalDateTime.now().plusYears(3)) // 보너스 3년 만료
                    .build();
            cookieBatchRepository.save(bonusBatch);
            log.info("🎯 CookieBatch 저장 완료(보너스) - batchId: {}", bonusBatch.getId());
        }

        log.info("🎯 쿠키 충전 완료 - userId: {}, paid: {}, bonus: {}, balanceAfter: {}",
                userId, safePaid, safeBonus, balance.getAmount());
    }

    // WalletBalance 조회 (내부용)
    @Transactional(readOnly = true)
    public WalletBalance getWalletBalance(Long userId) {
        Wallet wallet = walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalStateException("지갑을 찾을 수 없습니다"));

        return walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), CURRENCY_KRW)
                .orElseThrow(() -> new IllegalStateException("지갑 잔액을 찾을 수 없습니다"));
    }

    // 관리자 쿠키 조정 (증가/감소)
    @Transactional
    public void adminAdjustCookies(Long targetUserId, Integer adjustAmount, Long adminUserId, String reason) {
        if (adjustAmount == null || adjustAmount == 0) {
            throw new IllegalArgumentException("조정할 쿠키 수량이 올바르지 않습니다");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("조정 사유를 입력해주세요");
        }

        // 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

        // 관리자 조회
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다"));

        // 지갑 조회 또는 생성
        Wallet wallet = getOrCreateWallet(targetUserId);

        // 잔액 조회
        WalletBalance balance = walletBalanceRepository
                .findByWallet_IdAndCurrencyCode(wallet.getId(), CURRENCY_KRW)
                .orElseThrow(() -> new IllegalStateException("지갑 잔액을 찾을 수 없습니다"));

        // 감소 시 잔액 확인
        if (adjustAmount < 0) {
            long absAmount = Math.abs(adjustAmount.longValue());
            if (!balance.hasSufficientBalance(absAmount)) {
                throw new IllegalStateException(
                    String.format("쿠키 잔액이 부족합니다. 차감하려는 금액: %d, 현재 잔액: %d", 
                        absAmount, balance.getAvailableAmount())
                );
            }
        }

        // 쿠키 조정
        if (adjustAmount > 0) {
            balance.addAmount(
                adjustAmount.longValue(),
                "ADMIN",
                String.format("[관리자: %s] %s", admin.getName(), reason)
            );
        } else {
            balance.subtractAmount(
                Math.abs(adjustAmount.longValue()),
                "ADMIN",
                String.format("[관리자: %s] %s", admin.getName(), reason)
            );
        }
        walletBalanceRepository.save(balance);

        // WalletLedger 기록
        WalletLedger ledger = WalletLedger.builder()
                .user(targetUser)
                .type("ADMIN_ADJUST")
                .cookieAmount(adjustAmount) // 양수면 증가, 음수면 감소
                .balanceAfter(balance.getAmount().intValue())
                .notes(String.format("[관리자 조정 by %s (%s)] %s", 
                    admin.getName(), admin.getMemberId(), reason))
                .build();
        walletLedgerRepository.save(ledger);

        log.info("관리자 쿠키 조정 완료 - targetUserId: {}, adminUserId: {}, adjustAmount: {}, reason: {}, balanceAfter: {}", 
                targetUserId, adminUserId, adjustAmount, reason, balance.getAmount());
    }
}

