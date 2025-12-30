package com.studyblock.domain.payment.dto;

import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.enums.OrderItemStatus;
import com.studyblock.domain.payment.enums.OrderStatus;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.refund.entity.Refund;
import com.studyblock.domain.refund.enums.RefundStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;                    // 주문 ID
    private String orderNumber;               // 주문 번호
    private OrderStatus status;               // 주문 상태
    private PaymentType paymentType;          // 결제 타입
    private Long totalAmount;                 // 총 결제 금액
    private Integer cookieSpent;              // 사용한 쿠키
    private LocalDateTime paidAt;             // 결제 일시
    private LocalDateTime createdAt;          // 주문 생성 일시
    private LocalDateTime refundableUntil;    // 환불 가능 기한
    private Boolean isRefundable;             // 환불 가능 여부
    private List<OrderItemResponse> orderItems; // 주문 항목 목록
    private List<RefundInfo> refunds;        // 환불 정보 목록
    private List<Long> refundedCourseIds;    // ✅ 환불 완료된 강의 ID 목록 (프론트엔드 간소화용)
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundInfo {
        private String type;                  // 'FULL' 또는 'PARTIAL'
        private String status;                 // 환불 상태 (PENDING, APPROVED, PROCESSED, REJECTED, FAILED)
        private List<Long> refundItemIds;     // 부분 환불 시 환불된 강의 ID 목록
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long itemId;                  // 주문 항목 ID
        private Long courseId;                // 강의 ID
        private String courseTitle;           // 강의명
        private String courseThumbnailUrl;   // 강의 썸네일
        private String itemType;              // COURSE / SECTION / COOKIE_BUNDLE
        private Long sectionId;                // 섹션 ID (쿠키로 섹션 구매 시)
        private String sectionTitle;           // 섹션명 (쿠키로 섹션 구매 시)
        private Integer quantity;             // 수량
        private Long originalAmount;          // 원래 금액
        private Long discountAmount;          // 할인 금액
        private Long finalAmount;             // 최종 금액
        private String refundStatus;          // 환불 상태 (NONE, PENDING, APPROVED, COMPLETED, REJECTED)

        // ✅ 프론트엔드 호환성: itemId를 id로도 접근 가능
        @JsonProperty("id")
        public Long getId() {
            return itemId;
        }

        // ✅ 프론트엔드 호환성: 중첩 객체 구조 지원 (course.title, course.id 등)
        @JsonProperty("course")
        public Map<String, Object> getCourse() {
            if (courseId == null) return null;
            Map<String, Object> course = new HashMap<>();
            course.put("id", courseId);
            course.put("title", courseTitle);
            course.put("thumbnailUrl", courseThumbnailUrl);
            return course;
        }

        // ✅ 프론트엔드 호환성: 중첩 객체 구조 지원 (section.title, section.id 등)
        @JsonProperty("section")
        public Map<String, Object> getSection() {
            if (sectionId == null) return null;
            Map<String, Object> section = new HashMap<>();
            section.put("id", sectionId);
            section.put("title", sectionTitle);
            return section;
        }
    }
    
    public static OrderResponse from(Order order, List<Refund> refunds) {
        boolean isRefundable = order.isRefundable();
        
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> {
                    // 강의 정보 (course 또는 section의 course)
                    Long courseId = null;
                    String courseTitle = null;
                    String courseThumbnailUrl = null;
                    
                    if (item.getSection() != null && item.getSection().getCourse() != null) {
                        // 섹션 구매인 경우 섹션의 강의 정보 사용
                        courseId = item.getSection().getCourse().getId();
                        courseTitle = item.getSection().getCourse().getTitle();
                        courseThumbnailUrl = item.getSection().getCourse().getThumbnailUrl();
                    } else if (item.getCourse() != null) {
                        // 직접 강의 구매인 경우
                        courseId = item.getCourse().getId();
                        courseTitle = item.getCourse().getTitle();
                        courseThumbnailUrl = item.getCourse().getThumbnailUrl();
                    }
                    
                    // OrderItem의 status를 refundStatus로 매핑
                    // 프론트엔드에서 COMPLETED 또는 APPROVED를 부분 환불 완료로 인식
                    OrderItemStatus itemStatus = item.getStatus();
                    String refundStatus = mapOrderItemStatusToRefundStatus(itemStatus);
                    
                    // 디버깅: 모든 OrderItem의 status 확인 (로그 레벨로 변경)
                    System.out.println("DEBUG OrderResponse - orderItemId=" + item.getId() + 
                            ", courseId=" + courseId + ", itemStatus=" + itemStatus + 
                            ", refundStatus=" + refundStatus);
                    
                    return OrderItemResponse.builder()
                            .itemId(item.getId())
                            .courseId(courseId)
                            .courseTitle(courseTitle)
                            .courseThumbnailUrl(courseThumbnailUrl)
                            .itemType(item.getItemType() != null ? item.getItemType().name() : null)
                            .sectionId(item.getSection() != null ? item.getSection().getId() : null)
                            .sectionTitle(item.getSection() != null ? item.getSection().getTitle() : null)
                            .quantity(item.getQuantity())
                            .originalAmount(item.getOriginalAmount())
                            .discountAmount(item.getDiscountAmount())
                            .finalAmount(item.getFinalAmount())
                            .refundStatus(refundStatus)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 환불 정보 목록 생성
        List<RefundInfo> refundInfos = buildRefundInfos(order, refunds);

        // ✅ 환불 완료된 강의 ID 목록 추출 (프론트엔드 간소화용)
        List<Long> refundedCourseIds = order.getOrderItems().stream()
                .filter(item -> item.getStatus() == OrderItemStatus.REFUNDED)
                .filter(item -> item.getItemType() == ItemType.COURSE)
                .map(item -> {
                    if (item.getCourse() != null) {
                        return item.getCourse().getId();
                    } else if (item.getSection() != null && item.getSection().getCourse() != null) {
                        return item.getSection().getCourse().getId();
                    }
                    return null;
                })
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentType(order.getPaymentType())
                .totalAmount(order.getTotalAmount())
                .cookieSpent(order.getCookieSpent())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .refundableUntil(order.getRefundableUntil())
                .isRefundable(isRefundable)
                .orderItems(itemResponses)
                .refunds(refundInfos)
                .refundedCourseIds(refundedCourseIds)
                .build();
    }
    
    // 환불 정보 목록 생성
    private static List<RefundInfo> buildRefundInfos(Order order, List<Refund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RefundInfo> refundInfos = new ArrayList<>();
        
        for (Refund refund : refunds) {
            // 환불 타입 결정: 전체 환불인지 부분 환불인지
            String refundType = determineRefundType(order, refund);
            
            // 환불 상태 매핑
            String refundStatus = mapRefundStatus(refund.getStatus());
            
            // 부분 환불 시 환불된 강의 ID 목록
            List<Long> refundItemIds = null;
            if ("PARTIAL".equals(refundType)) {
                refundItemIds = getRefundedCourseIds(order, refund);
            }
            
            refundInfos.add(RefundInfo.builder()
                    .type(refundType)
                    .status(refundStatus)
                    .refundItemIds(refundItemIds)
                    .build());
        }
        
        return refundInfos;
    }
    
    // 환불 타입 결정: 전체 환불인지 부분 환불인지
    private static String determineRefundType(Order order, Refund refund) {
        // RefundStatus가 PROCESSED인 경우에만 타입 결정
        if (refund.getStatus() != RefundStatus.PROCESSED) {
            // PROCESSED가 아니면 타입 결정 불가 (null 반환하면 안 됨)
            return "PARTIAL"; // 기본값으로 PARTIAL 반환
        }
        
        // OrderItem의 status를 확인하여 전체 환불인지 부분 환불인지 판단
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return "FULL"; // OrderItem이 없으면 전체 환불로 간주
        }
        
        // 모든 OrderItem이 REFUNDED인지 확인
        boolean allRefunded = orderItems.stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.REFUNDED);
        
        if (allRefunded) {
            return "FULL";
        } else {
            // 일부만 REFUNDED인 경우 부분 환불
            return "PARTIAL";
        }
    }
    
    // 환불 상태 매핑
    private static String mapRefundStatus(RefundStatus status) {
        if (status == null) {
            return null;
        }
        
        switch (status) {
            case PENDING:
                return "PENDING";
            case APPROVED:
                return "APPROVED";
            case PROCESSED:
                return "PROCESSED";
            case REJECTED:
                return "REJECTED";
            case FAILED:
                return "FAILED";
            default:
                return null;
        }
    }
    
    // 부분 환불 시 환불된 강의 ID 목록
    private static List<Long> getRefundedCourseIds(Order order, Refund refund) {
        List<Long> courseIds = new ArrayList<>();
        
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return courseIds;
        }
        
        // OrderItem.status == REFUNDED인 항목들의 courseId 수집
        for (OrderItem item : orderItems) {
            if (item.getStatus() == OrderItemStatus.REFUNDED) {
                // 강의 ID 추출 (course 또는 section의 course)
                Long courseId = null;
                if (item.getSection() != null && item.getSection().getCourse() != null) {
                    courseId = item.getSection().getCourse().getId();
                } else if (item.getCourse() != null) {
                    courseId = item.getCourse().getId();
                }
                
                if (courseId != null && !courseIds.contains(courseId)) {
                    courseIds.add(courseId);
                }
            }
        }
        
        return courseIds;
    }
    
    // OrderItem의 status를 프론트엔드가 기대하는 refundStatus로 매핑
    private static String mapOrderItemStatusToRefundStatus(OrderItemStatus status) {
        if (status == null) {
            return null; // NONE (환불 없음)
        }
        
        switch (status) {
            case REFUNDED:
                // 부분 환불 완료: 프론트엔드에서 COMPLETED로 인식
                return "COMPLETED";
            case PENDING:
                // 환불 대기 중 (현재는 OrderItem에 PENDING 상태가 있지만 환불 대기 의미는 아님)
                return null; // NONE
            case PAID:
                // 결제 완료 (환불 안 됨)
                return null; // NONE
            case CANCELLED:
                // 취소됨
                return null; // NONE
            default:
                return null; // NONE
        }
    }
}

