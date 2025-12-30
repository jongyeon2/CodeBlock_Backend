package com.studyblock.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Builder
    public Permission(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    // Business methods
    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }
}