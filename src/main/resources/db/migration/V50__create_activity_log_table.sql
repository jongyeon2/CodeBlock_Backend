-- V50: activity_log 테이블 생성
-- 작성일: 2025-11-07
-- 목적: 유저 활동 로그 저장을 위한 테이블 생성

CREATE TABLE activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '활동 로그 ID',
    
    -- 사용자 정보
    user_id BIGINT NOT NULL COMMENT '활동을 수행한 사용자 ID',
    
    -- 액션 정보
    action_type VARCHAR(50) NOT NULL COMMENT '액션 타입 (SIGNUP, COOKIE_CHARGE, COURSE_PURCHASE, REPORT, LOGIN, LOGOUT, COURSE_REVIEW)',
    target_type VARCHAR(50) NULL COMMENT '대상 타입 (USER, COURSE, ORDER, POST, COMMENT 등)',
    target_id BIGINT NULL COMMENT '대상 ID (Course ID, Order ID, Post ID 등)',
    
    -- 상세 정보
    description TEXT NULL COMMENT '활동 설명 (사람이 읽을 수 있는 형태)',
    ip_address VARCHAR(45) NULL COMMENT 'IP 주소 (IPv4: 15자, IPv6: 45자)',
    metadata JSON NULL COMMENT '추가 메타데이터 (JSON 형식)',
    
    -- 시간 정보
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그 생성 시간',
    
    -- 인덱스
    INDEX idx_user_id (user_id) COMMENT '사용자별 로그 조회',
    INDEX idx_action_type (action_type) COMMENT '액션 타입별 로그 조회',
    INDEX idx_created_at (created_at) COMMENT '시간순 로그 조회',
    INDEX idx_user_action (user_id, action_type) COMMENT '사용자+액션 조합 조회',
    INDEX idx_target (target_type, target_id) COMMENT '대상별 로그 조회',
    
    -- 외래키
    CONSTRAINT fk_activity_log_user FOREIGN KEY (user_id) 
        REFERENCES user(id) ON DELETE CASCADE
        
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 활동 로그 테이블';

-- 활동 로그 테이블 설명
-- 1. action_type: ENUM 대신 VARCHAR 사용 (확장성)
-- 2. metadata: JSON 타입으로 유연한 메타데이터 저장
-- 3. ip_address: VARCHAR(45)로 IPv6 지원
-- 4. 복합 인덱스로 자주 사용되는 쿼리 최적화
-- 5. ON DELETE CASCADE로 사용자 삭제 시 로그도 함께 삭제