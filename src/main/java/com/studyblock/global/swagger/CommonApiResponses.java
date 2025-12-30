package com.studyblock.global.swagger;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 공통 에러 응답을 위한 메타 어노테이션
 * 400, 500 에러는 대부분의 API에서 공통으로 사용되므로 반복을 줄이기 위해 사용
 */

@Target(ElementType.METHOD) // 이 어노테이션은 메서드 단위에만 적용할 수 있음
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 어노테이션 정보를 유지함
@ApiResponses({ // Swagger 문서에서 여러 응답 코드를 한번에 정의한다.
        @ApiResponse(responseCode = "400", description = "잘못된 요청"), // 400 응답 : 잘못된 요청
        @ApiResponse(responseCode = "500", description = "서버 오류") // 500 응답 : 서버 내부 오류
})
// 공통 응답을 정의하는 커스텀 어노테이션
public @interface CommonApiResponses {
}