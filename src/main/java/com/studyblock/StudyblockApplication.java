package com.studyblock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 스케줄러 활성화 (VideoProgressScheduler)
public class StudyblockApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyblockApplication.class, args);
    }

}