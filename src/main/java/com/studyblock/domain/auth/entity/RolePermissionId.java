package com.studyblock.domain.auth.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    private Long roleId;
    private Long permissionId;
}