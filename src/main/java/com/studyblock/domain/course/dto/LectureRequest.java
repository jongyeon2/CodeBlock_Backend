package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.enums.LectureStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강의 생성/수정 요청 DTO
 * 
 * 주의:
 * - sectionId: 강의 생성 시 필수 (서비스 레이어에서 검증), 수정 시에는 무시됨 (섹션 변경 불가)
 *   - 생성 API: sectionId 필수
 *   - 수정 API: sectionId 무시 (null 허용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureRequest {

    private Long sectionId;  // 강의 생성 시 필수 (서비스 레이어에서 검증), 수정 시에는 무시

    @NotBlank(message = "강의 제목은 필수입니다")
    @Size(max = 255, message = "강의 제목은 255자를 초과할 수 없습니다")
    private String title;

    private String description;

    @NotNull(message = "순서는 필수입니다")
    @Min(value = 1, message = "순서는 1 이상이어야 합니다")
    private Integer sequence;

    @NotNull(message = "강의 상태는 필수입니다")
    private LectureStatus status;
}