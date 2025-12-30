-- V27: Video 테이블 1:1 관계 적용 및 PreviewVideo 테이블 생성 (호환성 개선 버전)
-- 작성일: 2025-10-31
-- 목적: Lecture-Video 관계를 1:N에서 1:1로 변경, 맛보기 강의 영상 테이블 추가
-- 개선: 인덱스 중복 제거, 컬럼 타입 일치, 안전성 강화

-- ============================================
-- Step 1: 기존 데이터 백업 (안전장치)
-- ============================================

CREATE TABLE IF NOT EXISTS video_backup_v27 AS
SELECT * FROM video;

-- ============================================
-- Step 2: Lecture당 여러 Video가 있는 경우 정리
-- ============================================

-- 임시 테이블 생성: Lecture별 첫 번째 Video ID 추적
CREATE TEMPORARY TABLE temp_first_videos AS
SELECT
    lecture_id,
    MIN(id) as first_video_id
FROM video
GROUP BY lecture_id;

-- Lecture당 두 번째 이후 비디오들을 별도 테이블로 이동
CREATE TABLE IF NOT EXISTS video_additional (
    id BIGINT PRIMARY KEY,
    lecture_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,                    -- ✅ V3 원본과 동일
    original_url VARCHAR(255),                     -- ✅ V17 이후 구조
    url_1080p VARCHAR(255),
    url_720p VARCHAR(255),
    url_540p VARCHAR(255),
    thumbnail_url VARCHAR(255),
    subtitle_url VARCHAR(255),
    resolution VARCHAR(20),                        -- ✅ V3 원본과 동일 (VARCHAR(20))
    file_size BIGINT,
    duration_seconds INT,
    encoding_status VARCHAR(20) NOT NULL,          -- ✅ ENUM → VARCHAR 변환
    created_at DATETIME NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    moved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_additional_lecture (lecture_id),
    INDEX idx_additional_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='1:1 마이그레이션으로 인해 이동된 추가 비디오';

-- ✅ 임시 컬럼 없이 직접 이동 (안전성 개선)
INSERT INTO video_additional (
    id, lecture_id, name, original_url, url_1080p, url_720p, url_540p,
    thumbnail_url, subtitle_url, resolution, file_size, duration_seconds,
    encoding_status, created_at
)
SELECT
    v.id, v.lecture_id, v.name, v.original_url, v.url_1080p, v.url_720p, v.url_540p,
    v.thumbnail_url, v.subtitle_url, v.resolution, v.file_size, v.duration_seconds,
    v.encoding_status, v.created_at
FROM video v
LEFT JOIN temp_first_videos tfv ON v.lecture_id = tfv.lecture_id AND v.id = tfv.first_video_id
WHERE tfv.first_video_id IS NULL;

-- 추가 비디오 삭제
DELETE v FROM video v
LEFT JOIN temp_first_videos tfv ON v.lecture_id = tfv.lecture_id AND v.id = tfv.first_video_id
WHERE tfv.first_video_id IS NULL;

-- 임시 테이블 정리
DROP TEMPORARY TABLE IF EXISTS temp_first_videos;

-- ============================================
-- Step 3: video 테이블 인덱스 정리 및 UNIQUE 제약조건 추가
-- ============================================

-- ✅ 1단계: 외래키 제약조건 제거 (인덱스 수정을 위해)
ALTER TABLE video DROP FOREIGN KEY FK_lecture_TO_video_1;

-- ✅ 2단계: 기존 인덱스 제거
ALTER TABLE video DROP INDEX idx_lecture_id;

-- ✅ 3단계: lecture_id에 UNIQUE 제약조건 추가 (1:1 관계 강제)
ALTER TABLE video
ADD CONSTRAINT uk_video_lecture_id UNIQUE (lecture_id);

-- ✅ 4단계: 복합 인덱스 추가 (조회 성능 개선)
CREATE INDEX idx_video_encoding_lecture ON video(encoding_status, lecture_id);

-- ✅ 5단계: 외래키 제약조건 재생성
ALTER TABLE video
ADD CONSTRAINT FK_lecture_TO_video_1 
    FOREIGN KEY (lecture_id) REFERENCES lecture(id) ON DELETE CASCADE;

-- ============================================
-- Step 4: preview_video 테이블 생성 (1:1 관계)
-- ============================================

CREATE TABLE preview_video (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL UNIQUE COMMENT '강의 ID (1:1 관계)',
    name VARCHAR(255) NOT NULL COMMENT '비디오 파일명',
    original_url VARCHAR(500) NOT NULL COMMENT 'S3 원본 비디오 URL',
    url_1080p VARCHAR(500) COMMENT '1080p 인코딩 URL',
    url_720p VARCHAR(500) COMMENT '720p 인코딩 URL',
    url_540p VARCHAR(500) COMMENT '540p 인코딩 URL',
    thumbnail_url VARCHAR(500) COMMENT '썸네일 URL',
    subtitle_url VARCHAR(500) COMMENT '자막 파일 URL',
    resolution VARCHAR(10) COMMENT '목표 해상도 (1080p, 720p, 540p)',
    file_size BIGINT COMMENT '파일 크기 (bytes)',
    duration_seconds INT DEFAULT 0 COMMENT '재생 시간 (초)',
    encoding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '인코딩 상태',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_preview_video_lecture
        FOREIGN KEY (lecture_id) REFERENCES lecture(id) ON DELETE CASCADE,

    INDEX idx_preview_video_encoding (encoding_status),
    INDEX idx_preview_video_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='맛보기 강의 영상 테이블 (Lecture 1:1)';

-- ============================================
-- Step 5: 마이그레이션 검증 쿼리
-- ============================================

-- 검증 1: Lecture당 Video가 1개씩만 있는지 확인
-- SELECT lecture_id, COUNT(*) as cnt
-- FROM video
-- GROUP BY lecture_id
-- HAVING cnt > 1;
-- 결과: 0 rows (정상)

-- 검증 2: 이동된 비디오 개수 확인
-- SELECT
--     (SELECT COUNT(*) FROM video) as remaining_videos,
--     (SELECT COUNT(*) FROM video_additional) as moved_videos,
--     (SELECT COUNT(*) FROM video_backup_v27) as total_original;

-- 검증 3: UNIQUE 제약조건 확인
-- SHOW INDEX FROM video WHERE Key_name = 'uk_video_lecture_id';

-- ============================================
-- 참고사항
-- ============================================

-- 1. video_backup_v27: 원본 데이터 백업 (롤백 시 사용)
-- 2. video_additional: 두 번째 이후 비디오 보관 (S3 URL 포함)
-- 3. S3 파일 정리: video_additional의 *_url 컬럼 참조하여 별도 배치 작업
-- 4. 인덱스 최적화: UNIQUE 인덱스가 단일 컬럼 검색을 자동 커버

-- ============================================
-- 롤백 스크립트 (긴급 상황용)
-- ============================================

-- ALTER TABLE video DROP INDEX idx_video_encoding_lecture;
-- ALTER TABLE video DROP CONSTRAINT uk_video_lecture_id;
-- ALTER TABLE video ADD INDEX idx_lecture_id (lecture_id);
-- INSERT INTO video
-- SELECT id, lecture_id, name, original_url, url_1080p, url_720p, url_540p,
--        thumbnail_url, subtitle_url, resolution, file_size, duration_seconds,
--        encoding_status, created_at, updated_at
-- FROM video_additional;
-- DROP TABLE preview_video;
-- DROP TABLE video_additional;
-- DROP TABLE video_backup_v27;