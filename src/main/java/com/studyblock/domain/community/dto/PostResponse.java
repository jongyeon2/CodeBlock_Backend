package com.studyblock.domain.community.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.enums.ContentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Post 엔티티의 상세 정보를 담는 Response DTO
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostResponse {

    private Long id;
    private Long boardId;
    private String boardName;
    private Long userId;
    private String userNickName;
    private String userProfileImage;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isEdited;
    private ContentStatus status;
    private Integer commentCount;
    private String imageUrl;
    private Long hit;

    private List<String> imageUrls;

    @Builder
    public PostResponse(Long id, Long boardId, String boardName, Long userId, String userNickName,
                        String userProfileImage, String title, String content,
                        LocalDateTime createdAt, LocalDateTime updatedAt,
                        Boolean isEdited, ContentStatus status, Integer commentCount, String imageUrl, Long hit) {
        this.id = id;
        this.boardId = boardId;
        this.boardName = boardName;
        this.userId = userId;
        this.userNickName = userNickName;
        this.userProfileImage = userProfileImage;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isEdited = isEdited;
        this.status = status;
        this.commentCount = commentCount;
        this.imageUrl = imageUrl;
        this.hit = hit;
        this.imageUrls = parseImageUrls(imageUrl);
    }
    //json 파싱 헬퍼 메소드
    private static List<String> parseImageUrls(String imageUrlJson){
        if(imageUrlJson == null || imageUrlJson.isEmpty()){
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(imageUrlJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            //파싱 실패 시 빈 리스트 리턴
            return new ArrayList<>();
        }
    }

    /**
     * Post 엔티티를 PostResponse로 변환
     */
    public static PostResponse from(Post post, int commentCount) {
        return PostResponse.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId())
                .boardName(post.getBoard().getName())
                .userId(post.getUser().getId())
                .userNickName(post.getUser().getNickname())
                .userProfileImage(post.getUser().getImg())
                .title(post.getTitle())
                .content(post.getCurrentContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .isEdited(post.getIsEdited())
                .status(post.getStatus())
                .commentCount(commentCount)
                .hit(post.getHit())
                .imageUrl(post.getImageUrl()) //json 문자열 전달
                .build();
    }

    /**
     * 간단한 정보만 포함하는 Response 생성 (목록용)
     */
    public static PostResponse fromSimple(Post post, int commentCount) {
        return PostResponse.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId())
                .userId(post.getUser().getId())
                .userNickName(post.getUser().getNickname())
                .title(post.getTitle())
                .createdAt(post.getCreatedAt())
                .isEdited(post.getIsEdited())
                .commentCount(commentCount)
                .build();
    }

}