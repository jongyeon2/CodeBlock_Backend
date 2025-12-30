package com.studyblock.domain.settlement.dto;

import com.studyblock.domain.settlement.entity.SettlementLedger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementLedgerResponse {
    private Long id;
    private Long instructorId;
    private String instructorName;
    private Long orderId;
    private String orderNumber;
    private String buyerName;  // 주문자 이름 (마스킹)
    private String buyerEmail; // 주문자 이메일 (마스킹)
    private Long orderItemId;
    private Long courseId;
    private String courseName;
    private Long sectionId;
    private String sectionName;
    private String itemType; // COURSE, SECTION, COOKIE_BUNDLE
    private Integer netAmount;
    private Integer feeAmount;
    private Integer totalAmount;
    private Double rate;
    private Boolean eligibleFlag;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
    private String status; // PENDING, ELIGIBLE, SETTLED

    public static SettlementLedgerResponse from(SettlementLedger ledger) {
        String status = determineStatus(ledger);

        // OrderItem에서 강의 정보 추출
        Long courseId = null;
        String courseName = null;
        Long sectionId = null;
        String sectionName = null;
        String itemType = null;

        if (ledger.getOrderItem() != null) {
            itemType = ledger.getOrderItem().getItemType().name();

            // Course 정보
            if (ledger.getOrderItem().getCourse() != null) {
                courseId = ledger.getOrderItem().getCourse().getId();
                courseName = ledger.getOrderItem().getCourse().getTitle();
            }

            // Section 정보 (섹션 구매인 경우)
            if (ledger.getOrderItem().getSection() != null) {
                sectionId = ledger.getOrderItem().getSection().getId();
                sectionName = ledger.getOrderItem().getSection().getTitle();

                // 섹션 구매인 경우 Course 정보도 Section에서 가져오기
                if (ledger.getOrderItem().getSection().getCourse() != null) {
                    courseId = ledger.getOrderItem().getSection().getCourse().getId();
                    courseName = ledger.getOrderItem().getSection().getCourse().getTitle();
                }
            }
        }

        // 주문자 정보 추출 및 마스킹
        String buyerName = null;
        String buyerEmail = null;
        if (ledger.getOrder().getUser() != null) {
            buyerName = maskName(ledger.getOrder().getUser().getName());
            buyerEmail = maskEmail(ledger.getOrder().getUser().getEmail());
        }

        return SettlementLedgerResponse.builder()
                .id(ledger.getId())
                .instructorId(ledger.getInstructor().getId())
                .instructorName(ledger.getInstructor().getName())
                .orderId(ledger.getOrder().getId())
                .orderNumber(ledger.getOrder().getOrderNumber())
                .buyerName(buyerName)
                .buyerEmail(buyerEmail)
                .orderItemId(ledger.getOrderItem() != null ? ledger.getOrderItem().getId() : null)
                .courseId(courseId)
                .courseName(courseName)
                .sectionId(sectionId)
                .sectionName(sectionName)
                .itemType(itemType)
                .netAmount(ledger.getNetAmount())
                .feeAmount(ledger.getFeeAmount())
                .totalAmount(ledger.getTotalAmount())
                .rate(ledger.getRate())
                .eligibleFlag(ledger.getEligibleFlag())
                .settledAt(ledger.getSettledAt())
                .createdAt(ledger.getCreatedAt())
                .status(status)
                .build();
    }

    // 이름 마스킹: "김철수" -> "김**"
    private static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.substring(0, 1) + "*".repeat(name.length() - 1);
    }

    // 이메일 마스킹: "test@example.com" -> "t***@example.com"
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() == 1) {
            return localPart + "***" + domainPart;
        }
        return localPart.substring(0, 1) + "***" + domainPart;
    }

    private static String determineStatus(SettlementLedger ledger) {
        if (ledger.isSettled()) {
            return "SETTLED";
        } else if (ledger.isEligible()) {
            return "ELIGIBLE";
        } else {
            return "PENDING";
        }
    }
}


