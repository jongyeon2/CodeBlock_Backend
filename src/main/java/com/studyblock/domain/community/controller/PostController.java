package com.studyblock.domain.community.controller;

import com.studyblock.domain.community.dto.*;
import com.studyblock.domain.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

//    /**
//     * 게시글 전체 목록 조회
//     */
//    @GetMapping
//    public ResponseEntity<List<PostResponse>> getAllPosts() {
//        List<PostResponse> posts = communityService.getAllPosts();
//        return ResponseEntity.ok(posts);
//    }

    //게시글 1개 조회

    //특정 게시판의 게시글들 조회
    @GetMapping("/post/{boardId}")
    public ResponseEntity<List<PostResponse>> getPostsByBoard(@PathVariable Long boardId) {
        List<PostResponse> posts = postService.getPostByCategory(boardId);
        return ResponseEntity.ok(posts);
    }

    //게시물의 댓글 조회 (작성자 프로필 포함)
    @GetMapping("/post/comment/{postId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByPost(@PathVariable Long postId) {
        List<CommentResponse> comment = postService.getPostComment(postId);
        return ResponseEntity.ok(comment);
    }

    //게시물의 댓글 조회 (모든 상태 포함, 관리자용)
    @GetMapping("/post/comment/admin/{postId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByPostForAdmin(@PathVariable Long postId) {
        List<CommentResponse> comment = postService.getPostCommentForAdmin(postId);
        return ResponseEntity.ok(comment);
    }

    //특정 게시판의 게시글들 조회 (모든 상태 포함, 관리자용)
    @GetMapping("/post/admin/{boardId}")
    public ResponseEntity<List<PostResponse>> getPostsByBoardForAdmin(@PathVariable Long boardId) {
        List<PostResponse> posts = postService.getPostByCategoryForAdmin(boardId);
        return ResponseEntity.ok(posts);
    }

    //활성화된 게시글 전부 불러오기
    @GetMapping("/post/active")
    public ResponseEntity<PostListResponse> getAllActivePosts(){
        PostListResponse response = postService.getAllActivePosts();
        return ResponseEntity.ok(response);
    }

    //활성화된 게시글 전부 불러오기 (FAQ제외한 글들)
    @GetMapping("/post/active2")
    public ResponseEntity<PostListResponse> getAllActivePosts2(){
        PostListResponse response = postService.getAllActivePosts2();
        return ResponseEntity.ok(response);
    }

    //조회수 증가
    @PostMapping("/post/{postId}/view")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long postId, @RequestHeader("User-Id") Long userId){
        postService.increaseViewCount(postId, userId);
        return ResponseEntity.ok().build();
    }

    //자유게시판 인기 게시글 10개 불러오기(최근 1개월 기준)
    @GetMapping("/post/freeboard/top10")
    public ResponseEntity<List<PostResponse>> getTop10PopularFreeBoardPosts(){
        List<PostResponse> posts = postService.getTop10PopularFreeBoardPosts(10); //최대 10개
        return ResponseEntity.ok(posts);
    }

    //자유게시판 인기 게시글 5개 불러오기(최근 1개월 기준)
    @GetMapping("/post/freeboard/top5")
    public ResponseEntity<List<PostResponse>> getTop5PopularFreeBoardPosts(){
        List<PostResponse> posts = postService.getTop10PopularFreeBoardPosts(5); //최대 5개
        return ResponseEntity.ok(posts);
    }



    //게시글 작성
    @PostMapping(value = "/post/write/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @PathVariable Long boardId,
            @RequestHeader("User-Id") Long userId,
            @RequestPart("request") PostCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {

        PostResponse response = postService.createPost(userId, boardId, request, imageFiles);
        return ResponseEntity.ok(response);
    }

     //게시글 수정
    @PutMapping(value = "/post/edit/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> editPost(@PathVariable Long postId,
                                                 @RequestPart("request") PostCreateRequest request,
                                                 @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                                 @RequestHeader("User-Id") Long userId){

        PostResponse response = postService.updatePost(postId, userId, request, images);
        return ResponseEntity.ok(response);
    }

    //게시글 삭제
    @DeleteMapping("/post/remove/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId){
        postService.deletePost(postId);
        return ResponseEntity.ok().build();
    }


    // 오늘 작성된 자유게시글 갯수 조회
    @GetMapping("/post/freeboard/count/today")
    public ResponseEntity<Long> getTodayFreeBoardCount() {
        long count = postService.getTodayFreeBoardCount();
        return ResponseEntity.ok(count);
    }

    // 최근 7일간 작성된 자유게시글 갯수 조회
    @GetMapping("/post/freeboard/count/recent-week")
    public ResponseEntity<Long> getRecentWeekFreeBoardCount() {
        long count = postService.getRecentWeekFreeBoardCount();
        return ResponseEntity.ok(count);
    }



//    /**
//     * 게시글 생성
//     * POST /api/posts
//     */
//    @PostMapping
//    public ResponseEntity<PostResponse> createPost(
//            @Valid @RequestBody PostCreateRequest request,
//            @RequestHeader("User-Id") Long userId) {
//        PostResponse post = communityService.createPost(request, userId);
//        return ResponseEntity.status(HttpStatus.CREATED).body(post);
//    }
//
//    /**
//     * 게시글 수정
//     * PUT /api/posts/1
//     */
//    @PutMapping("/{postId}")
//    public ResponseEntity<PostResponse> updatePost(
//            @PathVariable Long postId,
//            @Valid @RequestBody PostUpdateRequest request,
//            @RequestHeader("User-Id") Long userId) {
//        PostResponse post = communityService.updatePost(postId, request, userId);
//        return ResponseEntity.ok(post);
//    }
//
//    /**
//     * 게시글 삭제
//     * DELETE /api/posts/1
//     */
//    @DeleteMapping("/{postId}")
//    public ResponseEntity<Void> deletePost(
//            @PathVariable Long postId,
//            @RequestHeader("User-Id") Long userId) {
//        communityService.deletePost(postId, userId);
//        return ResponseEntity.noContent().build();
//    }

}