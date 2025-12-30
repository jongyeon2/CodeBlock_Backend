package com.studyblock.domain.wallet.service;

import com.studyblock.domain.payment.repository.CookieBatchRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.wallet.dto.CookieAdminStatsResponse;
import com.studyblock.domain.wallet.dto.CookieAdminUsageResponse;
import com.studyblock.domain.wallet.entity.WalletLedger;
import com.studyblock.domain.wallet.repository.WalletBalanceRepository;
import com.studyblock.domain.wallet.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 쿠키 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieAdminService {

    private final WalletBalanceRepository walletBalanceRepository;
    private final CookieBatchRepository cookieBatchRepository;
    private final WalletLedgerRepository walletLedgerRepository;

    private static final String CURRENCY_KRW = "KRW";

    /**
     * 전체 사용자 쿠키 잔액 통계 조회
     */
    @Transactional(readOnly = true)
    public CookieAdminStatsResponse getCookieStats(Integer year, Integer month) {
        // 현재 년월 설정 (파라미터가 없으면 현재 년월)
        LocalDateTime now = LocalDateTime.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        // 전체 쿠키 잔액 합계 (cookie_batch.qty_remain 합계)
        Long totalBalance = cookieBatchRepository.sumQtyRemainTotal(now);
        if (totalBalance == null) {
            totalBalance = 0L;
        }

        // 유료 쿠키 잔액
        Long totalBalancePaid = cookieBatchRepository.sumQtyRemainPaid(now);
        if (totalBalancePaid == null) {
            totalBalancePaid = 0L;
        }

        // 무료 쿠키 잔액
        Long totalBalanceFree = cookieBatchRepository.sumQtyRemainFree(now);
        if (totalBalanceFree == null) {
            totalBalanceFree = 0L;
        }

        // 만료된 무료 쿠키 잔액
        Long totalBalanceExpiredFree = cookieBatchRepository.sumQtyRemainExpiredFree(now);
        if (totalBalanceExpiredFree == null) {
            totalBalanceExpiredFree = 0L;
        }

        // 년도별 충전량 계산 (전체 기간)
        LocalDateTime yearStart = LocalDateTime.of(targetYear, 1, 1, 0, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(targetYear, 12, 31, 23, 59, 59);

        // 년도별 유료 쿠키 충전량
        Long totalChargedPaid = cookieBatchRepository.sumQtyTotalPaidByPurchaseAndDateRange(yearStart, yearEnd);
        if (totalChargedPaid == null) {
            totalChargedPaid = 0L;
        }

        // 년도별 무료 쿠키 충전량
        Long totalChargedFree = cookieBatchRepository.sumQtyTotalFreeByPurchaseAndBonusAndDateRange(yearStart, yearEnd);
        if (totalChargedFree == null) {
            totalChargedFree = 0L;
        }

        // 년도별 전체 충전량
        Long totalCharged = totalChargedPaid + totalChargedFree;

        // 이번 달 시작일/종료일 계산
        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 이번 달 충전 (유료 + 무료 쿠키)
        // 유료 쿠키 충전량
        Long monthlyChargedPaid = cookieBatchRepository.sumQtyTotalPaidByPurchaseAndDateRange(monthStart, monthEnd);
        if (monthlyChargedPaid == null) {
            monthlyChargedPaid = 0L;
        }
        
        // 무료 쿠키 충전량
        Long monthlyChargedFree = cookieBatchRepository.sumQtyTotalFreeByPurchaseAndBonusAndDateRange(monthStart, monthEnd);
        if (monthlyChargedFree == null) {
            monthlyChargedFree = 0L;
        }
        
        // 전체 충전량 (유료 + 무료)
        Long monthlyCharged = monthlyChargedPaid + monthlyChargedFree;

        // 이번 달 사용 (wallet_ledger에서 이번 달 생성되고 type이 DEBIT인 것들의 cookie_amount 합계)
        Long monthlyUsed = walletLedgerRepository.sumDebitedCookiesByDateRange(monthStart, monthEnd);
        if (monthlyUsed == null) {
            monthlyUsed = 0L;
        }
        // DEBIT는 음수로 저장되므로 절댓값으로 변환
        monthlyUsed = Math.abs(monthlyUsed);

        return CookieAdminStatsResponse.builder()
                .totalBalance(totalBalance)
                .totalBalancePaid(totalBalancePaid)
                .totalBalanceFree(totalBalanceFree)
                .totalBalanceExpiredFree(totalBalanceExpiredFree)
                .totalCharged(totalCharged)
                .totalChargedPaid(totalChargedPaid)
                .totalChargedFree(totalChargedFree)
                .monthlyCharged(monthlyCharged)
                .monthlyChargedPaid(monthlyChargedPaid)
                .monthlyChargedFree(monthlyChargedFree)
                .monthlyUsed(monthlyUsed)
                .year(targetYear)
                .month(targetMonth)
                .build();
    }


    /**
     * 쿠키 사용 내역 조회 (필터링 포함)
     */
    @Transactional(readOnly = true)
    public List<CookieAdminUsageResponse> getCookieUsageHistory(Long userId, String type,
                                                                 LocalDateTime startDate, LocalDateTime endDate,
                                                                 Pageable pageable) {
        // WalletLedger 조회 (필터링)
        List<WalletLedger> ledgers;
        
        if (userId != null && type != null && startDate != null && endDate != null) {
            // 모든 필터 적용
            if ("DEBIT".equals(type)) {
                ledgers = walletLedgerRepository.findByUser_IdAndType(userId, type);
                ledgers = ledgers.stream()
                        .filter(ledger -> ledger.getCreatedAt().isAfter(startDate.minusSeconds(1)) &&
                                         ledger.getCreatedAt().isBefore(endDate.plusSeconds(1)))
                        .collect(Collectors.toList());
            } else {
                ledgers = walletLedgerRepository.findByUser_IdAndType(userId, type);
                ledgers = ledgers.stream()
                        .filter(ledger -> ledger.getCreatedAt().isAfter(startDate.minusSeconds(1)) &&
                                         ledger.getCreatedAt().isBefore(endDate.plusSeconds(1)))
                        .collect(Collectors.toList());
            }
        } else if (userId != null && type != null) {
            // userId + type 필터
            ledgers = walletLedgerRepository.findByUser_IdAndType(userId, type);
        } else if (userId != null && startDate != null && endDate != null) {
            // userId + 날짜 필터
            ledgers = walletLedgerRepository.findByUser_IdAndCreatedAtBetween(userId, startDate, endDate);
        } else if (type != null && startDate != null && endDate != null) {
            // type + 날짜 필터
            ledgers = walletLedgerRepository.findByTypeAndCreatedAtBetween(type, startDate, endDate);
        } else if (userId != null) {
            // userId만 필터
            ledgers = walletLedgerRepository.findByUser_Id(userId);
        } else if (type != null) {
            // type만 필터
            ledgers = walletLedgerRepository.findByType(type);
        } else if (startDate != null && endDate != null) {
            // 날짜만 필터
            ledgers = walletLedgerRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            // 필터 없음 (전체 조회)
            ledgers = walletLedgerRepository.findAll();
        }

        // 최신순 정렬 (type 필터링은 이미 위에서 처리됨)
        ledgers = ledgers.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 최신순 정렬
                .collect(Collectors.toList());

        // 페이징 처리
        int fromIndex = (int) pageable.getOffset();
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), ledgers.size());
        if (fromIndex >= ledgers.size()) {
            ledgers = List.of();
        } else {
            ledgers = ledgers.subList(fromIndex, toIndex);
        }

        // DTO 변환
        return ledgers.stream()
                .map(this::toCookieAdminUsageResponse)
                .collect(Collectors.toList());
    }

    /**
     * WalletLedger를 CookieAdminUsageResponse로 변환
     */
    private CookieAdminUsageResponse toCookieAdminUsageResponse(WalletLedger ledger) {
        User user = ledger.getUser();
        
        return CookieAdminUsageResponse.builder()
                .id(ledger.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : null)
                .type(ledger.getType())
                .cookieAmount(ledger.getCookieAmount())
                .balanceAfter(ledger.getBalanceAfter())
                .notes(ledger.getNotes())
                .referenceType(ledger.getReferenceType())
                .referenceId(ledger.getReferenceId())
                .createdAt(ledger.getCreatedAt())
                .build();
    }

    /**
     * 쿠키 사용 내역 개수 조회
     */
    @Transactional(readOnly = true)
    public Long countCookieUsageHistory(Long userId, String type, 
                                         LocalDateTime startDate, LocalDateTime endDate) {
        List<WalletLedger> ledgers;
        
        if (userId != null && type != null && startDate != null && endDate != null) {
            ledgers = walletLedgerRepository.findByUser_IdAndType(userId, type);
            ledgers = ledgers.stream()
                    .filter(ledger -> ledger.getCreatedAt().isAfter(startDate.minusSeconds(1)) &&
                                     ledger.getCreatedAt().isBefore(endDate.plusSeconds(1)))
                    .collect(Collectors.toList());
        } else if (userId != null && type != null) {
            ledgers = walletLedgerRepository.findByUser_IdAndType(userId, type);
        } else if (userId != null && startDate != null && endDate != null) {
            ledgers = walletLedgerRepository.findByUser_IdAndCreatedAtBetween(userId, startDate, endDate);
        } else if (type != null && startDate != null && endDate != null) {
            ledgers = walletLedgerRepository.findByTypeAndCreatedAtBetween(type, startDate, endDate);
        } else if (userId != null) {
            ledgers = walletLedgerRepository.findByUser_Id(userId);
        } else if (type != null) {
            ledgers = walletLedgerRepository.findByType(type);
        } else if (startDate != null && endDate != null) {
            ledgers = walletLedgerRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            ledgers = walletLedgerRepository.findAll();
        }

        // 개수 반환 (type 필터링은 이미 위에서 처리됨)
        return (long) ledgers.size();
    }
}

