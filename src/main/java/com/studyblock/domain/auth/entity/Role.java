package com.studyblock.domain.auth.entity;

import com.studyblock.domain.auth.enums.RoleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleCode code;

    @Column(nullable = false, length = 50)
    private String name;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @Builder
    public Role(RoleCode code, String name) {
        this.code = code;
        this.name = name;
    }

    // Business methods
    public void addPermission(Permission permission) {
        RolePermission rolePermission = RolePermission.builder()
                .role(this)
                .permission(permission)
                .build();
        this.rolePermissions.add(rolePermission);
    }

    public void removePermission(Permission permission) {
        this.rolePermissions.removeIf(rp -> rp.getPermission().equals(permission));
    }

    public boolean hasPermission(String permissionCode) {
        return rolePermissions.stream()
                .anyMatch(rp -> rp.getPermission().getCode().equals(permissionCode));
    }
}