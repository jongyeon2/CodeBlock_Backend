package com.studyblock.domain.cart.repository;

import com.studyblock.domain.cart.entity.CartItem;
import com.studyblock.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

//장바구니 리포지토리
@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {

    //사용자의 장바구니 전체 조회
    List<CartItem> findByUserOrderByAddedAtDesc(User user);

    //사용자 ID로 장바구니 조회
    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId ORDER BY c.addedAt DESC")
    List<CartItem> findByUserIdOrderByAddedAtDesc(@Param("userId") Long userId);

    //사용자 ID와 장바구니 아이템 ID로 조회
    @Query("SELECT c FROM CartItem c WHERE c.id = :itemId AND c.user.id = :userId")
    Optional<CartItem> findByIdAndUserId(@Param("itemId") Long itemId, @Param("userId") Long userId);

    //사용자와 강의로 조회 (중복 체크용)
    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.course.id = :courseId")
    Optional<CartItem> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    //사용자와 강의로 존재 여부 확인
    @Query("SELECT COUNT(c) > 0 FROM CartItem c WHERE c.user.id = :userId AND c.course.id = :courseId")
    boolean existsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    //사용자의 장바구니 전체 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    //사용자의 선택된 아이템 조회
    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.selected = true ORDER BY c.addedAt DESC")
    List<CartItem> findSelectedItemsByUserId(@Param("userId") Long userId);
}

