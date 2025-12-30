package com.studyblock.domain.payment.service.support;

import com.studyblock.domain.payment.dto.TossPaymentResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TossMethodDetailsConverter {

    public static String toJson(TossPaymentResponse tossResponse) {
        if (tossResponse == null) {
            return null;
        }
        try {
            StringBuilder json = new StringBuilder("{");

            if (tossResponse.getCard() != null) {
                json.append("\"card\": {")
                    .append("\"number\": \"").append(tossResponse.getCard().getNumber()).append("\",")
                    .append("\"company\": \"").append(tossResponse.getCard().getCompany()).append("\",")
                    .append("\"installment_plan_months\": ").append(tossResponse.getCard().getInstallmentPlanMonths()).append(",")
                    .append("\"type\": \"").append(tossResponse.getCard().getCardType()).append("\",")
                    .append("\"owner_type\": \"").append(tossResponse.getCard().getOwnerType()).append("\",")
                    .append("\"approve_no\": \"").append(tossResponse.getCard().getApproveNo()).append("\",")
                    .append("\"acquire_status\": \"").append(tossResponse.getCard().getAcquireStatus()).append("\"")
                    .append("},");
            }

            if (tossResponse.getVirtualAccount() != null) {
                json.append("\"virtual_account\": {")
                    .append("\"account_number\": \"").append(tossResponse.getVirtualAccount().getAccountNumber()).append("\",")
                    .append("\"bank\": \"").append(tossResponse.getVirtualAccount().getBank()).append("\",")
                    .append("\"due_date\": \"").append(tossResponse.getVirtualAccount().getDueDate()).append("\",")
                    .append("\"holder_name\": \"").append(tossResponse.getVirtualAccount().getCustomerName()).append("\"")
                    .append("},");
            }

            if (tossResponse.getTransfer() != null) {
                json.append("\"transfer\": {")
                    .append("\"bank\": \"").append(tossResponse.getTransfer().getBank()).append("\",")
                    .append("\"settlement_status\": \"").append(tossResponse.getTransfer().getSettlementStatus()).append("\"")
                    .append("},");
            }

            if (tossResponse.getEasyPay() != null) {
                json.append("\"easy_pay\": {")
                    .append("\"provider\": \"").append(tossResponse.getEasyPay().getProvider()).append("\",")
                    .append("\"method\": \"").append(tossResponse.getEasyPay().getMethod()).append("\",")
                    .append("\"discount_amount\": ").append(tossResponse.getEasyPay().getDiscountAmount())
                    .append("},");
            }

            if (tossResponse.getMobilePhone() != null) {
                json.append("\"mobile_phone\": {")
                    .append("\"carrier\": \"").append(tossResponse.getMobilePhone().getCarrier()).append("\",")
                    .append("\"phone_number\": \"").append(tossResponse.getMobilePhone().getCustomerMobilePhone()).append("\",")
                    .append("\"discount_amount\": ").append(tossResponse.getMobilePhone().getDiscountAmount())
                    .append("},");
            }

            if (json.length() > 1 && json.charAt(json.length() - 1) == ',') {
                json.setLength(json.length() - 1);
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            log.error("Toss method details JSON 변환 실패: {}", e.getMessage());
            return null;
        }
    }
}


