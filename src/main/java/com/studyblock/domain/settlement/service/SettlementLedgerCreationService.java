package com.studyblock.domain.settlement.service;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.settlement.entity.SettlementLedger;
import com.studyblock.domain.settlement.repository.SettlementHoldRepository;
import com.studyblock.domain.settlement.repository.SettlementLedgerRepository;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 레코드 생성 전담 서비스
 * 결제 완료 시 정산 레코드를 생성하는 역할
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementLedgerCreationService {

    private final SettlementLedgerRepository settlementLedgerRepository;
    private final SettlementHoldRepository settlementHoldRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final LectureRepository lectureRepository;

    // 상수 정의
    private static final double PLATFORM_FEE_RATE = 0.10; // 플랫폼 수수료율 10%
    private static final int REFUND_HOLD_DAYS = 7; // 환불 보류 기간 7일
    private static final double VAT_RATE = 1.1; // 부가세율 10%

    /**
     * 결제 완료 시 정산 레코드 생성
     * PaymentService에서 결제 성공 시 호출됨
     * 
     * @param orderId 주문 ID
     */
    @Transactional
    public void createSettlementLedgers(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(orderId);

        // 정산 대상 확인
        if (!hasSettlementTarget(orderItems)) {
            log.debug("정산 대상 OrderItem이 없습니다 - orderId: {}", orderId);
            return;
        }

        // 정산 보류 레코드 생성
        createSettlementHold(order, orderItems);

        // 정산 정보 레코드 생성
        int createdCount = createLedgersForOrderItems(order, orderItems);

        logCreationResult(orderId, createdCount);
    }

    /**
     * 정산 대상이 있는지 확인 (쿠키 충전 제외)
     */
    private boolean hasSettlementTarget(List<OrderItem> orderItems) {
        return orderItems.stream()
            .anyMatch(oi -> oi.getItemType() != ItemType.COOKIE_BUNDLE 
                    && (oi.getItemType() == ItemType.COURSE || oi.getItemType() == ItemType.SECTION));
    }

    /**
     * 정산 보류 레코드 생성 (환불 기간 7일)
     */
    private void createSettlementHold(Order order, List<OrderItem> orderItems) {
        OrderItem firstOrderItem = orderItems.get(0);
        
        // 이미 존재하는지 확인
        boolean holdExists = settlementHoldRepository
                .findByOrderItem_Id(firstOrderItem.getId())
                .isPresent();
        
        if (holdExists) {
            log.debug("정산 보류 레코드가 이미 존재합니다 - orderId: {}", order.getId());
            return;
        }

        // 보류 레코드 생성
        com.studyblock.domain.settlement.entity.SettlementHold hold = 
            com.studyblock.domain.settlement.entity.SettlementHold.builder()
                .orderItem(firstOrderItem)
                .user(order.getUser())
                .holdUntil(LocalDateTime.now().plusDays(REFUND_HOLD_DAYS))
                .status("HELD")
                .build();
        
        settlementHoldRepository.save(hold);
        log.info("정산 보류 레코드 생성 - orderId: {}, holdUntil: {}", order.getId(), hold.getHoldUntil());
    }

    /**
     * OrderItem 리스트에 대한 정산 레코드 생성
     * 
     * @return 생성된 레코드 수
     */
    private int createLedgersForOrderItems(Order order, List<OrderItem> orderItems) {
        int createdCount = 0;
        
        for (OrderItem orderItem : orderItems) {
            if (shouldSkipOrderItem(orderItem, order.getId())) {
                continue;
            }

            try {
                createLedgerForOrderItem(order, orderItem);
                createdCount++;
            } catch (Exception e) {
                log.error("정산 레코드 생성 실패 - orderItemId: {}, orderId: {}, error: {}", 
                        orderItem.getId(), order.getId(), e.getMessage());
            }
        }
        
        return createdCount;
    }

    /**
     * OrderItem을 건너뛸지 확인
     */
    private boolean shouldSkipOrderItem(OrderItem orderItem, Long orderId) {
        if (orderItem.getItemType() == ItemType.COOKIE_BUNDLE) {
            log.debug("쿠키 충전은 정산 대상이 아닙니다 - orderItemId: {}, orderId: {}", 
                    orderItem.getId(), orderId);
            return true;
        }
        return false;
    }

    /**
     * 단일 OrderItem에 대한 정산 레코드 생성
     */
    private void createLedgerForOrderItem(Order order, OrderItem orderItem) {
        // Course 정보 조회
        Course course = extractCourse(orderItem);
        if (course == null) {
            log.warn("Course 정보를 찾을 수 없습니다 - orderItemId: {}, itemType: {}, orderId: {}", 
                    orderItem.getId(), orderItem.getItemType(), order.getId());
            return;
        }

        // 강사 정보 조회
        User instructor = getInstructorFromCourse(course, order.getId());
        if (instructor == null) {
            return;
        }

        // 정산 금액 계산
        SettlementAmount amount = calculateSettlementAmount(orderItem.getOriginalAmount());

        // SettlementLedger 생성 및 저장
        SettlementLedger ledger = buildSettlementLedger(order, orderItem, instructor, amount);
        settlementLedgerRepository.save(ledger);

        log.info("정산 정보 레코드 생성 - instructorId: {}, orderId: {}, courseId: {}, itemType: {}, netAmount: {}, feeAmount: {}", 
                instructor.getId(), order.getId(), course.getId(), orderItem.getItemType(), 
                amount.netAmount, amount.feeAmount);
    }

    /**
     * OrderItem에서 Course 추출 (강의 구매 또는 섹션 구매)
     */
    private Course extractCourse(OrderItem orderItem) {
        if (orderItem.getItemType() == ItemType.COURSE) {
            return orderItem.getCourse();
        } else if (orderItem.getItemType() == ItemType.SECTION) {
            Section section = orderItem.getSection();
            return section != null ? section.getCourse() : null;
        }
        return null;
    }

    /**
     * Course에서 강사 정보 조회 (첫 번째 Lecture의 강사)
     */
    private User getInstructorFromCourse(Course course, Long orderId) {
        List<Lecture> lectures = lectureRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        
        if (lectures.isEmpty()) {
            log.warn("Course에 Lecture가 없습니다 - courseId: {}, orderId: {}", course.getId(), orderId);
            return null;
        }

        Lecture firstLecture = lectures.get(0);
        if (firstLecture.getInstructor() == null) {
            log.warn("Lecture에 강사 정보가 없습니다 - lectureId: {}, orderId: {}", firstLecture.getId(), orderId);
            return null;
        }

        return firstLecture.getInstructor().getUser();
    }

    /**
     * 정산 금액 계산 (공급가액, 수수료, 순수익)
     */
    private SettlementAmount calculateSettlementAmount(Long originalAmount) {
        // 부가세 제외 공급가액 계산
        long supply = Math.round(originalAmount / VAT_RATE);
        // 플랫폼 수수료 계산
        long feeAmount = Math.round(supply * PLATFORM_FEE_RATE);
        // 강사 순수익
        long netAmount = supply - feeAmount;

        return new SettlementAmount(
                Math.toIntExact(netAmount),
                Math.toIntExact(feeAmount)
        );
    }

    /**
     * SettlementLedger 엔티티 생성
     */
    private SettlementLedger buildSettlementLedger(Order order, OrderItem orderItem, 
                                                    User instructor, SettlementAmount amount) {
        return SettlementLedger.builder()
                .instructor(instructor)
                .order(order)
                .orderItem(orderItem)
                .netAmount(amount.netAmount)
                .feeAmount(amount.feeAmount)
                .rate(PLATFORM_FEE_RATE)
                .eligibleFlag(false) // 환불 기간 동안은 정산 불가
                .build();
    }

    /**
     * 정산 레코드 생성 결과 로깅
     */
    private void logCreationResult(Long orderId, int createdCount) {
        if (createdCount == 0) {
            log.warn("정산 정보 레코드가 생성되지 않았습니다 - orderId: {}", orderId);
        } else {
            log.info("정산 정보 레코드 생성 완료 - orderId: {}, 생성된 레코드 수: {}", orderId, createdCount);
        }
    }

    /**
     * Order 조회 (없으면 예외 발생)
     */
    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
    }

    /**
     * 정산 금액 정보를 담는 내부 클래스
     */
    private static class SettlementAmount {
        final Integer netAmount;  // 강사 순수익
        final Integer feeAmount;  // 플랫폼 수수료

        SettlementAmount(Integer netAmount, Integer feeAmount) {
            this.netAmount = netAmount;
            this.feeAmount = feeAmount;
        }
    }
}

