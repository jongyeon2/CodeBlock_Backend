package com.studyblock.global.exception;

import com.studyblock.global.dto.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 전역 예외 처리 핸들러
 * - @RestControllerAdvice를 통해 모든 Controller의 예외를 한 곳에서 처리
 * - Controller의 try-catch 중복 제거
 * - 일관된 에러 응답 형식 제공 (CommonResponse<T>)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException 처리
     * - 잘못된 요청 파라미터, 존재하지 않는 리소스 등
     * - HTTP 400 (Bad Request) 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(e.getMessage()));
    }

    /**
     * IllegalStateException 처리
     * - 비즈니스 로직 상태 오류 (예: 인코딩 미완료 상태에서 스트리밍 요청)
     * - HTTP 400 (Bad Request) 반환
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.warn("상태 오류: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(e.getMessage()));
    }

    /**
     * MaxUploadSizeExceededException 처리
     * - 파일 업로드 크기 초과 시 발생
     * - HTTP 413 (Payload Too Large) 반환
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CommonResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("파일 크기 초과: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(CommonResponse.error("업로드 파일 크기가 너무 큽니다."));
    }

    /**
     * AsyncRequestTimeoutException 처리
     * - SSE(Server-Sent Events) 연결 타임아웃 시 발생
     * - SSE 엔드포인트의 경우 예외를 무시 (타임아웃은 정상적인 동작)
     * - 다른 엔드포인트의 경우 일반적인 타임아웃 오류로 처리
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<CommonResponse<Void>> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException e, 
            HttpServletRequest request) {
        
        String requestPath = request.getRequestURI();
        
        // SSE 엔드포인트인 경우 타임아웃은 정상적인 동작 (로그만 기록)
        if (requestPath != null && requestPath.contains("/encoding-status/stream")) {
            log.debug("SSE 연결 타임아웃 (정상 동작) - Path: {}", requestPath);
            // SSE 엔드포인트에서는 응답을 보내지 않음 (이미 타임아웃 처리됨)
            return null; // null을 반환하면 응답을 보내지 않음
        }
        
        // 일반 엔드포인트의 타임아웃
        log.warn("요청 타임아웃: {}", requestPath);
        return ResponseEntity
                .status(HttpStatus.REQUEST_TIMEOUT)
                .body(CommonResponse.error("요청 시간이 초과되었습니다."));
    }

    /**
     * 그 외 모든 예외 처리
     * - 예상치 못한 서버 오류
     * - HTTP 500 (Internal Server Error) 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleGeneralException(
            Exception e, 
            HttpServletRequest request) {
        
        // SSE 엔드포인트인 경우에도 일반 예외는 처리하지 않음
        String requestPath = request.getRequestURI();
        if (requestPath != null && requestPath.contains("/encoding-status/stream")) {
            // SSE 엔드포인트에서는 예외를 무시 (이미 타임아웃이나 다른 방식으로 처리됨)
            log.debug("SSE 엔드포인트 예외 무시 - Path: {}, Exception: {}", requestPath, e.getClass().getSimpleName());
            return null;
        }
        
        log.error("서버 오류 발생", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.error("서버 내부 오류가 발생했습니다."));
    }

    /**
     * VideoEncodingNotCompletedException 처리
     * - 인코딩이 완료되지 않은 비디오에 대한 스트리밍 URL 요청 시 발생
     * - HTTP 409 (Conflict) 반환
     */
    @ExceptionHandler(com.studyblock.domain.course.exception.VideoEncodingNotCompletedException.class)
    public ResponseEntity<CommonResponse<Void>> handleVideoEncodingNotCompletedException(
            com.studyblock.domain.course.exception.VideoEncodingNotCompletedException e) {
        log.warn("인코딩 미완료 비디오 스트리밍 시도: Video ID={}, 현재 상태={}",
                e.getVideoId(), e.getCurrentStatus());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommonResponse.error(e.getMessage()));
    }

    /**
     * InvalidResolutionException 처리
     * - 유효하지 않은 해상도가 요청되었을 때 발생
     * - HTTP 400 (Bad Request) 반환
     */
    @ExceptionHandler(com.studyblock.domain.course.exception.InvalidResolutionException.class)
    public ResponseEntity<CommonResponse<Void>> handleInvalidResolutionException(
            com.studyblock.domain.course.exception.InvalidResolutionException e) {
        log.warn("유효하지 않은 해상도 요청: {}", e.getProvidedResolution());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(e.getMessage()));
    }
}