package com.studyblock.domain.enrollment.event;

import com.studyblock.domain.payment.entity.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 결제 완료 이벤트
 * Payment completion event for triggering enrollment creation
 */
@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final Order order;

    public PaymentCompletedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}
