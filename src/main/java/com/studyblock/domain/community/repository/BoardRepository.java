package com.studyblock.domain.community.repository;

import com.studyblock.domain.community.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Board findByType(int value);
}
