package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.VideoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * VideoProgress Repository
 */
@Repository
public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {

    /**
     * 사용자 ID와 비디오 ID로 진도율 조회
     *
     * @param userId  사용자 ID
     * @param videoId 비디오 ID
     * @return 진도율 (Optional)
     */
    Optional<VideoProgress> findByUserIdAndVideoId(Long userId, Long videoId);

    /**
     * 사용자 ID로 진도율 목록 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @return 진도율 목록
     */
    List<VideoProgress> findByUserIdOrderByUpdatedAtDesc(Long userId);
}