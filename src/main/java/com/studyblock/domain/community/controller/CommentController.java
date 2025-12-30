package com.studyblock.domain.community.controller;

import com.studyblock.domain.community.dto.CommentRequest;
import com.studyblock.domain.community.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    //댓글 작성 (저장)
    @PostMapping("/comment/{postId}")
    public ResponseEntity<Void> createComment(@PathVariable Long postId, @RequestHeader("userId") Long userId, @RequestBody CommentRequest request) {

        commentService.createComment(postId, userId, request);
        return ResponseEntity.ok().build();  // 본문 없이 200 OK만
    }

    //댓글 삭제
    @DeleteMapping("/comment/remove/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId){
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

}
