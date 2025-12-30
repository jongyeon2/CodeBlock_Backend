package com.studyblock.domain.community.service;

import com.studyblock.domain.community.dto.CommentRequest;
import com.studyblock.domain.community.dto.CommentResponse;
import com.studyblock.domain.community.entity.Comment;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.enums.ContentStatus;
import com.studyblock.domain.community.repository.CommentRepository;
import com.studyblock.domain.community.repository.PostRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    //댓글 + 대댓글 작성
    public void createComment(Long postId, Long userId, CommentRequest commentRequest){

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 부모 댓글 조회 (대댓글인 경우만)
        Comment parentComment = null;

        // parentId가 null이 아닐 때만 부모 댓글 조회
        if (commentRequest.getParentCommentId() != null) {
            parentComment = commentRepository.findById(commentRequest.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        }

        // 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(commentRequest.getContent())
                .parentComment(parentComment)
                .build();

        commentRepository.save(comment); //댓글 저장
        //양방향 관계 동기화
        if(parentComment != null){
            parentComment.addReply(comment);
        }
    }

    //댓글 삭제 (status만 변경)
    public void deleteComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        comment.delete();
        commentRepository.save(comment); // 변경사항 저장
    }

    // 차단된 댓글 조회 (status = DELETED)
    public List<CommentResponse> getBlockedComments() {
        List<Comment> blockedComments = commentRepository.findByStatus(ContentStatus.DELETED);
        return blockedComments.stream()
                .map(comment -> {
                    CommentResponse response = CommentResponse.from(comment);
                    // 추가 필드 설정
                    response.setPostTitle(comment.getPost().getTitle());
                    response.setUserName(comment.getUser().getName());
                    response.setUserNickname(comment.getUser().getNickname());
                    return response;
                })
                .collect(Collectors.toList());
    }
}
