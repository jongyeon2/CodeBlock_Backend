-- V47: report 테이블의 target_type ENUM에 VIDEO, COURSE 추가
-- 동영상 신고 및 강의 신고 기능을 위한 마이그레이션

-- target_type ENUM에 VIDEO, COURSE 추가
ALTER TABLE report
MODIFY COLUMN target_type ENUM('POST', 'COMMENT', 'USER', 'VIDEO', 'COURSE') NOT NULL COMMENT '신고 유형 (POST: 게시글, COMMENT: 댓글, USER: 유저, VIDEO: 동영상, COURSE: 강의)';

