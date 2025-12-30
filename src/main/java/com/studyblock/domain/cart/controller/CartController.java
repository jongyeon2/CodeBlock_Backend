package com.studyblock.domain.cart.controller;

import com.studyblock.domain.cart.dto.*;
import com.studyblock.domain.cart.service.CartService;
import com.studyblock.global.dto.CommonResponse;
import com.studyblock.global.swagger.CommonApiResponses;
import com.studyblock.global.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//장바구니 컨트롤러
//장바구니 CRUD 및 동기화 기능을 제공합니다.
@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "장바구니 관련 API")
public class CartController {

    private final CartService cartService;
    private final AuthenticationUtils authenticationUtils;

    //장바구니 조회 GET /api/cart
    @GetMapping
    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CartResponse>> getCart(
            Authentication authentication) {
        
        try {
            // 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            CartResponse response = cartService.getCart(userId);
            
            return ResponseEntity.ok(CommonResponse.success("장바구니 조회 성공", response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장바구니 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("장바구니 조회 중 오류가 발생했습니다"));
        }
    }

    //장바구니 저장/업데이트 POST /api/cart 또는 PUT /api/cart
    @PostMapping
    @PutMapping
    @Operation(summary = "장바구니 저장/업데이트", 
                description = "장바구니를 저장하거나 업데이트합니다. 기존 장바구니는 모두 삭제되고 새로 저장됩니다.")
    @ApiResponse(responseCode = "200", description = "저장 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CartResponse>> saveCart(
            @Valid @RequestBody CartRequest request,
            Authentication authentication) {
        
        try {
            // 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            CartResponse response = cartService.saveCart(userId, request);
            
            return ResponseEntity.ok(CommonResponse.success("장바구니가 저장되었습니다", response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장바구니 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("장바구니 저장 중 오류가 발생했습니다"));
        }
    }

    //장바구니 아이템 추가 POST /api/cart/items
    @PostMapping("/items")
    @Operation(summary = "장바구니 아이템 추가", description = "장바구니에 강의를 추가합니다.")
    @ApiResponse(responseCode = "200", description = "추가 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CartItemResponse>> addItem(
            @Valid @RequestBody CartItemRequest request,
            Authentication authentication) {
        
        try {
            // 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            CartItemResponse response = cartService.addItem(userId, request);
            
            return ResponseEntity.ok(CommonResponse.success("장바구니에 추가되었습니다", response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장바구니 아이템 추가 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("장바구니 아이템 추가 중 오류가 발생했습니다"));
        }
    }

    //장바구니 아이템 삭제 DELETE /api/cart/items/{itemId}
    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "장바구니 아이템 삭제", description = "장바구니에서 아이템을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<MessageResponse>> removeItem(
            @Parameter(description = "장바구니 아이템 ID", required = true, example = "1")
            @PathVariable Long itemId,
            Authentication authentication) {
        
        try {
            // 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            cartService.removeItem(userId, itemId);
            
            return ResponseEntity.ok(CommonResponse.success(
                    "장바구니에서 제거되었습니다",
                    MessageResponse.builder().message("장바구니에서 제거되었습니다").build()));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장바구니 아이템 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("장바구니 아이템 삭제 중 오류가 발생했습니다"));
        }
    }

    //장바구니 전체 삭제 DELETE /api/cart
    @DeleteMapping
    @Operation(summary = "장바구니 전체 삭제", description = "사용자의 장바구니를 모두 비웁니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<MessageResponse>> clearCart(
            Authentication authentication) {
        
        try {
            // 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            cartService.clearCart(userId);
            
            return ResponseEntity.ok(CommonResponse.success(
                    "장바구니가 비워졌습니다",
                    MessageResponse.builder().message("장바구니가 비워졌습니다").build()));
            
        } catch (Exception e) {
            log.error("장바구니 전체 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("장바구니 삭제 중 오류가 발생했습니다"));
        }
    }

    //장바구니 아이템 선택 상태 업데이트 PATCH /api/cart/items/{itemId}/select
    @PatchMapping("/items/{itemId}/select")
    @Operation(summary = "장바구니 아이템 선택 상태 업데이트", 
               description = "장바구니 아이템의 선택 상태를 업데이트합니다.")
    @ApiResponse(responseCode = "200", description = "업데이트 성공")
    @CommonApiResponses
    public ResponseEntity<CommonResponse<CartItemResponse>> updateSelection(
            @Parameter(description = "장바구니 아이템 ID", required = true, example = "1")
            @PathVariable Long itemId,
            @Valid @RequestBody SelectRequest request,
            Authentication authentication) {
        
        try {
            // 인증 확인
            if (!authenticationUtils.isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }

            Long userId = authenticationUtils.extractAuthenticatedUserId(authentication);
            CartItemResponse response = cartService.updateSelection(userId, itemId, request.getSelected());
            
            return ResponseEntity.ok(CommonResponse.success(
                    "선택 상태가 업데이트되었습니다", response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장바구니 아이템 선택 상태 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("선택 상태 업데이트 중 오류가 발생했습니다"));
        }
    }
}

