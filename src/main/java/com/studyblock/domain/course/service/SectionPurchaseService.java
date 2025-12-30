package com.studyblock.domain.course.service;

import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.repository.SectionRepository;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.settlement.service.SettlementService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectionPurchaseService {

    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SettlementService settlementService;
    private final LectureOwnershipService lectureOwnershipService;

    private static final int KRW_PER_COOKIE = 100;

    @Transactional
    public Map<String, Object> purchase(Long userId, Long sectionId) {
        log.info("섹션 구매 시작 - userId: {}, sectionId: {}", userId, sectionId);
        // 1) 사용자/섹션 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다"));

        // 2) 금액/쿠키 계산(섹션 가격은 cookie 단위)
        Long discountedCookiePrice = section.getDiscountedPrice();
        if (discountedCookiePrice == null) {
            throw new IllegalStateException("섹션 가격 정보가 없습니다");
        }

        // ⭐ 0원 섹션도 구매 가능 (LectureOwnership 생성 필요)
        int needCookies = discountedCookiePrice.intValue();
        log.info("섹션 구매 금액 - sectionId: {}, needCookies: {}", sectionId, needCookies);

        // 3) 잔액 검증 (0원 섹션은 생략)
        if (needCookies > 0 && !walletService.hasSufficientBalance(userId, (long) needCookies)) {
            throw new IllegalStateException("쿠키 잔액이 부족합니다. 충전 페이지로 이동해주세요");
        }

        // 4) 주문 생성 (COOKIE)
        // cookieSpent는 쿠키 개수 단위로 저장 (KRW 변환하지 않음)
        Order order = Order.builder()
                .user(user)
                .paymentType(PaymentType.COOKIE)
                .totalAmount(0L)
                .cookieSpent(needCookies)
                .orderNumber("SEC-" + System.currentTimeMillis())
                .orderType("SECTION_PURCHASE")
                .idempotencyKey(null)
                .tossOrderId(null)
                .orderName(section.getTitle())
                .customerKey(user.getEmail())
                .build();
        order.markAsPaid();
        order = orderRepository.saveAndFlush(order);

        // 5) OrderItem 생성(KRW 일관성: 최종금액=needCookies*100)
        // 섹션 구매 시에는 section만 저장하고 course는 NULL (section.getCourse()로 접근 가능)
        long finalAmountKrw = (long) needCookies * KRW_PER_COOKIE;
        OrderItem item = OrderItem.builder()
                .order(order)
                .course(null)                    // 섹션 구매 시 course는 NULL
                .section(section)                // 섹션 정보만 저장
                .itemType(com.studyblock.domain.payment.enums.ItemType.SECTION)
                .quantity(1)
                .unitPrice(finalAmountKrw)
                .coupon(null)
                .originalAmount(finalAmountKrw)
                .discountAmount(0L)
                .build();
        orderItemRepository.saveAndFlush(item);
        order.addOrderItem(item);

        // 6) 쿠키 차감 (0원 섹션은 생략)
        if (needCookies > 0) {
            walletService.deductCookies(userId, needCookies, order, null, "섹션 구매: " + section.getTitle());
        } else {
            log.info("무료 섹션 구매 - 쿠키 차감 생략 (userId: {}, sectionId: {})", userId, sectionId);
        }

        // 7) 정산/보류 생성
        settlementService.createSettlementLedgers(order.getId());

        // ⭐ 8) LectureOwnership 생성 (섹션 소유권 부여)
        LectureOwnership ownership = lectureOwnershipService.createOwnership(user, section, order);
        log.info("섹션 소유권 생성 완료 - User: {}, Section: {}, Ownership: {}",
                userId, sectionId, ownership.getId());

        // 9) 응답
        Long remain = walletService.getCookieBalance(userId);
        return Map.of(
                "orderId", order.getId(),
                "sectionId", sectionId,
                "spentCookies", needCookies,
                "remainCookies", remain,
                "purchasedAt", LocalDateTime.now(),
                "ownershipId", ownership.getId()
        );
    }
}


