package com.studyblock.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PaymentValidationRequest {
    
    private Long userId;                    // 결제하는 사용자 ID
    private List<OrderItemRequest> items;  // 주문할 상품 목록
    private Long cookieAmount;          // 쿠키 사용 금액 (선택)
    private Long totalAmount;           // 클라이언트가 계산한 총 금액
    private String paymentMethod;          // 결제 수단
    private String idempotencyKey;         // 멱등성 키
    private Long userCouponId;             // 사용자 쿠폰 ID
    
    @Getter
    @Builder
    public static class OrderItemRequest {
        private Object courseId;           // 강의 ID (Long 또는 String 허용)
        private Integer quantity;          // 수량 (일반적으로 1)
        private Long unitPrice;         // 클라이언트가 보낸 단가
        
        // courseId를 Long으로 변환하는 헬퍼 메서드
        public Long getCourseIdAsLong() {
            if (courseId == null) {
                return null;
            }
            if (courseId instanceof Long) {
                Long longValue = (Long) courseId;
                if (longValue <= 0) {
                    throw new IllegalArgumentException("courseId는 양수여야 합니다: " + longValue);
                }
                return longValue;
            }
            if (courseId instanceof String) {
                String str = (String) courseId;
                // 특별한 문자열 처리
                if ("cookie-payment".equals(str)) {
                    return 1L; // 기본 코스 ID로 매핑
                }
                try {
                    long parsed = Long.parseLong(str);
                    if (parsed <= 0) {
                        throw new IllegalArgumentException("courseId는 양수여야 합니다: " + str);
                    }
                    return parsed;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("유효하지 않은 courseId 형식입니다: " + str);
                }
            }
            throw new IllegalArgumentException("courseId must be Long or String");
        }
    }
}
