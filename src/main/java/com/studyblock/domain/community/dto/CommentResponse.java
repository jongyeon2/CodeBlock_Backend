package com.studyblock.domain.community.dto;

import com.studyblock.domain.community.entity.Comment;
import com.studyblock.domain.community.enums.ContentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private ContentStatus status;
    private Long parentCommentId;
    private String nickName;
    private String userImageUrl;           // presigned URL
    private String userImageOriginalUrl;   // 원본 URL
    private String postTitle;              // 게시글 제목 (차단된 댓글 조회용)
    private String userName;               // 사용자 이름 (차단된 댓글 조회용)
    private String userNickname;           // 사용자 닉네임 (차단된 댓글 조회용)

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .status(comment.getStatus())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .nickName(comment.getUser().getNickname())
                .userImageUrl(comment.getUser().getImg())
                .userImageOriginalUrl(comment.getUser().getImg())
                .build();
    }
}
