package com.studyblock.domain.community.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {
    private Long boardId;
    private String title;
    private String originalContent; //첫 내용
    //이미지는 따로 처리

    //게시글 수정에도 재활용
    private String editedContent = null;
    private LocalDateTime updatedAt = null;
    private List<String> RemainingImageUrls;
}
