-- V17: 비디오 다중 해상도 URL 지원 추가
-- 설명: FFmpeg 인코딩을 위한 여러 해상도 URL 필드 추가 (1080p, 720p, 540p)

-- 1. 새로운 해상도별 URL 컬럼 추가
ALTER TABLE video
ADD COLUMN original_url VARCHAR(255) NULL COMMENT '원본 영상 URL' AFTER video_url,
ADD COLUMN url_1080p VARCHAR(255) NULL COMMENT '1080p 인코딩 영상 URL' AFTER original_url,
ADD COLUMN url_720p VARCHAR(255) NULL COMMENT '720p 인코딩 영상 URL' AFTER url_1080p,
ADD COLUMN url_540p VARCHAR(255) NULL COMMENT '540p 인코딩 영상 URL' AFTER url_720p;

-- 2. 기존 데이터 마이그레이션
-- 기존 video_url 값을 original_url로 복사 (기존 데이터 보존)
UPDATE video
SET original_url = video_url
WHERE original_url IS NULL;

-- 3. 기존 video_url 컬럼 삭제
ALTER TABLE video
DROP COLUMN video_url;

-- 4. resolution 컬럼은 유지 (메타데이터로 활용 가능)
-- 필요시 주석 해제하여 삭제 가능
-- ALTER TABLE video DROP COLUMN resolution;