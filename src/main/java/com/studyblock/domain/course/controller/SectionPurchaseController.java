package com.studyblock.domain.course.controller;

import com.studyblock.domain.course.dto.SectionPurchaseRequest;
import com.studyblock.domain.course.service.SectionPurchaseService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.global.dto.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionPurchaseController {

    private final SectionPurchaseService sectionPurchaseService;

    @PostMapping("/purchase")
    public ResponseEntity<CommonResponse<Map<String, Object>>> purchaseSection(
            @Valid @RequestBody SectionPurchaseRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(401).body(CommonResponse.error("인증되지 않은 사용자입니다"));
            }
            Long userId = ((User) authentication.getPrincipal()).getId();

            var result = sectionPurchaseService.purchase(userId, request.getSectionId());
            return ResponseEntity.ok(CommonResponse.success("섹션 구매가 완료되었습니다", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("섹션 구매 실패", e);
            return ResponseEntity.status(500).body(CommonResponse.error("섹션 구매 중 오류가 발생했습니다"));
        }
    }
}


