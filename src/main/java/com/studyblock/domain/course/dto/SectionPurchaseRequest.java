package com.studyblock.domain.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionPurchaseRequest {
    
    @NotNull(message = "섹션 ID는 필수입니다")
    private Long sectionId;
}
