package com.studyblock.domain.admin.repository;

import com.studyblock.domain.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPostRepository extends JpaRepository<Post, Long> {

    boolean existsByTitleAndBoardId(String title, Long boardId);

    @Query("SELECT p FROM Post p WHERE p.board.type = :type")
    List<Post> findByBoardType(@Param("type") Integer type);
}

