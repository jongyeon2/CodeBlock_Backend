package com.studyblock.domain.settlement.enums;

//정산 지급 방법
public enum PaymentMethod {
    BANK_TRANSFER("계좌 이체"),
    //PAYPAL("페이팔"),
    //STRIPE("스트라이프"),
    OTHER("기타");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
