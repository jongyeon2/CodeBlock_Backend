package com.studyblock.domain.payment.entity;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.coupon.entity.Coupon;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.enums.OrderItemStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = true)
    private Section section;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Column(name = "unit_amount", nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "original_amount", nullable = false)
    private Long originalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "final_amount", nullable = false)
    private Long finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderItemStatus status = OrderItemStatus.PENDING;

    @Builder
    public OrderItem(Order order, Course course, Section section, ItemType itemType, Integer quantity, Long unitPrice,
                     Coupon coupon, Long originalAmount, Long discountAmount) {
        this.order = order;
        this.course = course;
        this.section = section;
        this.itemType = itemType != null ? itemType : ItemType.COURSE;
        this.quantity = quantity != null ? quantity : 1;
        this.unitPrice = unitPrice != null ? unitPrice : 0L;
        this.amount = this.unitPrice * this.quantity;
        this.coupon = coupon;
        this.originalAmount = originalAmount != null ? originalAmount : this.amount;
        this.discountAmount = discountAmount != null ? discountAmount : 0L;
        this.finalAmount = this.originalAmount - this.discountAmount;
        this.status = OrderItemStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Business methods
    public void applyCoupon(Coupon coupon, Long discountAmount) {
        this.coupon = coupon;
        this.discountAmount = discountAmount;
        this.finalAmount = this.originalAmount - this.discountAmount;
    }

    public void removeCoupon() {
        this.coupon = null;
        this.discountAmount = 0L;
        this.finalAmount = this.originalAmount;
    }

    public boolean hasCoupon() {
        return this.coupon != null;
    }

    public boolean hasDiscount() {
        return this.discountAmount > 0L;
    }

    public Double getDiscountRate() {
        if (this.originalAmount == 0) {
            return 0.0;
        }
        return (double) this.discountAmount / this.originalAmount * 100;
    }

    public Long getTotalDiscountAmount() {
        return this.discountAmount * this.quantity;
    }

    public Long getTotalFinalAmount() {
        return this.finalAmount * this.quantity;
    }

    public void updateQuantity(Integer newQuantity) {
        this.quantity = newQuantity;
        this.amount = this.unitPrice * this.quantity;
        this.originalAmount = this.amount;
        this.finalAmount = this.originalAmount - this.discountAmount;
    }

    public boolean isSameCourse(Course course) {
        return this.course.getId().equals(course.getId());
    }

    // Business methods for status
    public void markAsPaid() {
        this.status = OrderItemStatus.PAID;
    }

    public void markAsRefunded() {
        this.status = OrderItemStatus.REFUNDED;
    }

    public void markAsCancelled() {
        this.status = OrderItemStatus.CANCELLED;
    }

    public boolean isPaid() {
        return this.status == OrderItemStatus.PAID;
    }

    public boolean isRefunded() {
        return this.status == OrderItemStatus.REFUNDED;
    }

    public boolean canBeRefunded() {
        return this.status == OrderItemStatus.PAID;
    }
}