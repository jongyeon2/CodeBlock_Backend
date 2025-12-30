package com.studyblock.domain.admin.dto;

import com.studyblock.domain.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String originalContent;
    private String editedContent;
    private String imageUrl;          // presigned URL
    private String imageOriginalUrl;  // 원본 URL
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Enum status;
    private Boolean is_edited;

    public static NoticeResponse from(Post post) {
        return NoticeResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .originalContent(post.getOriginalContent())
                .editedContent(post.getEditedContent())
                .imageUrl(post.getImageUrl())
                .imageOriginalUrl(post.getImageUrl())
                .created_at(post.getCreatedAt())
                .updated_at(post.getUpdatedAt())
                .status(post.getStatus())
                .is_edited(post.getIsEdited())
                .build();
    }
}
