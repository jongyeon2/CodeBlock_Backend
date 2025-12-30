package com.studyblock.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자료 삭제 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDeleteResponse {

    private Boolean success;
    private Long deletedId;

    /**
     * 성공 응답 생성
     * @param deletedId 삭제된 자료 ID
     * @return MaterialDeleteResponse
     */
    public static MaterialDeleteResponse success(Long deletedId) {
        return MaterialDeleteResponse.builder()
                .success(true)
                .deletedId(deletedId)
                .build();
    }
}