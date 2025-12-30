package com.studyblock.domain.course.service;

import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.enums.OwnershipSource;
import com.studyblock.domain.course.enums.OwnershipStatus;
import com.studyblock.domain.enrollment.service.SectionEnrollmentService;
import com.studyblock.domain.user.repository.LectureOwnershipRepository;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.payment.entity.OrderItem;
import com.studyblock.domain.payment.enums.ItemType;
import com.studyblock.domain.payment.enums.PaymentType;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 섹션 단위 소유권 관리 서비스
 * 결제 완료 후 SECTION 타입 주문에 대해 LectureOwnership을 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LectureOwnershipService {

    private final LectureOwnershipRepository lectureOwnershipRepository;
    private final SectionEnrollmentService sectionEnrollmentService;

    /**
     * 주문으로부터 섹션 소유권 생성
     * SECTION 타입의 OrderItem들을 처리하여 LectureOwnership을 생성합니다.
     *
     * @param order 결제 완료된 주문
     * @return 생성된 소유권 목록
     */
    @Transactional
    public List<LectureOwnership> createOwnershipsFromOrder(Order order) {
        log.info("주문 {}로부터 섹션 소유권 생성 시작", order.getOrderNumber());

        List<LectureOwnership> ownerships = new ArrayList<>();
        User user = order.getUser();

        for (OrderItem item : order.getOrderItems()) {
            if (item.getItemType() == ItemType.SECTION && item.getSection() != null) {
                try {
                    LectureOwnership ownership = createOwnership(user, item.getSection(), order);
                    ownerships.add(ownership);
                    log.info("섹션 소유권 생성 성공 - User: {}, Section: {}, Order: {}",
                            user.getId(), item.getSection().getId(), order.getOrderNumber());
                } catch (IllegalStateException e) {
                    log.warn("섹션 소유권 생성 실패 (중복 가능): {}", e.getMessage());
                }
            }
        }

        log.info("총 {}개의 섹션 소유권 생성 완료 - Order: {}", ownerships.size(), order.getOrderNumber());
        return ownerships;
    }

    /**
     * 개별 섹션 소유권 생성
     * 중복 체크 후 LectureOwnership 엔티티를 생성하고 저장합니다.
     *
     * @param user 사용자
     * @param section 섹션
     * @param order 주문
     * @return 생성된 소유권
     * @throws IllegalStateException 이미 소유권이 존재하는 경우
     */
    @Transactional
    public LectureOwnership createOwnership(User user, Section section, Order order) {
        // 중복 체크 - 이미 활성 소유권이 있는지 확인
        boolean exists = lectureOwnershipRepository.existsByUserAndSectionAndStatus(
                user, section, OwnershipStatus.ACTIVE
        );

        if (exists) {
            throw new IllegalStateException(
                    String.format("사용자 %d는 이미 섹션 %d에 대한 소유권을 보유하고 있습니다",
                            user.getId(), section.getId())
            );
        }

        // PaymentType에 따른 OwnershipSource 결정
        OwnershipSource source = determineOwnershipSource(order.getPaymentType());

        // LectureOwnership 생성
        LectureOwnership ownership = LectureOwnership.builder()
                .user(user)
                .section(section)
                .order(order)
                .source(source)
                .expiresAt(null) // 영구 소유권
                .build();

        LectureOwnership saved = lectureOwnershipRepository.saveAndFlush(ownership);
        sectionEnrollmentService.createOrUpdateForSection(user, section, order);
        log.debug("섹션 소유권 저장 - userId: {}, sectionId: {}, ownershipId: {}", user.getId(), section.getId(), saved.getId());
        return saved;
    }

    /**
     * 결제 타입에 따른 소유권 출처 결정
     *
     * @param paymentType 결제 타입
     * @return 소유권 출처
     */
    private OwnershipSource determineOwnershipSource(PaymentType paymentType) {
        return switch (paymentType) {
            case CASH, MIXED -> OwnershipSource.PURCHASE_CASH;
            case COOKIE -> OwnershipSource.PURCHASE_PAID_COOKIE;
            default -> throw new IllegalArgumentException("지원하지 않는 결제 타입: " + paymentType);
        };
    }

    /**
     * 관리자가 수동으로 소유권 부여
     *
     * @param user 사용자
     * @param section 섹션
     * @return 생성된 소유권
     */
    @Transactional
    public LectureOwnership grantOwnershipByAdmin(User user, Section section) {
        // 중복 체크
        boolean exists = lectureOwnershipRepository.existsByUserAndSectionAndStatus(
                user, section, OwnershipStatus.ACTIVE
        );

        if (exists) {
            throw new IllegalStateException(
                    String.format("사용자 %d는 이미 섹션 %d에 대한 소유권을 보유하고 있습니다",
                            user.getId(), section.getId())
            );
        }

        LectureOwnership ownership = LectureOwnership.builder()
                .user(user)
                .section(section)
                .order(null) // 관리자 부여는 주문 없음
                .source(OwnershipSource.ADMIN)
                .expiresAt(null) // 영구 소유권
                .build();

        LectureOwnership saved = lectureOwnershipRepository.save(ownership);
        sectionEnrollmentService.createOrUpdateForSection(user, section, null);

        log.info("관리자가 섹션 소유권 부여 - User: {}, Section: {}", user.getId(), section.getId());
        return saved;
    }

    /**
     * 소유권 취소 (환불 시 사용)
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void revokeOwnershipsByOrder(Long orderId) {
        List<LectureOwnership> ownerships = lectureOwnershipRepository.findByOrderId(orderId);

        for (LectureOwnership ownership : ownerships) {
            ownership.revoke();
        }

        log.info("주문 {}에 대한 {}개 소유권 취소 완료", orderId, ownerships.size());
    }

    /**
     * 사용자가 특정 코스에서 구매한 섹션 ID 목록 조회
     *
     * @param userId 사용자 ID
     * @param courseId 코스 ID
     * @return 구매한 섹션 ID 목록
     */
    public List<Long> getPurchasedSectionIds(Long userId, Long courseId) {
        return lectureOwnershipRepository.findPurchasedSectionIdsByUserAndCourse(userId, courseId);
    }

    /**
     * 사용자가 구매한 모든 섹션 ID 목록 조회 (코스 필터 없음)
     *
     * @param userId 사용자 ID
     * @return 구매한 모든 섹션 ID 목록
     */
    public List<Long> getPurchasedSectionIdsByUser(Long userId) {
        return lectureOwnershipRepository.findPurchasedSectionIdsByUser(userId);
    }
}
