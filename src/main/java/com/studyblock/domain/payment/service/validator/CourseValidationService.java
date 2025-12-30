package com.studyblock.domain.payment.service.validator;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.payment.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 강의 검증 전담 서비스
// 단일 책임: 강의 존재 여부, 구매 가능 여부, 중복 구매 검증
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseValidationService {

    private final CourseRepository courseRepository;
    private final OrderItemRepository orderItemRepository;

    // 강의 존재 여부 및 구매 가능 여부 검증
    public Course validateCourseExists(Long courseId) {
        // 강의가 DB에 존재하는지 확인
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "강의를 찾을 수 없습니다. ID: " + courseId
                ));

        // 강의가 공개 상태인지 확인
        if (!course.getIsPublished()) {
            throw new IllegalStateException("구매할 수 없는 강의입니다.");
        }

        // 강의 가격이 설정되어 있는지 확인
        if (course.getPrice() == null || course.getPrice() < 0) {
            throw new IllegalStateException("강의 가격 정보가 올바르지 않습니다.");
        }

        return course; // Course 객체 반환 (가격 검증 등에 재사용)
    }

    // 중복 구매 방지
    public void validateDuplicatePurchase(Long userId, Long courseId) {
        // 사용자가 이미 결제 완료된 주문에서 이 강의를 구매했는지 확인
        boolean alreadyPurchased = orderItemRepository
                .existsByUserIdAndCourseIdAndPaid(userId, courseId);

        if (alreadyPurchased) {
            throw new IllegalStateException("이미 구매한 강의입니다.");
        }
    }

    // 강의가 쿠키 결제 가능한지 검증 (현재는 Course는 현금 결제만 가능)
    public void validatePaymentMethod(Long cookieAmount) {
        if (cookieAmount != null && cookieAmount > 0) {
            throw new IllegalStateException("코스는 쿠키 결제가 불가능합니다. 현금 결제만 가능합니다.");
        }
    }
}

