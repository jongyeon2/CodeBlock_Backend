package com.studyblock.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 API 응답 래퍼
 * @param <T> 응답 데이터 타입
 */
@Schema(description = "API 공통 응답 형식")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {

    // Swagger문서에서 이 클래스와 필드에 대한 설명을 표시하기 위해 사용
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "요청이 성공했습니다.")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, "요청이 성공했습니다.", data);
    }

    /**
     * 성공 응답 (메시지 + 데이터)
     */
    public static <T> CommonResponse<T> success(String message, T data) {
        return new CommonResponse<>(true, message, data);
    }

    /**
     * 성공 응답 (메시지만)
     */
    public static <T> CommonResponse<T> success(String message) {
        return new CommonResponse<>(true, message, null);
    }

    /**
     * 실패 응답
     */
    public static <T> CommonResponse<T> error(String message) {
        return new CommonResponse<>(false, message, null);
    }

    /**
     * 실패 응답 (데이터 포함)
     */
    public static <T> CommonResponse<T> error(String message, T data) {
        return new CommonResponse<>(false, message, data);
    }
}