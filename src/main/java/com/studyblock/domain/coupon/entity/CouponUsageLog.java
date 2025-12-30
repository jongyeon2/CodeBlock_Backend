package com.studyblock.domain.coupon.entity;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_usage_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id", nullable = false)
    private UserCoupon userCoupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt = LocalDateTime.now();

    @Builder
    public CouponUsageLog(UserCoupon userCoupon,
                          Order order,
                          OrderItem orderItem,
                          Integer discountAmount,
                          LocalDateTime usedAt) {
        this.userCoupon = userCoupon;
        this.order = order;
        this.orderItem = orderItem;
        this.discountAmount = discountAmount;
        if (usedAt != null) {
            this.usedAt = usedAt;
        }
    }
}


