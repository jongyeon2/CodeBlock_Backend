package com.studyblock.domain.user.controller;

import com.studyblock.domain.user.dto.InstructorProfilePatchRequest;
import com.studyblock.domain.user.dto.InstructorProfileResponse;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.service.InstructorProfileService;
import com.studyblock.global.dto.CommonResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructors/profile")
@RequiredArgsConstructor
@Validated
@Slf4j
public class InstructorProfileController {

    private final InstructorProfileService instructorProfileService;

    @GetMapping
    public ResponseEntity<CommonResponse<InstructorProfileResponse>> getMyProfile() {
        Long userId = getCurrentUserId();
        log.debug("내 강사 프로필 조회 요청 - userId: {}", userId);
        
        try {
            InstructorProfile profile = instructorProfileService.getMyProfile(userId);
            InstructorProfileResponse response = InstructorProfileResponse.from(profile);
            return ResponseEntity.ok(CommonResponse.success(response));
        } catch (EntityNotFoundException e) {
            log.warn("강사 프로필 없음 - userId: {}", userId);
            return ResponseEntity.status(404).body(
                    CommonResponse.error("강사 프로필이 존재하지 않습니다.")
            );
        }
    }

    @PatchMapping
    public ResponseEntity<CommonResponse<InstructorProfileResponse>> patchMyProfile(
            @Valid @RequestBody InstructorProfilePatchRequest request
    ) {
        Long userId = getCurrentUserId();
        log.debug("강사 프로필 수정 요청 - userId: {}", userId);
        
        InstructorProfile saved = instructorProfileService.upsertMyProfile(userId, request);
        InstructorProfileResponse response = InstructorProfileResponse.from(saved);
        
        return ResponseEntity.ok(CommonResponse.success(
                "강사 프로필이 저장되었습니다.",
                response
        ));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID 추출
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 접근 시도");
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) {
            log.warn("인증된 사용자 정보를 찾을 수 없음 - principal type: {}", 
                    principal != null ? principal.getClass().getName() : "null");
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        return ((User) principal).getId();
    }
}
