-- V18: 비디오 시청 진도율 테이블 생성

CREATE TABLE video_progress (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    video_id BIGINT NOT NULL COMMENT '비디오 ID',
    last_position INT NOT NULL DEFAULT 0 COMMENT '마지막 시청 시점(초)',
    duration INT NULL COMMENT '전체 영상 길이(초)',
    is_completed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '완료 여부 (90% 이상 시청 시 자동 완료)',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트 시간',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_video (user_id, video_id),
    CONSTRAINT fk_video_progress_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_video_progress_video FOREIGN KEY (video_id) REFERENCES video (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='비디오 시청 진도율';

-- 인덱스 추가 (조회 성능 최적화)
CREATE INDEX idx_user_updated_at ON video_progress (user_id, updated_at DESC);
CREATE INDEX idx_video_updated_at ON video_progress (video_id, updated_at DESC);