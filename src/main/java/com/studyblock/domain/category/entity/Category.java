package com.studyblock.domain.category.entity;

import com.studyblock.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCategory> userCategories = new ArrayList<>();

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(nullable = false)
    private Byte depth;

    @Builder
    private Category(String name, String description, Category parent, Long orderNo, Integer depth) {
        this.name = name;
        this.description = description;
        this.parent = parent;
        this.orderNo = orderNo != null ? orderNo : 0L;
        this.depth = depth != null ? depth.byteValue() : (byte) 0;
    }

    public Long getParentId() {
        return parent != null ? parent.getId() : null;
    }
}
