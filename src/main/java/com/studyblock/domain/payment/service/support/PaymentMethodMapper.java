package com.studyblock.domain.payment.service.support;

import com.studyblock.domain.payment.enums.PaymentMethod;

public class PaymentMethodMapper {

    public static PaymentMethod fromTossMethod(String tossMethod) {
        if (tossMethod == null) {
            return PaymentMethod.CARD;
        }
        return switch (tossMethod.toUpperCase()) {
            case "카드", "CARD" -> PaymentMethod.CARD;
            case "가상계좌", "VIRTUAL_ACCOUNT" -> PaymentMethod.VIRTUAL_ACCOUNT;
            case "계좌이체", "TRANSFER" -> PaymentMethod.TRANSFER;
            case "휴대폰", "MOBILE_PHONE", "MOBILE" -> PaymentMethod.MOBILE;
            case "간편결제", "EASY_PAY" -> PaymentMethod.EASY_PAY;
            case "상품권", "GIFT_CERTIFICATE" -> PaymentMethod.GIFT_CERTIFICATE;
            default -> PaymentMethod.CARD;
        };
    }
}


