-- V20: wishlist 테이블에 생성시간/수정시간 컬럼 추가
-- 작성일: 2024-10-23
-- 목적: wishlist 테이블에 created_at, updated_at 컬럼을 추가하여 BaseTimeEntity 패턴 적용

-- ========================================
-- 1단계: wishlist 테이블에 created_at, updated_at 컬럼 추가
-- ========================================

ALTER TABLE `wishlist`
    ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '찜 등록일시'
    AFTER `lecture_id`;

ALTER TABLE `wishlist`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '찜 수정일시'
    AFTER `created_at`;

-- ========================================
-- 2단계: 성능 최적화를 위한 인덱스 추가
-- ========================================

-- 사용자별 최근 찜 목록 조회를 위한 복합 인덱스
CREATE INDEX `idx_wishlist_user_created`
    ON `wishlist` (`user_id`, `created_at` DESC);

-- 강의별 찜 등록 현황 조회를 위한 복합 인덱스
CREATE INDEX `idx_wishlist_lecture_created`
    ON `wishlist` (`lecture_id`, `created_at` DESC);

-- ========================================
-- 3단계: 마이그레이션 완료 확인 (주석)
-- ========================================

-- 테이블 구조 확인
-- DESCRIBE `wishlist`;

-- 인덱스 확인
-- SHOW INDEX FROM `wishlist`;

-- 사용자별 찜 목록 조회 (샘플 쿼리)
-- SELECT
--     w.id,
--     u.name as user_name,
--     u.nickname,
--     l.title as lecture_title,
--     w.created_at,
--     w.updated_at
-- FROM wishlist w
-- JOIN user u ON w.user_id = u.id
-- JOIN lecture l ON w.lecture_id = l.id
-- WHERE w.user_id = 1
-- ORDER BY w.created_at DESC;

-- 강의별 찜 통계 확인
-- SELECT
--     l.id as lecture_id,
--     l.title,
--     COUNT(w.id) as wishlist_count,
--     MAX(w.created_at) as latest_wishlist_at,
--     MIN(w.created_at) as earliest_wishlist_at
-- FROM lecture l
-- LEFT JOIN wishlist w ON l.id = w.lecture_id
-- GROUP BY l.id, l.title
-- HAVING wishlist_count > 0
-- ORDER BY wishlist_count DESC;

-- 일자별 찜 등록 추이 확인
-- SELECT
--     DATE(created_at) as date,
--     COUNT(*) as daily_wishlist_count
-- FROM wishlist
-- GROUP BY DATE(created_at)
-- ORDER BY date DESC
-- LIMIT 30;