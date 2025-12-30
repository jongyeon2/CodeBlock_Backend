package com.studyblock.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * - @Async 어노테이션을 사용한 비동기 메서드 실행
 * - 비디오 인코딩과 같은 무거운 작업을 백그라운드에서 처리
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비디오 인코딩용 스레드 풀
     * - 코어 스레드: 2개 (기본적으로 2개의 스레드 유지)
     * - 최대 스레드: 5개 (최대 5개까지 동시 인코딩)
     * - 큐 용량: 100 (대기열에 100개까지 작업 저장)
     */
    @Bean(name = "videoEncodingExecutor")
    public Executor videoEncodingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 코어 스레드 개수 (항상 유지)
        executor.setCorePoolSize(2);

        // 최대 스레드 개수
        executor.setMaxPoolSize(5);

        // 큐 용량 (대기 작업 수)
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("VideoEncoding-");

        // 거절 정책: 호출자 스레드에서 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 애플리케이션 종료 시 실행 중인 작업 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("비디오 인코딩용 스레드 풀 초기화 완료 - Core: {}, Max: {}, Queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 기본 비동기 Executor 설정
     */
    @Override
    public Executor getAsyncExecutor() {
        return videoEncodingExecutor();
    }
}