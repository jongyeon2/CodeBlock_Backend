package com.studyblock.domain.category.entity;

import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자-카테고리 매핑 엔티티
 * - 사용자가 관심을 가지는 카테고리를 관리
 * - 다대다 관계를 위한 중간 테이블 역할
 */
@Getter
@Entity
@Table(name = "user_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    private UserCategory(User user, Category category) {
        this.user = user;
        this.category = category;
    }

    /**
     * 사용자-카테고리 매핑 생성
     * @param user 사용자
     * @param category 카테고리
     * @return UserCategory 인스턴스
     */
    public static UserCategory of(User user, Category category) {
        return new UserCategory(user, category);
    }
}


