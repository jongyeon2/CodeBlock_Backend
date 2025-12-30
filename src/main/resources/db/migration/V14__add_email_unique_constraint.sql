-- ============================================
-- V14: Collation 통일 및 이메일 통합 정책 적용
-- 작성일: 2025-10-17
-- 목적:
--   1. 모든 VARCHAR 컬럼을 utf8mb4_0900_ai_ci로 통일 (MySQL 8.0 최신 표준)
--   2. 1 이메일 = 1 계정 정책 구현
-- ============================================

-- ========================================
-- 1단계: user 테이블의 모든 VARCHAR 컬럼을 utf8mb4_0900_ai_ci로 변경
-- ========================================
-- V1~V12까지는 utf8mb4_unicode_ci로 생성됨
-- V13에서 추가된 컬럼들은 utf8mb4_0900_ai_ci로 생성됨
-- → 전체를 utf8mb4_0900_ai_ci로 통일

ALTER TABLE `user`
    MODIFY COLUMN `name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY COLUMN `member_id` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY COLUMN `phone` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY COLUMN `email` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    MODIFY COLUMN `oauth_provider_id` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    MODIFY COLUMN `nickname` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    MODIFY COLUMN `intro` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    MODIFY COLUMN `img` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    MODIFY COLUMN `interests` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;

-- ========================================
-- 2단계: 기존 중복 이메일 처리 (안전장치)
-- ========================================
-- 혹시 중복된 이메일이 있다면 순번을 붙여서 고유하게 만듦
-- 예: test@gmail.com → test@gmail.com, test_1@gmail.com

-- 중복 이메일 확인 및 처리
UPDATE `user` u1
    INNER JOIN (
        SELECT
            u2.id,
            u2.email,
            ROW_NUMBER() OVER (PARTITION BY u2.email ORDER BY u2.id) - 1 AS dup_num
        FROM `user` u2
        WHERE u2.email IS NOT NULL AND u2.email != ''
    ) dup ON u1.id = dup.id
SET u1.email = CASE
    WHEN dup.dup_num > 0 THEN CONCAT(
        SUBSTRING_INDEX(dup.email, '@', 1),
        '_',
        dup.dup_num,
        '@',
        SUBSTRING_INDEX(dup.email, '@', -1)
    )
    ELSE u1.email
END
WHERE dup.dup_num > 0;

-- ========================================
-- 3단계: email 컬럼에 UNIQUE 제약조건 추가
-- ========================================
-- 이제부터 같은 이메일로 중복 가입 불가
-- OAuth2 로그인 시 이메일로 먼저 조회 → 있으면 기존 계정 사용

ALTER TABLE `user`
    ADD UNIQUE KEY `uk_email` (`email`);

-- ========================================
-- 완료 확인 쿼리 (주석)
-- ========================================
-- 마이그레이션 후 수동으로 실행해서 확인

-- 1. Collation 확인
-- SELECT
--     COLUMN_NAME,
--     COLLATION_NAME
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = 'study_block'
--   AND TABLE_NAME = 'user'
--   AND DATA_TYPE = 'varchar'
-- ORDER BY ORDINAL_POSITION;
-- → 모든 VARCHAR 컬럼이 utf8mb4_0900_ai_ci여야 함

-- 2. 이메일 중복 확인
-- SELECT
--     email,
--     COUNT(*) as count
-- FROM `user`
-- WHERE email IS NOT NULL
-- GROUP BY email
-- HAVING count > 1;
-- → 결과가 없어야 정상

-- 3. UNIQUE 제약조건 확인
-- SHOW INDEX FROM `user` WHERE Key_name = 'uk_email';
-- → uk_email 인덱스가 표시되어야 함
