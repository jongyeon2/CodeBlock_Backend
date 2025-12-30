package com.studyblock.domain.course.scheduler;

import com.studyblock.domain.course.dto.VideoProgressDto;
import com.studyblock.domain.course.service.VideoProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VideoProgress 스케줄러
 * - 5분마다 Redis → MySQL 동기화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProgressScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoProgressService videoProgressService;

    private static final String REDIS_KEY_PATTERN = "video:progress:*";

    /**
     * Redis → MySQL 동기화 (5분마다 실행)
     * - fixedDelay: 이전 실행 완료 후 5분 뒤 다시 실행
     */
    @Scheduled(fixedDelay = 300000)  // 5분 = 300,000ms
    public void syncRedisToDatabase() {
        log.info("Redis → MySQL 동기화 시작");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            // 1. Redis에서 모든 video:progress:* 키 조회
            Set<String> keys = redisTemplate.keys(REDIS_KEY_PATTERN);

            if (keys == null || keys.isEmpty()) {
                log.info("동기화할 Redis 데이터 없음");
                return;
            }

            log.info("동기화할 Redis 키 개수: {}", keys.size());

            // 2. 각 키마다 동기화 실행
            for (String key : keys) {
                try {
                    // 키 파싱: video:progress:{userId}:{videoId}
                    String[] parts = key.split(":");
                    if (parts.length != 4) {
                        log.warn("잘못된 Redis 키 형식 - key: {}", key);
                        failCount.incrementAndGet();
                        continue;
                    }

                    Long userId = Long.parseLong(parts[2]);
                    Long videoId = Long.parseLong(parts[3]);

                    // 3. MySQL에 영구 저장
                    videoProgressService.persistToDatabase(userId, videoId, null, null);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("개별 키 동기화 실패 - key: {}", key, e);
                    failCount.incrementAndGet();
                }
            }

        } catch (Exception e) {
            log.error("Redis → MySQL 동기화 중 오류 발생", e);
        }

        log.info("Redis → MySQL 동기화 완료 - 성공: {}, 실패: {}", successCount.get(), failCount.get());
    }
}