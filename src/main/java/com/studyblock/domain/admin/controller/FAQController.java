package com.studyblock.domain.admin.controller;

import com.studyblock.domain.admin.dto.EditNoticeRequest;
import com.studyblock.domain.admin.dto.NoticeAddRequest;
import com.studyblock.domain.admin.dto.NoticeResponse;
import com.studyblock.domain.admin.service.FAQService;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "FAQ 관리", description = "FAQ CRUD API")
public class FAQController {

    private final FAQService faqService;

    // FAQ 등록
    @Operation(summary = "FAQ 등록", description = "새로운 FAQ를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "중복 또는 잘못된 요청")
    })
    @PostMapping("/post/createfaq")
    public ResponseEntity<?> createFAQ(
            @RequestBody NoticeAddRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증이 필요합니다."));
        }

        try {
            Post post = faqService.createFAQ(
                    currentUser,
                    request.getTitle(),
                    request.getOriginalContent()
            );
            return ResponseEntity.ok(NoticeResponse.from(post));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "FAQ 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // FAQ 목록 조회
    @Operation(summary = "FAQ 목록 조회", description = "모든 FAQ 목록을 조회합니다.")
    @GetMapping("/post/faq")
    public List<NoticeResponse> getFAQList() {
        return faqService.getFAQList();
    }

    // FAQ 수정
    @Operation(summary = "FAQ 수정", description = "기존 FAQ 내용을 수정합니다.")
    @PutMapping("/post/editfaq/{id}")
    public ResponseEntity<?> editFAQ(
            @PathVariable Long id,
            @RequestBody EditNoticeRequest request
            ) {
        try {
            Post edit = faqService.editFAQ(id, request.getEditedContent());
            NoticeResponse response = NoticeResponse.from(edit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "FAQ 수정 중 오류가 발생했습니다."));
        }
    }

    // FAQ 비활성화
    @Operation(summary = "FAQ 비활성화", description = "FAQ 상태를 'DELETED'로 변경합니다.")
    @DeleteMapping("/post/deletefaq/{id}")
    public ResponseEntity<?> deleteFAQ(@PathVariable Long id) {
        try {
            faqService.deleteFAQ(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
