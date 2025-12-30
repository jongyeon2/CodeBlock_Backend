package com.studyblock.domain.payment.dto;

import java.util.List;

import com.studyblock.domain.payment.dto.PaymentValidationRequest.OrderItemRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {

// 토스페이먼츠 필수 정보
private String paymentKey;      // 토스가 발급한 결제 키
private String orderId;         // 프론트에서 생성한 주문 번호 (UUID)
private Integer amount;         // 실제 결제 금액 (쿠폰 할인 후 금액)

// 주문 상세 정보
private List<OrderItemRequest> items; // 구매하는 강의 목록

// 쿠폰 사용 (옵션)
// userCouponId: user_coupons 테이블의 PK (사용자가 보유한 특정 쿠폰)
// 서버에서 userId와 함께 검증하여 본인의 쿠폰인지 확인
// 할인 금액은 서버에서 계산 (프론트엔드 값은 신뢰하지 않음)
private Long userCouponId;

// 쿠키 결제 사용 (옵션)
// 쿠키로 결제할 금액 (서버에서 잔액 검증 필요)
private Integer cookieAmount;
}

