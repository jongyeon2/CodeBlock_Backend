package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSectionsRequest {

    @NotEmpty(message = "섹션 ID 목록은 필수입니다")
    private List<Long> orderedSectionIds;
}