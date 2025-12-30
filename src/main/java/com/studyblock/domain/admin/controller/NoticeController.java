package com.studyblock.domain.admin.controller;

import com.studyblock.domain.admin.dto
        .EditNoticeRequest;
import com.studyblock.domain.admin.dto.NoticeAddRequest;
import com.studyblock.domain.admin.dto.NoticeResponse;
import com.studyblock.domain.admin.service.NoticeService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "공지사항 관리", description = "공지사항 CRUD API")
public class NoticeController {

    private final NoticeService noticeService;

    // 공지사항 등록
    @Operation(summary = "공지사항 등록", description = "새로운 공지사항을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "중복 제목 또는 잘못된 요청")
    })
    @PostMapping("/createnotice")
    public ResponseEntity<?> createPost(
            @RequestPart("form") NoticeAddRequest dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {

        try {
            Post post = noticeService.createPost(
                    currentUser,
                    dto.getTitle(),
                    dto.getOriginalContent(),
                    files
            );
            return ResponseEntity.ok(noticeService.toNoticeResponse(post));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "공지사항 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 공지사항 목록 조회
    @Operation(summary = "공지사항 목록 조회", description = "모든 공지사항 목록을 조회합니다.")
    @GetMapping("/post/notice")
    public List<NoticeResponse> getNoticeList() {
        return noticeService.getNoticeList();
    }

    // 공지사항 수정
    @Operation(summary = "공지사항 수정", description = "기존 공지사항 내용을 수정합니다.")
    @PutMapping("/post/editnotice/{id}")
    public ResponseEntity<?> editNotice(
            @PathVariable Long id,
            @RequestPart("form") EditNoticeRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            Post edit = noticeService.editNotice(id, request.getEditedContent(), files, request.getRemoveImage());
            NoticeResponse response = noticeService.toNoticeResponse(edit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "공지사항 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 공지사항 삭제(비활성화)
    @Operation(summary = "공지사항 삭제", description = "공지사항 상태를 'DELETED'로 변경합니다.")
    @DeleteMapping("/post/deletenotice/{id}")
    public ResponseEntity<?> deleteNotice(@PathVariable Long id) {
        try {
            noticeService.deleteNotice(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
