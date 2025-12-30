package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.VideoProgressDto;
import com.studyblock.domain.course.entity.Video;
import com.studyblock.domain.course.entity.VideoProgress;
import com.studyblock.domain.course.repository.VideoProgressRepository;
import com.studyblock.domain.course.repository.VideoRepository;
import com.studyblock.domain.enrollment.service.EnrollmentService;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * VideoProgress 서비스
 * - Redis + MySQL 하이브리드 방식
 * - 실시간 진도율은 Redis에 저장
 * - 주기적으로 MySQL에 동기화 (스케줄러)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProgressService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoProgressRepository videoProgressRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentService enrollmentService;

    private static final String REDIS_KEY_PREFIX = "video:progress:";
    private static final long REDIS_TTL_HOURS = 24;  // 24시간

    /**
     * 진도율 조회 (Redis → MySQL 순)
     * 1. Redis 조회
     * 2. Redis에 없으면 MySQL 조회 후 Redis 캐싱
     *
     * @param userId  사용자 ID
     * @param videoId 비디오 ID
     * @return 진도율 DTO (없으면 null)
     */
    public VideoProgressDto getProgress(Long userId, Long videoId) {
        // 1. Redis 조회
        String redisKey = getRedisKey(userId, videoId);
        VideoProgressDto dto = (VideoProgressDto) redisTemplate.opsForValue().get(redisKey);

        if (dto != null) {
            log.debug("Redis에서 진도율 조회 - userId: {}, videoId: {}", userId, videoId);
            return dto;
        }

        // 2. MySQL 조회
        log.debug("Redis 미스 - MySQL 조회 - userId: {}, videoId: {}", userId, videoId);
        return videoProgressRepository.findByUserIdAndVideoId(userId, videoId)
                .map(progress -> {
                    VideoProgressDto mysqlDto = VideoProgressDto.builder()
                            .userId(userId)
                            .videoId(videoId)
                            .position(progress.getLastPosition())
                            .duration(progress.getDuration())
                            .updatedAt(progress.getUpdatedAt())
                            .build();

                    // 3. MySQL 데이터를 Redis에 캐싱
                    cacheToRedis(userId, videoId, mysqlDto);
                    log.info("MySQL 데이터 Redis 캐싱 완료 - userId: {}, videoId: {}", userId, videoId);

                    return mysqlDto;
                })
                .orElse(null);
    }

    /**
     * Redis에만 진도율 저장 (실시간 저장)
     * - TTL 24시간
     *
     * @param userId   사용자 ID
     * @param videoId  비디오 ID
     * @param position 시청 위치 (초)
     * @param duration 전체 길이 (초)
     */
    public void saveProgressToRedis(Long userId, Long videoId, Integer position, Integer duration) {
        if (position == null || duration == null || duration <= 0) {
            log.warn("Redis 진도율 저장 스킵 - 잘못된 데이터 - userId: {}, videoId: {}, position: {}, duration: {}",
                    userId, videoId, position, duration);
            return;
        }

        VideoProgressDto dto = VideoProgressDto.builder()
                .userId(userId)
                .videoId(videoId)
                .position(position)
                .duration(duration)
                .updatedAt(LocalDateTime.now())
                .build();

        String redisKey = getRedisKey(userId, videoId);
        redisTemplate.opsForValue().set(redisKey, dto, REDIS_TTL_HOURS, TimeUnit.HOURS);

        log.info("Redis 진도율 저장 완료 - userId: {}, videoId: {}, position: {}, duration: {}, TTL: {}h",
                userId, videoId, position, duration, REDIS_TTL_HOURS);
    }

    /**
     * Redis → MySQL 영구 저장
     * - Redis 데이터를 가져와서 MySQL에 저장
     * - 기존 데이터 있으면 UPDATE, 없으면 INSERT
     *
     * @param userId  사용자 ID
     * @param videoId 비디오 ID
     */
    @Transactional
    public void persistToDatabase(Long userId, Long videoId, Integer position, Integer duration) {
        // 1. Redis에서 데이터 조회
        String redisKey = getRedisKey(userId, videoId);
        VideoProgressDto dto = (VideoProgressDto) redisTemplate.opsForValue().get(redisKey);

        // 요청으로 전달된 위치/길이를 우선 저장하여 최신 상태를 보장
        if (position != null && duration != null) {
            saveProgressToRedis(userId, videoId, position, duration);
            dto = VideoProgressDto.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .position(position)
                    .duration(duration)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        if (dto == null) {
            log.warn("Redis에 데이터 없음 - 영구 저장 스킵 - userId: {}, videoId: {}", userId, videoId);
            return;
        }

        // 2. User, Video 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다. ID: " + videoId));

        // 3. 기존 데이터 조회
        VideoProgress progress = videoProgressRepository.findByUserIdAndVideoId(userId, videoId)
                .orElse(null);

        if (progress == null) {
            // INSERT
            progress = VideoProgress.builder()
                    .user(user)
                    .video(video)
                    .lastPosition(dto.getPosition())
                    .duration(dto.getDuration())
                    .isCompleted(dto.isCompleted())
                    .build();
            log.info("새 진도율 MySQL 저장 - userId: {}, videoId: {}, position: {}", userId, videoId, dto.getPosition());
        } else {
            // UPDATE
            progress.updateDuration(dto.getDuration());
            progress.updatePosition(dto.getPosition());
            if (dto.isCompleted()) {
                progress.complete();
            }
            log.info("진도율 MySQL 업데이트 - userId: {}, videoId: {}, position: {}", userId, videoId, dto.getPosition());
        }

        videoProgressRepository.save(progress);

        // 수강 진도율 업데이트
        try {
            BigDecimal watchPercentage = BigDecimal
                    .valueOf(dto.getProgressPercent())
                    .setScale(2, RoundingMode.HALF_UP);
            int timeSpentSeconds = dto.getPosition() != null ? dto.getPosition() : 0;
            enrollmentService.updateVideoProgress(
                    userId,
                    video.getLecture().getId(),
                    watchPercentage,
                    timeSpentSeconds
            );
        } catch (Exception e) {
            log.warn("수강 진도율 업데이트 실패 - userId: {}, videoId: {}", userId, videoId, e);
        }
    }

    /**
     * Redis 키 생성
     *
     * @param userId  사용자 ID
     * @param videoId 비디오 ID
     * @return Redis 키 (video:progress:{userId}:{videoId})
     */
    private String getRedisKey(Long userId, Long videoId) {
        return REDIS_KEY_PREFIX + userId + ":" + videoId;
    }

    /**
     * MySQL 데이터를 Redis에 캐싱
     *
     * @param userId  사용자 ID
     * @param videoId 비디오 ID
     * @param dto     진도율 DTO
     */
    private void cacheToRedis(Long userId, Long videoId, VideoProgressDto dto) {
        String redisKey = getRedisKey(userId, videoId);
        redisTemplate.opsForValue().set(redisKey, dto, REDIS_TTL_HOURS, TimeUnit.HOURS);
    }
}