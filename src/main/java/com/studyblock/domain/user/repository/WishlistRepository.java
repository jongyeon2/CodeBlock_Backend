package com.studyblock.domain.user.repository;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.user.entity.Wishlist;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // Wishlist를 기준으로 유저 id를 통해 찜 목록 내의 섹션 id 추출 -> section table에서 섹션 정보들 추출
    // Section과 연결된 Course를 함께 가져오기
    @Query("""
        SELECT DISTINCT c
        FROM Wishlist w
        JOIN w.course c
        WHERE w.user.id = :userId
    """)
    List<Course> findCoursesByUserId(@Param("userId") Long userId);

    //유저의 찜 목록 삭제
    void deleteByUser_IdAndCourse_Id(Long userId, Long courseId);

    // 특정 유저가 특정 코스를 찜했는지 확인
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // 특정 유저의 특정 코스 찜 항목 조회
    @Query("""
        SELECT w
        FROM Wishlist w
        WHERE w.user.id = :userId AND w.course.id = :courseId
    """)
    Wishlist findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

}
