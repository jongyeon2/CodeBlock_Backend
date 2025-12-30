package com.studyblock.domain.community.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.community.dto.CommentResponse;
import com.studyblock.domain.community.dto.PostCreateRequest;
import com.studyblock.domain.community.dto.PostListResponse;
import com.studyblock.domain.community.dto.PostResponse;
import com.studyblock.domain.community.entity.Board;
import com.studyblock.domain.community.entity.Comment;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.enums.ContentStatus;
import com.studyblock.domain.community.repository.BoardRepository;
import com.studyblock.domain.community.repository.CommentRepository;
import com.studyblock.domain.community.repository.PostRepository;
import com.studyblock.domain.upload.dto.ImageUploadResponse;
import com.studyblock.domain.upload.enums.ImageType;
import com.studyblock.domain.upload.service.ImageUploadService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.infrastructure.storage.S3StorageService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostService {

    // Community - Post 서비스
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ImageUploadService imageUploadService;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final S3StorageService s3StorageService;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       RedisTemplate<String, Object> redisTemplate,
                       ImageUploadService imageUploadService,
                       BoardRepository boardRepository,
                       UserRepository userRepository,
                       ObjectMapper objectMapper, S3StorageService s3StorageService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.redisTemplate = redisTemplate;
        this.imageUploadService = imageUploadService;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.s3StorageService = s3StorageService;
    }

    /** 게시글 작성 */
    public PostResponse createPost(Long userId, Long boardId, PostCreateRequest request, List<MultipartFile> imageFiles) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 게시판 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        // 이미지 업로드
        List<String> imageUrls = new ArrayList<>();
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile imageFile : imageFiles) {
                if (!imageFile.isEmpty()) {
                    ImageUploadResponse uploadResponse = imageUploadService.uploadImage(imageFile, ImageType.POST);
                    if (!uploadResponse.getSuccess()) {
                        throw new RuntimeException("이미지 업로드 실패: " + uploadResponse.getMessage());
                    }
                    imageUrls.add(uploadResponse.getUrl());
                }
            }
        }

        // URL 리스트 JSON 변환
        String imageUrlJson = null;
        try {
            if (!imageUrls.isEmpty()) {
                imageUrlJson = objectMapper.writeValueAsString(imageUrls);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이미지 URL JSON 변환 실패", e);
        }

        // 게시글 생성
        Post post = Post.builder()
                .board(board)
                .user(user)
                .title(request.getTitle())
                .originalContent(request.getOriginalContent())
                .imageUrl(imageUrlJson)
                .build();

        // 저장
        postRepository.save(post);

        // 응답 반환
        int commentCount = commentRepository.countActiveByPostId(post.getId());
        return PostResponse.from(post, commentCount);
    }

    /** Presigned URL 변환 (게시글 이미지 + 작성자 프로필 이미지) */
    private PostResponse applyPresignedUrls(PostResponse response) {
        if (response == null) return response;

//        List<String> signedUrls = response.getImageUrls().stream()
//                .map(url -> {
//                    try {
//                        return s3StorageService.generatePresignedUrl(url, 30); // 30분 유효
//                    } catch (RuntimeException e) {
//                        return url; // presigned 실패 시 원본 URL 유지
//                    }
//                })
//                .collect(Collectors.toList());
        // 게시글 이미지 url에 presigned url 적용
        if(response.getImageUrls() != null && !response.getImageUrls().isEmpty()){
            List<String> signedUrls = response.getImageUrls().stream()
                    .map(url -> {
                        try {
                            return s3StorageService.generatePresignedUrl(url, 30);
                        } catch (RuntimeException e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toList());
            response.setImageUrls(signedUrls);
        }
        //작성자 프로필 이미지에 presigned url 적용
        if(response.getUserProfileImage() != null && !response.getUserProfileImage().isEmpty()){
            try {
                String presignedProfileUrl = s3StorageService.generatePresignedUrl(response.getUserProfileImage(), 30);
                response.setUserProfileImage(presignedProfileUrl);
            } catch (RuntimeException e) {

            }
        }
        return response;
    }

    /** 게시글 삭제 */
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        post.delete();
    }

    /** 게시글 수정 */
    public PostResponse updatePost(Long postId, Long userId, PostCreateRequest request, List<MultipartFile> imageFiles) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        // 제목, 내용 수정
        post.setTitle(request.getTitle());
        post.setEditedContent(request.getOriginalContent());
        post.setUpdatedAt(request.getUpdatedAt());
        post.setIsEdited(true);

        // 기존 이미지 URL 불러오기
        List<String> existingUrls = new ArrayList<>();
        if (post.getImageUrl() != null) {
            try {
                existingUrls = objectMapper.readValue(post.getImageUrl(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("기존 이미지 URL 파싱 실패", e);
            }
        }

        // 남겨둔 이미지 반영
        if (request.getRemainingImageUrls() != null) {
            existingUrls = request.getRemainingImageUrls();
        }

        // 새 이미지 추가
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile imageFile : imageFiles) {
                if (!imageFile.isEmpty()) {
                    ImageUploadResponse uploadResponse = imageUploadService.uploadImage(imageFile, ImageType.POST);
                    if (!uploadResponse.getSuccess()) {
                        throw new RuntimeException("이미지 업로드 실패: " + uploadResponse.getMessage());
                    }
                    existingUrls.add(uploadResponse.getUrl());
                }
            }
        }

        // 다시 JSON으로 변환
        try {
            String imageUrlJson = objectMapper.writeValueAsString(existingUrls);
            post.setImageUrl(imageUrlJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이미지 URL JSON 변환 실패", e);
        }

        postRepository.save(post);
        int commentCount = commentRepository.countActiveByPostId(post.getId());
        return PostResponse.from(post, commentCount);
    }

    /** 게시판별 게시글 조회 */
    public List<PostResponse> getPostByCategory(Long boardId) {
        List<Post> posts = postRepository.findActiveByBoardId(boardId);
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Integer> commentCountMap = commentRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));

        return posts.stream()
                .map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0)))
                .map(this::applyPresignedUrls)
                .toList();
    }

    //게시글 조회 (모든 상태 포함, 관리자용)
    public List<PostResponse> getPostByCategoryForAdmin(Long boardId) {
        List<Post> posts = postRepository.findAllByBoardId(boardId);
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> commentCountMap = commentRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));
        return posts.stream()
                .map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0)))
                .collect(Collectors.toList());
    }

    /** 댓글 조회 */
    public List<CommentResponse> getPostComment(Long postId) {
        List<Comment> comments = commentRepository.findActiveByPostId(postId);
        return comments.stream()
                .map(CommentResponse::from)
                .map(this::applyPresignedUrlsToComment)
                .toList();
    }
    //댓글 사용자 프로필 이미지에 presigned URL 적용
    private CommentResponse applyPresignedUrlsToComment(CommentResponse response){
        if(response == null || response.getUserImageUrl() == null || response.getUserImageUrl().isEmpty()){
            return response;
        }
        try {
            String presignedUrl = s3StorageService.generatePresignedUrl(response.getUserImageUrl(), 30);
            response.setUserImageUrl(presignedUrl);
        } catch (RuntimeException e) {
            //생성 실패 시 원본 유지
        }
        return response;
    }

    //댓글 조회 (모든 상태 포함, 관리자용)
    public List<CommentResponse> getPostCommentForAdmin(Long postId){
        List<Comment> comments = commentRepository.findAllByPostId(postId);
        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    /** 상태별 게시글 수 */
    public long countActivePost(ContentStatus status) {
        return postRepository.countByStatus(status);
    }

    /** 전체 활성 게시글 */
    public PostListResponse getAllActivePosts() {
        List<Post> posts = postRepository.findAllActive();
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Integer> commentCountMap = commentRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));

        List<PostResponse> postResponses = posts.stream()
                .map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0)))
                .map(this::applyPresignedUrls)
                .toList();

        long totalCount = postRepository.countByStatus(ContentStatus.ACTIVE);
        return new PostListResponse(postResponses, totalCount);
    }

    /** FAQ 제외(공지+자유) 활성 게시글 */
    public PostListResponse getAllActivePosts2() {
        List<Post> posts = postRepository.findAllActiveInBoards1And2();
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Integer> commentCountMap = commentRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));

        List<PostResponse> postResponses = posts.stream()
                .map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0)))
                .map(this::applyPresignedUrls)
                .toList();

        long totalCount = postRepository.countByStatus(ContentStatus.ACTIVE);
        return new PostListResponse(postResponses, totalCount);
    }

    /** 조회수 증가 (24시간 캐시) */
    public void increaseViewCount(Long postId, Long userId) {
        String redisKey = "post:view:" + postId + ":user:" + userId;

        Boolean hasViewed = redisTemplate.hasKey(redisKey);
        if (!Boolean.TRUE.equals(hasViewed)) {
            Post post = postRepository.findActiveById(postId)
                    .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

            post.incrementHits();
            postRepository.save(post);

            redisTemplate.opsForValue().set(redisKey, "viewed", Duration.ofHours(24));
        }
    }

    /** 게시글 단건 조회 */
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdAndStatus(postId, ContentStatus.ACTIVE);
        int commentCount = commentRepository.countActiveByPostId(postId);

        PostResponse response = PostResponse.from(post, commentCount);
        return applyPresignedUrls(response);
    }


    //자유게시판 최근 1개월 기준 인기 게시글 Top10 조회
    public List<PostResponse> getTop10PopularFreeBoardPosts(int limit){

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1); //1달 전 날짜 계산

        Pageable pageable = PageRequest.of(0, limit); // 개수 설정을 위한 페이징 설정

        List<Post> posts = postRepository.findTopNByBoardIdAndCreatedAtAfterOrderByHitDesc(
                2L, //게시물 id
                oneMonthAgo, //1개월 전
                pageable //최대 개수 제한
        );

        // 게시물 id 리스트 출력
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        // 게시물의 댓글 수 조회
        Map<Long, Integer> commentCountMap =
                commentRepository.countActiveByPostIds(postIds).stream().
                        collect(Collectors.toMap(arr -> (Long) arr[0], arr -> ((Long) arr[1]).intValue()));

        // PostResponse로 변환하여 return
        return posts.stream()
                .map(post -> PostResponse.from(
                        post,
                        commentCountMap.getOrDefault(post.getId(), 0)
                ))
                .map(this::applyPresignedUrls)
                .collect(Collectors.toList());
    }

    // 오늘 작성된 자유게시글 갯수
    public long getTodayFreeBoardCount() {
        return postRepository.countTodayPostsByBoardId(2L);
    }

    // 최근 7일간 작성된 자유게시글 갯수 (오늘 포함)
    public long getRecentWeekFreeBoardCount() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return postRepository.countRecentPostsByBoardId(2L, sevenDaysAgo);
    }



    // 게시글 상태 변경 (차단/해제)
    public void changePostStatus(Long postId, String status) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID=" + postId));

        try {
            ContentStatus contentStatus = ContentStatus.valueOf(status.toUpperCase());
            if (contentStatus == ContentStatus.DELETED) {
                post.delete();
            } else if (contentStatus == ContentStatus.ACTIVE) {
                post.restore();
            } else {
                throw new IllegalArgumentException("유효하지 않은 게시글 상태입니다: " + status);
            }
            postRepository.save(post);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 게시글 상태입니다: " + status);
        }
    }

    // 차단된 게시글 조회 (status = DELETED)
    public List<PostResponse> getBlockedPosts() {
        List<Post> blockedPosts = postRepository.findByStatus(ContentStatus.DELETED);
        
        // 게시글 ID 목록 추출
        List<Long> postIds = blockedPosts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());
        
        // 각 게시글의 댓글 수 조회
        Map<Long, Integer> commentCountMap = commentRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));
        
        return blockedPosts.stream()
                .map(post -> PostResponse.from(post, commentCountMap.getOrDefault(post.getId(), 0)))
                .map(this::applyPresignedUrls)
                .collect(Collectors.toList());
    }
}
