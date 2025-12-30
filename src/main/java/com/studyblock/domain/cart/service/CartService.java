package com.studyblock.domain.cart.service;

import com.studyblock.domain.cart.dto.*;
import com.studyblock.domain.cart.entity.CartItem;
import com.studyblock.domain.cart.repository.CartRepository;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

//장바구니 서비스
//장바구니 CRUD 및 동기화 기능을 제공합니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    //장바구니 조회
    public CartResponse getCart(Long userId) {
        log.info("장바구니 조회 - userId: {}", userId);
        
        // 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        List<CartItem> items = cartRepository.findByUserIdOrderByAddedAtDesc(userId);
        
        // 선택된 아이템 ID 목록
        List<Long> selectedIds = items.stream()
                .filter(CartItem::getSelected)
                .map(item -> item.getCourse().getId())
                .collect(Collectors.toList());
        
        // 마지막 업데이트 시간
        LocalDateTime lastUpdated = items.isEmpty() ? null :
                items.stream()
                        .map(CartItem::getUpdatedAt)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

        return CartResponse.builder()
                .cartItems(items.stream()
                        .map(this::toCartItemDto)
                        .collect(Collectors.toList()))
                .selectedIds(selectedIds)
                .lastUpdated(lastUpdated)
                .build();
    }

    //장바구니 저장/업데이트
    //기존 장바구니를 모두 삭제하고 새로 저장합니다.
    @Transactional
    public CartResponse saveCart(Long userId, CartRequest request) {
        log.info("장바구니 저장/업데이트 - userId: {}, itemCount: {}", userId, 
                request.getCartItems() != null ? request.getCartItems().size() : 0);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 기존 장바구니 삭제
        cartRepository.deleteByUserId(userId);
        log.info("기존 장바구니 삭제 완료 - userId: {}", userId);

        // 새 장바구니 아이템 저장
        if (request.getCartItems() != null && !request.getCartItems().isEmpty()) {
            List<CartItem> items = request.getCartItems().stream()
                    .map(dto -> {
                        Course course = courseRepository.findById(dto.getCourseId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "강의를 찾을 수 없습니다. ID: " + dto.getCourseId()));

                        // selectedIds에 포함되어 있으면 selected = true
                        boolean selected = request.getSelectedIds() != null &&
                                request.getSelectedIds().contains(dto.getCourseId());

                        return CartItem.builder()
                                .user(user)
                                .course(course)
                                .name(dto.getName())
                                .price(dto.getPrice())
                                .originalPrice(dto.getOriginalPrice())
                                .discountPercentage(dto.getDiscountPercentage())
                                .hasDiscount(dto.getHasDiscount())
                                .selected(selected)
                                .build();
                    })
                    .collect(Collectors.toList());

            cartRepository.saveAll(items);
            log.info("장바구니 저장 완료 - userId: {}, itemCount: {}", userId, items.size());
        }

        return getCart(userId);
    }

    //장바구니 아이템 추가
    @Transactional
    public CartItemResponse addItem(Long userId, CartItemRequest request) {
        log.info("장바구니 아이템 추가 - userId: {}, courseId: {}", userId, request.getCourseId());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "강의를 찾을 수 없습니다. ID: " + request.getCourseId()));

        // 중복 체크
        if (cartRepository.existsByUserIdAndCourseId(userId, request.getCourseId())) {
            throw new IllegalStateException("이미 장바구니에 있는 강의입니다.");
        }

        CartItem item = CartItem.builder()
                .user(user)
                .course(course)
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .discountPercentage(request.getDiscountPercentage())
                .hasDiscount(request.getHasDiscount())
                .selected(request.getSelected())
                .build();

        CartItem saved = cartRepository.save(item);
        log.info("장바구니 아이템 추가 완료 - itemId: {}, courseId: {}", saved.getId(), request.getCourseId());

        return toCartItemResponse(saved);
    }

    //장바구니 아이템 삭제
    @Transactional
    public void removeItem(Long userId, Long itemId) {
        log.info("장바구니 아이템 삭제 - userId: {}, itemId: {}", userId, itemId);
        
        CartItem item = cartRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "장바구니 아이템을 찾을 수 없습니다. ID: " + itemId));

        cartRepository.delete(item);
        log.info("장바구니 아이템 삭제 완료 - itemId: {}", itemId);
    }

    //장바구니 전체 삭제
    @Transactional
    public void clearCart(Long userId) {
        log.info("장바구니 전체 삭제 - userId: {}", userId);
        
        cartRepository.deleteByUserId(userId);
        log.info("장바구니 전체 삭제 완료 - userId: {}", userId);
    }

    //장바구니 아이템 선택 상태 업데이트
    @Transactional
    public CartItemResponse updateSelection(Long userId, Long itemId, Boolean selected) {
        log.info("장바구니 아이템 선택 상태 업데이트 - userId: {}, itemId: {}, selected: {}", 
                userId, itemId, selected);
        
        CartItem item = cartRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "장바구니 아이템을 찾을 수 없습니다. ID: " + itemId));

        item.updateSelection(selected);
        CartItem saved = cartRepository.save(item);
        log.info("장바구니 아이템 선택 상태 업데이트 완료 - itemId: {}, selected: {}", 
                saved.getId(), saved.getSelected());

        return toCartItemResponse(saved);
    }

    //CartItem을 CartItemDto로 변환
    private CartItemDto toCartItemDto(CartItem item) {
        return CartItemDto.builder()
                .id(item.getId())
                .courseId(item.getCourse().getId())
                .name(item.getName())
                .price(item.getPrice())
                .originalPrice(item.getOriginalPrice())
                .discountPercentage(item.getDiscountPercentage())
                .hasDiscount(item.getHasDiscount())
                .selected(item.getSelected())
                .addedAt(item.getAddedAt())
                .build();
    }

    //CartItem을 CartItemResponse로 변환
    private CartItemResponse toCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .courseId(item.getCourse().getId())
                .name(item.getName())
                .price(item.getPrice())
                .originalPrice(item.getOriginalPrice())
                .discountPercentage(item.getDiscountPercentage())
                .hasDiscount(item.getHasDiscount())
                .selected(item.getSelected())
                .addedAt(item.getAddedAt())
                .build();
    }
}

