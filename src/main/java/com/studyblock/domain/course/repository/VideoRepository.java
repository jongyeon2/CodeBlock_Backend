package com.studyblock.domain.course.repository;

import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.enums.EncodingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * 특정 강의(Lecture)의 모든 비디오 조회
     */
    List<Video> findByLectureId(Long lectureId);

    /**
     * 여러 강의 ID로 비디오 목록 조회
     * - N+1 문제 방지를 위해 IN 절 사용
     * - 일괄 조회로 성능 최적화
     * @param lectureIds 강의 ID 목록
     * @return 비디오 목록
     */
    List<Video> findByLectureIdIn(List<Long> lectureIds);

    /**
     * 특정 강의(Lecture)의 비디오 조회 (페이징 지원)
     */
    Page<Video> findByLectureId(Long lectureId, Pageable pageable);

    /**
     * 특정 강의의 인코딩 완료된 비디오만 조회
     */
    List<Video> findByLectureIdAndEncodingStatus(Long lectureId, EncodingStatus encodingStatus);

    /**
     * 인코딩 상태별 비디오 조회 (관리자용)
     */
    List<Video> findByEncodingStatus(EncodingStatus encodingStatus);

    /**
     * 특정 강의의 비디오 개수 조회
     */
    long countByLectureId(Long lectureId);

    /**
     * 비디오 URL로 비디오 조회
     */
    Optional<Video> findByOriginalUrl(String videoUrl);

    /**
     * Video ID로 조회 (Lecture Fetch Join)
     * - N+1 문제 방지를 위해 Lecture를 함께 조회
     * @param videoId 비디오 ID
     * @return Video (Lecture 포함)
     */
    @Query("SELECT v FROM Video v LEFT JOIN FETCH v.lecture WHERE v.id = :videoId")
    Optional<Video> findByIdWithLecture(@Param("videoId") Long videoId);

    /**
     * 특정 강의의 인코딩 완료된 비디오 개수
     */
    @Query("SELECT COUNT(v) FROM Video v WHERE v.lecture.id = :lectureId AND v.encodingStatus = :status")
    long countByLectureIdAndEncodingStatus(@Param("lectureId") Long lectureId,
                                           @Param("status") EncodingStatus status);

    /**
     * 특정 강의의 총 비디오 재생 시간(초) 계산
     */
    @Query("SELECT COALESCE(SUM(v.durationSeconds), 0) FROM Video v WHERE v.lecture.id = :lectureId AND v.encodingStatus = 'COMPLETED'")
    int getTotalDurationByLectureId(@Param("lectureId") Long lectureId);

    /**
     * 특정 강의의 마지막 업로드된 비디오 조회 (최신순)
     */
    @Query("SELECT v FROM Video v WHERE v.lecture.id = :lectureId ORDER BY v.createdAt DESC LIMIT 1")
    Optional<Video> findLatestByLectureId(@Param("lectureId") Long lectureId);
}