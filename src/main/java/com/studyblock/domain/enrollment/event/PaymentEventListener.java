package com.studyblock.domain.enrollment.event;

import com.studyblock.domain.enrollment.service.EnrollmentService;
import com.studyblock.domain.payment.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 이벤트 리스너
 * Listens for payment completion events and creates enrollments
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final EnrollmentService enrollmentService;

    /**
     * 결제 완료 시 수강신청 자동 생성
     * 결제 트랜잭션이 커밋된 후에 실행되어 데드락 방지
     * @param event 결제 완료 이벤트
     */
    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Order order = event.getOrder();

        log.info("Payment completed event received for order: {}", order.getId());

        try {
            // 수강신청 생성
            enrollmentService.createEnrollmentsFromOrder(order);

            log.info("Enrollments created successfully for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to create enrollments for order: {}", order.getId(), e);
            // 이벤트 리스너에서 예외가 발생해도 결제는 완료된 상태여야 함
            // 별도의 재시도 로직이나 수동 처리 필요
        }
    }
}
