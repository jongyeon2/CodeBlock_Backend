package com.studyblock.domain.cart.entity;

import java.time.LocalDateTime;

import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//사용자가 장바구니에 담은 강의 정보를 저장합니다.
//같은 사용자가 같은 강의를 중복으로 담을 수 없도록 UNIQUE 제약조건 설정
@Entity
@Table(name = "cart",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"user_id", "course_id"}, name = "unique_user_course")
        },
        indexes = {
            @Index(name = "idx_user_id", columnList = "user_id"),
            @Index(name = "idx_course_id", columnList = "course_id"),
            @Index(name = "idx_user_selected", columnList = "user_id, selected")
        }
    )
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String name;  // 장바구니에 담을 당시의 강의명

    @Column(nullable = false)
    private Integer price;  // 장바구니에 담을 당시의 가격

    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;  // 원래 가격 (할인 전)

    @Column(name = "discount_percentage", nullable = false)
    private Integer discountPercentage = 0;  // 할인율 (%)

    @Column(name = "has_discount", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean hasDiscount = false;  // 할인 여부

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean selected = true;  // 선택 여부 (결제 시 사용)

    @Column(name = "added_at", nullable = false, updatable = false)
    private java.time.LocalDateTime addedAt = java.time.LocalDateTime.now();

    @Builder
    public CartItem(User user, Course course, String name, Integer price,
                    Integer originalPrice, Integer discountPercentage,
                    Boolean hasDiscount, Boolean selected, LocalDateTime addedAt) {
        this.user = user;
        this.course = course;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercentage = discountPercentage != null ? discountPercentage : 0;
        this.hasDiscount = hasDiscount != null ? hasDiscount : false;
        this.selected = selected != null ? selected : true;
        this.addedAt = java.time.LocalDateTime.now();
    }

    // Business methods
    //선택 상태 변경
    public void updateSelection(Boolean selected) {
        this.selected = selected != null ? selected : true;
    }

    //가격 정보 업데이트 (강의 가격이 변경된 경우)
    public void updatePrice(Integer price, Integer originalPrice,
                            Integer discountPercentage, Boolean hasDiscount) {
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercentage = discountPercentage != null ? discountPercentage : 0;
        this.hasDiscount = hasDiscount != null ? hasDiscount : false;
    }
}

