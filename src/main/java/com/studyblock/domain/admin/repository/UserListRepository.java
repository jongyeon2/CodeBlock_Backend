package com.studyblock.domain.admin.repository;

import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserListRepository extends JpaRepository<User, Long> {

    /**
     * 관리자를 제외한 사용자 목록 조회
     * AdminProfile이 없고, ADMIN 역할을 가지지 않은 사용자만 조회
     * InstructorProfile을 함께 fetch하여 강사 여부 판단 가능
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.instructorProfile " +
           "WHERE NOT EXISTS (SELECT 1 FROM AdminProfile ap WHERE ap.userId = u.id) " +
           "AND NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r WHERE ur.user.id = u.id AND r.code = 'ADMIN')")
    List<User> findAllExceptAdmin();

    /**
     * 활성화 상태인 회원 수 카운트 (관리자 제외)
     * role_id가 1인 사용자(관리자)를 제외한 일반 회원만 카운트
     */
    @Query("""
        SELECT COUNT(DISTINCT u) FROM User u 
        WHERE u.status = :status 
            AND u.id NOT IN (
                SELECT ur.user.id FROM UserRole ur 
                WHERE ur.role.id = 1
            )
        """)
    long countActiveUsersExcludingAdmin(@Param("status") Integer statusValue);

    /**
     * 상태별 사용자 조회
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.instructorProfile " +
            "WHERE u.status = :statusValue")
    List<User> findByStatusEnum(@Param("statusValue") Integer statusValue);

    /**
     * 상태별 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :statusValue")
    Long countByStatusEnum(@Param("statusValue") Integer statusValue);

    /**
     * 이번 달에 차단된 사용자 수 조회 (status = SUSPENDED이고 이번 달에 updatedAt이 변경된 경우)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :statusValue AND YEAR(u.updatedAt) = :year AND MONTH(u.updatedAt) = :month")
    Long countByStatusAndUpdatedAtMonth(@Param("statusValue") Integer statusValue, @Param("year") int year, @Param("month") int month);
}
