package com.studyblock.domain.user.repository;

import com.studyblock.domain.course.entity.LectureOwnership;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.enums.OwnershipStatus;
import com.studyblock.domain.payment.entity.Order;
import com.studyblock.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LectureOwnershipRepository extends JpaRepository<LectureOwnership, Long> {

    //보유 강의들 강의 정보
    @Query("""
        SELECT lo
        FROM LectureOwnership lo
        JOIN FETCH lo.section s
        JOIN FETCH s.course c
        WHERE lo.user.id = :userId
    """)
    List<LectureOwnership> findActiveOwnershipsByUserId(@Param("userId") Long userId);

    /**
     * 사용자와 섹션으로 특정 상태의 소유권 존재 여부 확인
     */
    boolean existsByUserAndSectionAndStatus(User user, Section section, OwnershipStatus status);

    /**
     * 주문 ID로 소유권 조회 (환불 처리 시 사용)
     */
    @Query("""
        SELECT lo
        FROM LectureOwnership lo
        WHERE lo.order.id = :orderId
    """)
    List<LectureOwnership> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 사용자가 특정 코스에서 구매한 섹션 ID 목록 조회
     * @param userId 사용자 ID
     * @param courseId 코스 ID
     * @return 구매한 섹션 ID 목록 (ACTIVE 상태만)
     */
    @Query("""
        SELECT DISTINCT lo.section.id
        FROM LectureOwnership lo
        WHERE lo.user.id = :userId
        AND lo.section.course.id = :courseId
        AND lo.status = 'ACTIVE'
    """)
    List<Long> findPurchasedSectionIdsByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /**
     * 사용자가 구매한 모든 섹션 ID 목록 조회 (코스 필터 없음)
     * @param userId 사용자 ID
     * @return 구매한 섹션 ID 목록 (ACTIVE 상태만)
     */
    @Query("""
        SELECT DISTINCT lo.section.id
        FROM LectureOwnership lo
        WHERE lo.user.id = :userId
        AND lo.status = 'ACTIVE'
    """)
    List<Long> findPurchasedSectionIdsByUser(@Param("userId") Long userId);
}
