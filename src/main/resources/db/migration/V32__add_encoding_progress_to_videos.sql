-- V32: 비디오 인코딩 진행률 필드 추가
-- 목적: 인코딩 진행률을 DB에 영구 저장하여 SSE 재연결 시에도 진행률 복원 가능
-- 작성일: 2025-11-03

-- video 테이블에 encoding_progress 컬럼 추가
ALTER TABLE video
ADD COLUMN encoding_progress INT DEFAULT 0 COMMENT '인코딩 진행률 (0-100)';

-- preview_video 테이블에 encoding_progress 컬럼 추가
ALTER TABLE preview_video
ADD COLUMN encoding_progress INT DEFAULT 0 COMMENT '인코딩 진행률 (0-100)';

-- 기존 데이터 처리: COMPLETED 상태인 비디오는 100%로 설정
UPDATE video
SET encoding_progress = 100
WHERE encoding_status = 'COMPLETED';

UPDATE preview_video
SET encoding_progress = 100
WHERE encoding_status = 'COMPLETED';

-- 인덱스 추가 (선택사항: 진행 중인 비디오 조회 성능 향상)
CREATE INDEX idx_video_encoding_status_progress
ON video (encoding_status, encoding_progress);

CREATE INDEX idx_preview_video_encoding_status_progress
ON preview_video (encoding_status, encoding_progress);