package com.studyblock.domain.payment.service.validator;

import com.studyblock.domain.payment.entity.DailyLimitAggregate;
import com.studyblock.domain.payment.repository.DailyLimitAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

// 일일 한도 검증 전담 서비스
// 단일 책임: 일일 결제 한도 검증
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLimitValidationService {

    private final DailyLimitAggregateRepository dailyLimitAggregateRepository;

    // 일일 한도 체크 (현금 결제 시)
    public void validateDailyLimit(Long userId, Long cashAmount, Long cookieAmount) {
        // 현금도 쿠키도 없으면 체크 안 함
        if ((cashAmount == null || cashAmount <= 0L) &&
            (cookieAmount == null || cookieAmount <= 0L)) {
            return;
        }

        LocalDate today = LocalDate.now();

        // 오늘 사용량 조회 (없으면 생성 필요하지만, 여기서는 조회만)
        Optional<DailyLimitAggregate> optionalAggregate =
                dailyLimitAggregateRepository.findByUser_IdAndDate(userId, today);

        // 오늘 첫 결제면 한도 체크 필요 없음
        if (optionalAggregate.isEmpty()) {
            return; // 첫 결제는 통과
        }

        DailyLimitAggregate aggregate = optionalAggregate.get();

        // 현금 한도 체크 (예: 일일 100만원)
        int dailyCashLimit = 1000000; // 100만원
        if (cashAmount != null && cashAmount > 0) {
            int newCashSum = aggregate.getCashSum() + cashAmount.intValue();
            if (newCashSum > dailyCashLimit) {
                throw new IllegalStateException(
                    String.format("일일 현금 결제 한도를 초과합니다. 한도: %d원, 현재: %d원, 요청: %d원",
                        dailyCashLimit, aggregate.getCashSum(), cashAmount)
                );
            }
        }

        // 쿠키 한도 체크 (예: 일일 10만 쿠키)
        int dailyCookieLimit = 100000; // 10만 쿠키
        if (cookieAmount != null && cookieAmount > 0) {
            int newCookieSum = aggregate.getCookieSum() + cookieAmount.intValue();
            if (newCookieSum > dailyCookieLimit) {
                throw new IllegalStateException(
                    String.format("일일 쿠키 사용 한도를 초과합니다. 한도: %d, 현재: %d, 요청: %d",
                        dailyCookieLimit, aggregate.getCookieSum(), cookieAmount)
                );
            }
        }
    }
}

