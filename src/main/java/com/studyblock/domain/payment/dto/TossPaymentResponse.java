package com.studyblock.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 토스페이먼츠 승인 API 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPaymentResponse {
    
    private String paymentKey;          // 결제 키
    private String orderId;             // 주문 번호
    private String orderName;           // 주문명
    private String status;              // 결제 상태 (DONE, CANCELED 등)
    
    private Integer totalAmount;        // 총 결제 금액
    private Integer balanceAmount;      // 취소 가능 잔액
    private Integer suppliedAmount;     // 공급가액
    private Integer vat;                // 부가세
    private Integer taxFreeAmount;      // 면세금액
    
    private String method;              // 결제 수단 (카드, 가상계좌 등)
    private String currency;            // 통화 (KRW)
    private String version;             // API 버전
    @JsonProperty("mId")
    private String mId;                 // 상점아이디(MID)
    private String type;                // 결제 타입 (NORMAL, BILLING, BRANDPAY)
    private String country;             // 국가 코드
    private String lastTransactionKey;  // 마지막 트랜잭션 키
    
    private String requestedAt;  // 결제 요청 시간(문자열)
    private String approvedAt;   // 결제 승인 시간(문자열)
    
    // 영수증 정보 (선택적)
    private Receipt receipt;
    // 체크아웃 정보 (선택적)
    private Checkout checkout;

    @Getter
    @NoArgsConstructor
    public static class Receipt {
        private String url;             // 영수증 URL
    }

    @Getter
    @NoArgsConstructor
    public static class Checkout {
        private String url;             // 체크아웃 URL
    }

    // 카드 결제 정보 (선택적)
    private Card card;
    
    @Getter
    @NoArgsConstructor
    public static class Card {
        private String company;         // 카드사 텍스트(옵션)
        private String issuerCode;      // 발급사 코드
        private String acquirerCode;    // 매입사 코드
        private String number;          // 카드번호 (마스킹)
        private Integer installmentPlanMonths; // 할부 개월 수
        private Boolean isInterestFree; // 무이자 여부
        private String interestPayer;   // 이자 부담 주체
        private String approveNo;       // 승인 번호
        private Boolean useCardPoint;   // 카드 포인트 사용 여부
        private String cardType;        // 카드 타입 (신용/체크)
        private String ownerType;       // 소유자 타입 (개인/법인)
        private String acquireStatus;   // 매입 상태
        private Integer amount;         // 카드 결제 금액
    }
    
    // 가상계좌 정보 (선택적)
    private VirtualAccount virtualAccount;
    
    @Getter
    @NoArgsConstructor
    public static class VirtualAccount {
        private String accountNumber;   // 가상계좌 번호
        private String bank;            // 은행
        private String customerName;    // 입금자명
        private String dueDate;  // 입금 기한(문자열)
    }

    // 계좌이체 정보 (선택적)
    private Transfer transfer;

    @Getter
    @NoArgsConstructor
    public static class Transfer {
        private String bank;           // 은행명
        private String settlementStatus; // 정산 상태
    }

    // 간편결제 정보 (선택적) - 토스페이, 네이버페이, 카카오페이 등
    private EasyPay easyPay;

    @Getter
    @NoArgsConstructor
    public static class EasyPay {
        private String provider;       // 제공업체 (TOSS, NAVER, KAKAO)
        private String method;         // 결제 수단
        private Integer discountAmount; // 할인 금액
    }

    // 휴대폰 결제 정보 (선택적)
    private MobilePhone mobilePhone;

    @Getter
    @NoArgsConstructor
    public static class MobilePhone {
        private String carrier;        // 통신사 (SK, KT, LG)
        private String customerMobilePhone; // 휴대폰 번호
        private Integer discountAmount; // 할인 금액
    }
}

