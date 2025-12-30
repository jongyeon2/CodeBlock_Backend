-- V33: wishlist가 section_id 대신 course_id를 참조하도록 스키마 변경

-- 1. course_id 컬럼 추가 (임시로 NULL 허용)
ALTER TABLE `wishlist`
    ADD COLUMN `course_id` BIGINT NULL COMMENT '코스 FK'
        AFTER `section_id`;

-- 2. section을 통해 course_id 데이터 채우기
UPDATE `wishlist` w
INNER JOIN `section` s ON w.section_id = s.id
SET w.course_id = s.course_id
WHERE w.course_id IS NULL;

-- 3. 기존 FK 및 인덱스 정리
SET @schema := DATABASE();

-- fk_wishlist_lecture 제거 (존재할 때만)
SET @drop_fk_lecture := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND CONSTRAINT_NAME = 'fk_wishlist_lecture'
        ),
        'ALTER TABLE `wishlist` DROP FOREIGN KEY `fk_wishlist_lecture`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_fk_lecture;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- fk_wishlist_section 제거 (존재할 때만)
SET @drop_fk_section := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND CONSTRAINT_NAME = 'fk_wishlist_section'
        ),
        'ALTER TABLE `wishlist` DROP FOREIGN KEY `fk_wishlist_section`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_fk_section;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 인덱스 제거 (존재할 때만)
SET @drop_idx_uc := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND INDEX_NAME = 'uc_user_lecture'
        ),
        'ALTER TABLE `wishlist` DROP INDEX `uc_user_lecture`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_idx_uc;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_idx_lecture_created := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND INDEX_NAME = 'idx_wishlist_lecture_created'
        ),
        'ALTER TABLE `wishlist` DROP INDEX `idx_wishlist_lecture_created`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_idx_lecture_created;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_idx_section := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND INDEX_NAME = 'idx_wishlist_section_id'
        ),
        'ALTER TABLE `wishlist` DROP INDEX `idx_wishlist_section_id`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_idx_section;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_uc_user_section := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND CONSTRAINT_NAME = 'uc_user_section'
        ),
        'ALTER TABLE `wishlist` DROP INDEX `uc_user_section`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_uc_user_section;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 불필요해진 lecture_id, section_id 컬럼 제거 (존재할 때만)
SET @drop_col_lecture := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND COLUMN_NAME = 'lecture_id'
        ),
        'ALTER TABLE `wishlist` DROP COLUMN `lecture_id`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_col_lecture;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_col_section := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema
              AND TABLE_NAME = 'wishlist'
              AND COLUMN_NAME = 'section_id'
        ),
        'ALTER TABLE `wishlist` DROP COLUMN `section_id`',
        'DO 0'
    )
);
PREPARE stmt FROM @drop_col_section;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. course_id를 NOT NULL로 변경 후 FK/인덱스 재구성
ALTER TABLE `wishlist`
    MODIFY COLUMN `course_id` BIGINT NOT NULL COMMENT '코스 FK';

ALTER TABLE `wishlist`
    ADD CONSTRAINT `fk_wishlist_course`
        FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE;

ALTER TABLE `wishlist`
    ADD CONSTRAINT `uc_wishlist_user_course`
        UNIQUE (`user_id`, `course_id`);

CREATE INDEX `idx_wishlist_course_created`
    ON `wishlist` (`course_id`, `created_at` DESC);
