package com.studyblock.domain.payment.service;

import com.studyblock.domain.payment.entity.DailyLimitAggregate;
import com.studyblock.domain.payment.repository.DailyLimitAggregateRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLimitService {

    private final DailyLimitAggregateRepository dailyLimitAggregateRepository;
    private final UserRepository userRepository;
    //결제 성공 시 일일 사용량 증가
    // userId 사용자 ID
    // cashAmount 현금 결제 금액 (null 가능)
    // cookieAmount 쿠키 결제 금액 (null 가능)
    @Transactional
    public void updateDailyLimit(Long userId, Integer cashAmount, Integer cookieAmount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        LocalDate today = LocalDate.now();

        // 오늘 날짜의 DailyLimitAggregate 조회 또는 생성
        DailyLimitAggregate aggregate = dailyLimitAggregateRepository
            .findByUser_IdAndDate(userId, today)
            .orElseGet(() -> {
                // 없으면 새로 생성
                DailyLimitAggregate newAggregate = DailyLimitAggregate.builder()
                    .user(user)
                    .date(today)
                    .cashSum(0)
                    .cookieSum(0)
                    .build();
                return dailyLimitAggregateRepository.save(newAggregate);
            });

        // 현금 사용량 추가
        if (cashAmount != null && cashAmount > 0) {
            aggregate.addCashAmount(cashAmount);
            log.info("일일 현금 사용량 증가 - userId: {}, date: {}, amount: {}, newTotal: {}", 
                    userId, today, cashAmount, aggregate.getCashSum());
        }

        // 쿠키 사용량 추가
        if (cookieAmount != null && cookieAmount > 0) {
            aggregate.addCookieAmount(cookieAmount);
            log.info("일일 쿠키 사용량 증가 - userId: {}, date: {}, amount: {}, newTotal: {}", 
                    userId, today, cookieAmount, aggregate.getCookieSum());
        }

        dailyLimitAggregateRepository.save(aggregate);
    }

    //환불 시 일일 사용량 감소
    // @param userId 사용자 ID
    // @param cashAmount 현금 환불 금액 (null 가능)
    // cookieAmount 쿠키 환불 금액 (null 가능)
    // paymentDate 원래 결제한 날짜 (해당 날짜의 사용량을 차감)

    @Transactional
    public void refundDailyLimit(Long userId, Integer cashAmount, Integer cookieAmount, LocalDate paymentDate) {
        // 원래 결제했던 날짜의 DailyLimitAggregate 조회
        Optional<DailyLimitAggregate> optionalAggregate = 
            dailyLimitAggregateRepository.findByUser_IdAndDate(userId, paymentDate);

        if (optionalAggregate.isEmpty()) {
            log.warn("환불 시 일일 사용량 기록을 찾을 수 없습니다 - userId: {}, date: {}", userId, paymentDate);
            return; // 기록이 없으면 그냥 종료 (이미 삭제되었거나 오래된 데이터)
        }

        DailyLimitAggregate aggregate = optionalAggregate.get();

        // 현금 사용량 차감
        if (cashAmount != null && cashAmount > 0) {
            aggregate.subtractCashAmount(cashAmount);
            log.info("일일 현금 사용량 감소 - userId: {}, date: {}, amount: {}, newTotal: {}", 
                    userId, paymentDate, cashAmount, aggregate.getCashSum());
        }

        // 쿠키 사용량 차감
        if (cookieAmount != null && cookieAmount > 0) {
            aggregate.subtractCookieAmount(cookieAmount);
            log.info("일일 쿠키 사용량 감소 - userId: {}, date: {}, amount: {}, newTotal: {}", 
                    userId, paymentDate, cookieAmount, aggregate.getCookieSum());
        }

        dailyLimitAggregateRepository.save(aggregate);
    }

    //사용자의 오늘 일일 사용량 조회
    // userId 사용자 ID
    // @return DailyLimitAggregate (없으면 null)
    @Transactional(readOnly = true)
    public DailyLimitAggregate getTodayLimit(Long userId) {
        LocalDate today = LocalDate.now();
        return dailyLimitAggregateRepository
            .findByUser_IdAndDate(userId, today)
            .orElse(null);
    }

    //사용자의 특정 날짜 일일 사용량 조회
    // userId 사용자 ID
    // date 조회할 날짜
    //@return DailyLimitAggregate (없으면 null)
    @Transactional(readOnly = true)
    public DailyLimitAggregate getDailyLimit(Long userId, LocalDate date) {
        return dailyLimitAggregateRepository
            .findByUser_IdAndDate(userId, date)
            .orElse(null);
    }

    //사용자의 오늘 남은 현금 한도 조회
    // userId 사용자 ID
    // dailyCashLimit 일일 현금 한도
    //@return 남은 금액
    @Transactional(readOnly = true)
    public Integer getRemainingCashLimit(Long userId, Integer dailyCashLimit) {
        DailyLimitAggregate aggregate = getTodayLimit(userId);
        if (aggregate == null) {
            return dailyCashLimit; // 오늘 첫 결제면 전액 가능
        }
        return Math.max(0, dailyCashLimit - aggregate.getCashSum());
    }

    //사용자의 오늘 남은 쿠키 한도 조회
    // userId 사용자 ID
    // dailyCookieLimit 일일 쿠키 한도
    //@return 남은 쿠키
    @Transactional(readOnly = true)
    public Integer getRemainingCookieLimit(Long userId, Integer dailyCookieLimit) {
        DailyLimitAggregate aggregate = getTodayLimit(userId);
        if (aggregate == null) {
            return dailyCookieLimit; // 오늘 첫 사용이면 전액 가능
        }
        return Math.max(0, dailyCookieLimit - aggregate.getCookieSum());
    }
}

