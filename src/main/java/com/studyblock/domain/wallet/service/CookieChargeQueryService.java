package com.studyblock.domain.wallet.service;

import com.studyblock.domain.payment.entity.CookieBundle;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.repository.CookieBundleRepository;
import com.studyblock.domain.payment.repository.CookieBatchRepository;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import com.studyblock.domain.payment.repository.OrderRepository;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.domain.wallet.dto.CookieChargeHistoryPageResponse;
import com.studyblock.domain.wallet.dto.CookieChargeHistoryResponse;
import com.studyblock.domain.wallet.dto.CookieChargeGroupedResponse;
import com.studyblock.domain.wallet.dto.CookieUsageHistoryResponse;
import com.studyblock.domain.wallet.entity.WalletLedger;
import com.studyblock.domain.wallet.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieChargeQueryService {

    private final CookieBundleRepository cookieBundleRepository;
    private final UserRepository userRepository;
    private final CookieBatchRepository cookieBatchRepository;
    private final WalletService walletService;
    private final WalletLedgerRepository walletLedgerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableBundles() {
        try {
            log.info("쿠키 번들 목록 조회 시작");
            
            // 활성화된 번들 조회
            List<CookieBundle> bundles = cookieBundleRepository.findByIsActiveTrueOrderByNameAsc();
            log.info("DB에서 조회된 쿠키 번들 수: {}", bundles != null ? bundles.size() : 0);
            
            if (bundles == null || bundles.isEmpty()) {
                log.warn("활성화된 쿠키 번들이 없습니다");
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("bundles", new java.util.ArrayList<>());
                result.put("totalCount", 0);
                return result;
            }
            
            List<Map<String, Object>> bundleList = bundles.stream()
                    .filter(bundle -> bundle != null && bundle.getPrice() != null) // null 필터링
                    .sorted((a, b) -> {
                        Long priceA = a.getPrice() != null ? a.getPrice() : Long.MAX_VALUE;
                        Long priceB = b.getPrice() != null ? b.getPrice() : Long.MAX_VALUE;
                        return priceA.compareTo(priceB);
                    })
                    .map(bundle -> {
                        Map<String, Object> bundleMap = new java.util.HashMap<>();
                        try {
                            bundleMap.put("id", bundle.getId());
                            bundleMap.put("name", bundle.getName() != null ? bundle.getName() : "");
                            bundleMap.put("price", bundle.getPrice() != null ? bundle.getPrice() : 0L);
                            
                            Integer baseAmount = bundle.getBaseCookieAmount() != null ? bundle.getBaseCookieAmount() : 0;
                            Integer bonusAmount = bundle.getBonusCookieAmount() != null ? bundle.getBonusCookieAmount() : 0;
                            
                            bundleMap.put("baseCookieAmount", baseAmount);
                            bundleMap.put("bonusCookieAmount", bonusAmount);
                            bundleMap.put("totalCookieAmount", baseAmount + bonusAmount);
                            
                            return bundleMap;
                        } catch (Exception e) {
                            log.error("번들 변환 중 오류 - bundleId: {}", bundle.getId(), e);
                            return null;
                        }
                    })
                    .filter(map -> map != null) // 변환 실패한 항목 제외
                    .collect(Collectors.toList());

            log.info("변환된 번들 목록 수: {}", bundleList.size());
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("bundles", bundleList);
            result.put("totalCount", bundleList.size());
            
            return result;
        } catch (Exception e) {
            log.error("쿠키 번들 목록 조회 중 오류 발생", e);
            // 빈 결과 반환 (에러 발생 시 빈 리스트 반환)
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("bundles", new java.util.ArrayList<>());
            result.put("totalCount", 0);
            result.put("error", "쿠키 번들 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return result;
        }
    }

    // readOnly 제거: WalletService.getCookieBalance()가 지갑/잔액 생성 시 INSERT를 수행하므로 쓰기 가능해야 함
    @Transactional
    public Long getCookieBalance(Long userId) {
        try {
            return walletService.getCookieBalance(userId);
        } catch (Exception e) {
            log.error("쿠키 잔액 조회 중 오류 발생 - userId: {}", userId, e);
            // 에러 발생 시 0 반환 (프론트 오류 방지)
            return 0L;
        }
    }

    @Transactional(readOnly = true)
    public List<CookieChargeHistoryResponse> getMyChargeHistory(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        var batches = cookieBatchRepository.findByUserOrderByCreatedAtDesc(user);
        
        // 빈 리스트 처리
        if (batches == null || batches.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        List<CookieChargeHistoryResponse> list = new java.util.ArrayList<>();
        for (com.studyblock.domain.payment.entity.CookieBatch b : batches) {
            if (b == null) continue; // null 체크
            
            var oi = b.getOrderItem();
            list.add(CookieChargeHistoryResponse.builder()
                    .id(b.getId())
                    .userId(userId)
                    .orderItemsId(oi != null ? oi.getId() : null)
                    .qtyTotal(b.getQtyTotal())
                    .qtyRemain(b.getQtyRemain())
                    .cookieType(b.getCookieType() != null ? b.getCookieType().name() : null)
                    .source(b.getSource() != null ? b.getSource().name() : null)
                    .expiresAt(b.getExpiresAt())
                    .isActive(b.getIsActive())
                    .createdAt(b.getCreatedAt())
                    // order_items 매핑(가용 컬럼)
                    .orderId(oi != null && oi.getOrder() != null ? oi.getOrder().getId() : null)
                    .itemType(oi != null && oi.getItemType() != null ? oi.getItemType().name() : null)
                    .unitAmount(oi != null ? oi.getUnitPrice() : null)
                    .amount(oi != null ? oi.getAmount() : null)
                    .originalAmount(oi != null ? oi.getOriginalAmount() : null)
                    .discountAmount(oi != null ? oi.getDiscountAmount() : null)
                    .finalAmount(oi != null ? oi.getFinalAmount() : null)
                    .quantity(oi != null ? oi.getQuantity() : null)
                    .orderItemCreatedAt(oi != null ? oi.getCreatedAt() : null)
                    .build());
        }
        return list;
    }

    @Transactional(readOnly = true)
    public CookieChargeHistoryPageResponse getMyChargeHistoryPage(Long userId) {
        try {
            var items = getMyChargeHistory(userId);

            // 총 충전(유료 쿠키 합)과 총 사용(소진된 쿠키 합) 계산
            int totalCharged = 0;
            int totalUsed = 0;
            for (var it : items) {
                if (it.getCookieType() != null && it.getCookieType().equals("PAID")) {
                    totalCharged += it.getQtyTotal() != null ? it.getQtyTotal() : 0;
                }
                int used = 0;
                if (it.getQtyTotal() != null && it.getQtyRemain() != null) {
                    used = Math.max(0, it.getQtyTotal() - it.getQtyRemain());
                }
                totalUsed += used;
            }

            // 현재 보유(총량): wallet_balance.amount 기준
            Long balanceAmount;
            try {
                var balance = walletService.getWalletBalance(userId);
                balanceAmount = balance != null ? balance.getAmount() : 0L;
            } catch (IllegalStateException e) {
                // 지갑 또는 잔액이 없으면 0으로 처리
                log.debug("지갑 또는 잔액이 없어 0으로 처리합니다 - userId: {}", userId);
                balanceAmount = 0L;
            } catch (Exception e) {
                log.warn("잔액 조회 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage());
                balanceAmount = 0L;
            }

            return CookieChargeHistoryPageResponse.builder()
                    .balanceAmount(balanceAmount)
                    .totalCharged(totalCharged)
                    .totalUsed(totalUsed)
                    .items(items != null ? items : new java.util.ArrayList<>())
                    .build();
        } catch (IllegalArgumentException e) {
            // 사용자를 찾을 수 없거나 다른 예외 발생 시 빈 응답 반환
            log.warn("쿠키 충전 내역 조회 중 예외 발생 - userId: {}, error: {}", userId, e.getMessage());
            return CookieChargeHistoryPageResponse.builder()
                    .balanceAmount(0L)
                    .totalCharged(0)
                    .totalUsed(0)
                    .items(new java.util.ArrayList<>())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public List<CookieChargeGroupedResponse> getMyChargeHistoryGrouped(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        var batches = cookieBatchRepository.findByUserOrderByCreatedAtDesc(user);

        Map<Long, CookieChargeGroupedResponse.CookieChargeGroupedResponseBuilder> map = new java.util.LinkedHashMap<>();

        for (com.studyblock.domain.payment.entity.CookieBatch b : batches) {
            var oi = b.getOrderItem();
            Long orderId = (oi != null && oi.getOrder() != null) ? oi.getOrder().getId() : null;
            if (orderId == null) continue; // 주문 연결 없으면 건너뜀

            var order = oi.getOrder();
            var builder = map.computeIfAbsent(orderId, k -> CookieChargeGroupedResponse.builder()
                    .orderId(orderId)
                    .orderNumber(order != null ? order.getOrderNumber() : null)
                    .paidQty(0)
                    .bonusQty(0)
                    .totalQty(0)
                    .amount(order != null ? order.getTotalAmount() : null)
                    .chargedAt(order != null ? (order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt()) : null)
            );

            int addQty = b.getQtyTotal() != null ? b.getQtyTotal() : 0;
            if (b.getCookieType() == com.studyblock.domain.wallet.enums.CookieType.PAID) {
                builder.paidQty((builder.build().getPaidQty() != null ? builder.build().getPaidQty() : 0) + addQty);
            } else {
                builder.bonusQty((builder.build().getBonusQty() != null ? builder.build().getBonusQty() : 0) + addQty);
            }
            int currentTotal = (builder.build().getTotalQty() != null ? builder.build().getTotalQty() : 0) + addQty;
            builder.totalQty(currentTotal);
        }

        List<CookieChargeGroupedResponse> result = new java.util.ArrayList<>();
        for (var e : map.values()) {
            result.add(e.build());
        }
        return result;
    }

    // 쿠키 사용 내역 조회
    @Transactional(readOnly = true)
    public List<CookieUsageHistoryResponse> getMyUsageHistory(Long userId) {
        List<WalletLedger> ledgers = walletLedgerRepository.findByUser_IdAndType(userId, "DEBIT");
        ledgers.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return ledgers.stream()
                .map(this::toCookieUsageHistoryResponse)
                .collect(Collectors.toList());
    }

    private CookieUsageHistoryResponse toCookieUsageHistoryResponse(WalletLedger ledger) {
        Integer cookieAmount = Math.abs(ledger.getCookieAmount());
        OrderInfo orderInfo = extractOrderInfo(ledger);
        
        return CookieUsageHistoryResponse.builder()
                .id(ledger.getId())
                .cookieAmount(cookieAmount)
                .balanceAfter(ledger.getBalanceAfter())
                .notes(ledger.getNotes())
                .referenceType(ledger.getReferenceType())
                .referenceId(ledger.getReferenceId())
                .createdAt(ledger.getCreatedAt())
                .orderId(orderInfo.orderId)
                .orderNumber(orderInfo.orderNumber)
                .itemType(orderInfo.itemType)
                .courseId(orderInfo.courseId)
                .courseTitle(orderInfo.courseTitle)
                .sectionId(orderInfo.sectionId)
                .sectionTitle(orderInfo.sectionTitle)
                .build();
    }

    private OrderInfo extractOrderInfo(WalletLedger ledger) {
        if (!"ORDER".equals(ledger.getReferenceType()) || ledger.getReferenceId() == null) {
            return new OrderInfo();
        }

        Long orderId = ledger.getReferenceId();
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return new OrderInfo();
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(orderId);
        if (orderItems.isEmpty()) {
            return new OrderInfo(orderId, order.getOrderNumber(), null, null, null, null, null);
        }

        OrderItem firstItem = orderItems.get(0);
        String itemType = firstItem.getItemType() != null ? firstItem.getItemType().name() : null;

        // 섹션 구매인 경우
        if (firstItem.getSection() != null) {
            Long sectionId = firstItem.getSection().getId();
            String sectionTitle = firstItem.getSection().getTitle();
            Long courseId = null;
            String courseTitle = null;
            
            if (firstItem.getSection().getCourse() != null) {
                courseId = firstItem.getSection().getCourse().getId();
                courseTitle = firstItem.getSection().getCourse().getTitle();
            }
            
            return new OrderInfo(orderId, order.getOrderNumber(), itemType, 
                    courseId, courseTitle, sectionId, sectionTitle);
        }
        
        // 강의 구매인 경우
        if (firstItem.getCourse() != null) {
            Long courseId = firstItem.getCourse().getId();
            String courseTitle = firstItem.getCourse().getTitle();
            return new OrderInfo(orderId, order.getOrderNumber(), itemType, 
                    courseId, courseTitle, null, null);
        }

        return new OrderInfo(orderId, order.getOrderNumber(), itemType, null, null, null, null);
    }

    // 주문 정보를 담는 내부 클래스
    private static class OrderInfo {
        Long orderId;
        String orderNumber;
        String itemType;
        Long courseId;
        String courseTitle;
        Long sectionId;
        String sectionTitle;

        OrderInfo() {
            this(null, null, null, null, null, null, null);
        }

        OrderInfo(Long orderId, String orderNumber, String itemType,
                  Long courseId, String courseTitle, Long sectionId, String sectionTitle) {
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.itemType = itemType;
            this.courseId = courseId;
            this.courseTitle = courseTitle;
            this.sectionId = sectionId;
            this.sectionTitle = sectionTitle;
        }
    }
}


