package com.studyblock.domain.community.repository;

import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.enums.ContentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ============ 기본 조회 ============

    /**
     * 게시글 ID로 활성 상태인 게시글 조회 (삭제되지 않은 것만)
     */
    Post findByIdAndStatus(Long id, ContentStatus status);

    /**
     * 게시글 ID로 활성 상태인 게시글 조회 (편의 메서드)
     */
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.status = 'ACTIVE'")
    Optional<Post> findActiveById(@Param("id") Long id);

    // 전체 게시물 조회
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.board LEFT JOIN FETCH p.user WHERE p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Post> findAllActive();

    // 커뮤니티 홈 -> 전체 게시물 조회 ( 자유게시판(2) + 공지(1)만 )
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.board LEFT JOIN FETCH p.user WHERE p.status = 'ACTIVE' AND p.board.id IN (1, 2) ORDER BY p.createdAt DESC")
    List<Post> findAllActiveInBoards1And2();

    //사용자 게시글 내역 (활성화된 글들, 게시판 별, 최신순)
    @Query("SELECT p FROM Post p WHERE p.user.id = :userId AND p.board.id = :boardId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Post> findActiveByUserIdAndBoardIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("boardId") Long boardId);

    // 상태에 따른 post 개수
    long countByStatus(ContentStatus status);

    // ============ Board 기반 조회 ============
    /**
     * 특정 게시판의 활성 게시글 목록 (편의 메서드)
     */
    @Query("SELECT p FROM Post p WHERE p.board.id = :boardId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Post> findActiveByBoardId(@Param("boardId") Long boardId);

    /**
     * 특정 게시판의 모든 게시글 목록 (차단된 것 포함, 관리자용)
     */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.board LEFT JOIN FETCH p.user WHERE p.board.id = :boardId ORDER BY p.createdAt DESC")
    List<Post> findAllByBoardId(@Param("boardId") Long boardId);

    /**
     * 특정 게시판의 게시글 수 조회
     */
    long countByBoardIdAndStatus(Long boardId, ContentStatus status);

    // 자유게시판 인기 게시글 topN(조회수가 가장 높은 게시글 N개 조회하는 메서드)
    @Query("""
        SELECT p FROM Post p 
            WHERE p.board.id = :boardId
                AND p.status = 'ACTIVE'
                AND p.createdAt >= :since
            ORDER BY p.hit DESC
    """)
    List<Post> findTopNByBoardIdAndCreatedAtAfterOrderByHitDesc(
            @Param("boardId") Long boardId,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    // 오늘 작성된 자유게시글 개수 카운트
    @Query("""
    SELECT COUNT(p) FROM Post p 
    WHERE p.board.id = :boardId 
        AND p.status = 'ACTIVE' 
        AND DATE(p.createdAt) = CURRENT_DATE
    """)
    long countTodayPostsByBoardId(@Param("boardId") Long boardId);

    // 최근 7일간 작성된 자유게시글 갯수 카운트 (오늘 기준)
    @Query("""
    SELECT COUNT(p) FROM Post p 
    WHERE p.board.id = :boardId 
        AND p.status = 'ACTIVE' 
        AND p.createdAt >= :startDate
    """)
    long countRecentPostsByBoardId(
            @Param("boardId") Long boardId,
            @Param("startDate") LocalDateTime startDate
    );

    // ============ User 기반 조회 ============


    /**
     * 특정 사용자가 작성한 게시글 수
     */
    long countByUserIdAndStatus(Long userId, ContentStatus status);

    /**
     * 특정 사용자의 모든 게시글 (삭제된 것 포함)
     */
    List<Post> findByUserId(Long userId);

    //특정 사용자의 모든 게시글 ( 활성화 된 것들만 )
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.board LEFT JOIN FETCH p.user WHERE p.user.id = :userId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
   List<Post> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 상태별 게시글 조회
     */
    List<Post> findByStatus(ContentStatus status);

}