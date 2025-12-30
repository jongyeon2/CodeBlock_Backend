package com.studyblock.domain.community.repository;

import com.studyblock.domain.community.entity.Comment;
import com.studyblock.domain.community.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    //댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.status = 'ACTIVE' ORDER BY c.createdAt ASC")
    List<Comment> findActiveByPostId(@Param("postId") Long postId);

    //댓글 조회 (모든 상태 포함, 관리자용)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);

    //댓글 개수 조회 ( 단일 게시글 )
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.status = 'ACTIVE'")
    int countActiveByPostId(@Param("postId") Long postId);

    //댓글 개수 조회 (여러 게시글)
   @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.status = 'ACTIVE' GROUP BY c.post.id")
   List<Object[]> countActiveByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 상태별 댓글 조회
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<Comment> findByStatus(@Param("status") ContentStatus status);

}