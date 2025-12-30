package com.studyblock.domain.community.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PostListResponse {
    private List<PostResponse> posts; //전체 게시물 리스트
    private long totalCount; //전체 게시물 수

    public PostListResponse(List<PostResponse> postResponses, long totalCount) {
        this.posts = postResponses;
        this.totalCount = totalCount;
    }
}
