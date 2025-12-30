-- V11: 강의에 강사 정보 연결
-- 작성일: 2025-01-27
-- 목적: lecture 테이블에 instructor_id 컬럼 추가하여 강사와 강의 연결

-- ========================================
-- 1단계: lecture 테이블에 instructor_id 컬럼 추가
-- ========================================

ALTER TABLE `lecture`
    ADD COLUMN `instructor_id` BIGINT NULL
    COMMENT '강의를 담당하는 강사 ID'
    AFTER `course_id`;

-- ========================================
-- 2단계: 외래키 제약조건 추가
-- ========================================

ALTER TABLE `lecture`
    ADD CONSTRAINT `FK_lecture_TO_instructor_profile_1` 
    FOREIGN KEY (`instructor_id`) 
    REFERENCES `instructor_profile` (`id`) 
    ON DELETE SET NULL 
    ON UPDATE CASCADE;

-- ========================================
-- 3단계: 성능 최적화를 위한 인덱스 추가
-- ========================================

-- 강사별 강의 조회를 위한 인덱스
CREATE INDEX `idx_lecture_instructor_id` 
    ON `lecture` (`instructor_id`);

-- 강사 + 강의 상태 조합 인덱스 (강사별 활성 강의 조회용)
CREATE INDEX `idx_lecture_instructor_status` 
    ON `lecture` (`instructor_id`, `status`);

-- ========================================
-- 4단계: 마이그레이션 완료 확인 (주석)
-- ========================================

-- 테이블 구조 확인
-- DESCRIBE `lecture`;

-- 인덱스 확인
-- SHOW INDEX FROM `lecture`;

-- 강사별 강의 수 확인 (샘플 쿼리)
-- SELECT 
--     ip.id as instructor_id,
--     ip.channel_name,
--     COUNT(l.id) as lecture_count
-- FROM instructor_profile ip
-- LEFT JOIN lecture l ON ip.id = l.instructor_id
-- GROUP BY ip.id, ip.channel_name
-- ORDER BY lecture_count DESC;

-- ========================================
-- 5단계: 마이그레이션 완료 확인 (주석)
-- ========================================

-- 테이블 구조 확인
-- DESCRIBE `lecture`;

-- 인덱스 확인
-- SHOW INDEX FROM `lecture`;

-- 강사별 강의 수 확인 (샘플 쿼리)
-- SELECT 
--     ip.id as instructor_id,
--     ip.channel_name,
--     COUNT(l.id) as lecture_count
-- FROM instructor_profile ip
-- LEFT JOIN lecture l ON ip.id = l.instructor_id
-- GROUP BY ip.id, ip.channel_name
-- ORDER BY lecture_count DESC;

-- 전체 연결 관계 확인
-- SELECT 
--     u.name as user_name,
--     u.nickname,
--     ip.channel_name,
--     c.title as course_title,
--     l.title as lecture_title,
--     l.price_cookie,
--     l.is_free
-- FROM user u
-- JOIN instructor_profile ip ON u.id = ip.user_id
-- JOIN lecture l ON ip.id = l.instructor_id
-- JOIN course c ON l.course_id = c.id
-- ORDER BY u.name, l.sequence;
