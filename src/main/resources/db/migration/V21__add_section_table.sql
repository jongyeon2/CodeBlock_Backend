-- V21: Section 테이블 추가 및 관련 테이블 수정
-- 작성일: 2025-01-XX
-- 목적: Course → Section → Lecture 구조로 재구성

-- ========================================
-- 1단계: section 테이블 생성
-- ========================================

CREATE TABLE `section` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL COMMENT '코스 FK',
    `title` VARCHAR(255) NOT NULL COMMENT '섹션 제목',
    `description` TEXT NULL COMMENT '섹션 설명',
    `sequence` INT NOT NULL COMMENT '섹션 순서',
    `cookie_price` BIGINT NULL COMMENT '섹션 가격 (쿠키)',
    `discount_percentage` INT NULL DEFAULT 0 COMMENT '할인율',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_section_course_id` (`course_id`),
    INDEX `idx_section_sequence` (`sequence`),
    INDEX `idx_section_course_sequence` (`course_id`, `sequence`),
    CONSTRAINT `fk_section_course` 
        FOREIGN KEY (`course_id`) 
        REFERENCES `course` (`id`) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    CONSTRAINT `chk_section_discount_range` CHECK (`discount_percentage` >= 0 AND `discount_percentage` <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='강의 섹션 테이블';

-- ========================================
-- 2단계: lecture 테이블에 section_id 추가
-- ========================================

ALTER TABLE `lecture`
    ADD COLUMN `section_id` BIGINT NULL COMMENT '섹션 FK'
    AFTER `course_id`;

-- ========================================
-- 3단계: 기존 데이터 마이그레이션 - 각 course별로 기본 섹션 생성
-- ========================================

-- 각 코스별로 '기본 섹션' 생성
-- 기존 lecture의 평균 가격을 section 가격으로 설정
INSERT INTO `section` (`course_id`, `title`, `description`, `sequence`, `cookie_price`, `discount_percentage`)
SELECT 
    c.id,
    CONCAT(c.title, ' - 강의 목록'),
    '기본 섹션',
    1,
    COALESCE((
        SELECT AVG(l.price_cookie) 
        FROM `lecture` l 
        WHERE l.course_id = c.id AND l.price_cookie > 0
    ), 0),
    0
FROM `course` c;

-- 모든 기존 lecture를 해당 course의 기본 섹션에 할당
UPDATE `lecture` l
INNER JOIN `section` s ON l.course_id = s.course_id
SET l.section_id = s.id
WHERE l.section_id IS NULL;

-- ========================================
-- 4단계: lecture 테이블에 section_id 외래키 제약조건 추가
-- ========================================

-- section_id를 NOT NULL로 변경
ALTER TABLE `lecture`
    MODIFY COLUMN `section_id` BIGINT NOT NULL COMMENT '섹션 FK';

-- 외래키 제약조건 추가
ALTER TABLE `lecture`
    ADD CONSTRAINT `fk_lecture_section` 
    FOREIGN KEY (`section_id`) 
    REFERENCES `section` (`id`) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE;

-- section_id 인덱스 추가
CREATE INDEX `idx_lecture_section_id` ON `lecture` (`section_id`);
CREATE INDEX `idx_lecture_section_sequence` ON `lecture` (`section_id`, `sequence`);

-- lecture 테이블에서 가격 정보 컬럼 제거 (이제 section에서 관리)
ALTER TABLE `lecture`
    DROP COLUMN `price_cookie`,
    DROP COLUMN `discount_percentage`;

-- ========================================
-- 5단계: 관련 테이블에 section_id 추가
-- ========================================

-- quiz 테이블
ALTER TABLE `quiz`
    ADD COLUMN `section_id` BIGINT NULL COMMENT '섹션 FK'
    AFTER `lecture_id`;

UPDATE `quiz` q
INNER JOIN `lecture` l ON q.lecture_id = l.id
SET q.section_id = l.section_id
WHERE q.section_id IS NULL;

ALTER TABLE `quiz`
    MODIFY COLUMN `section_id` BIGINT NOT NULL COMMENT '섹션 FK';

ALTER TABLE `quiz`
    ADD CONSTRAINT `fk_quiz_section` 
    FOREIGN KEY (`section_id`) 
    REFERENCES `section` (`id`) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE;

CREATE INDEX `idx_quiz_section_id` ON `quiz` (`section_id`);

-- lecture_resource 테이블
ALTER TABLE `lecture_resource`
    ADD COLUMN `section_id` BIGINT NULL COMMENT '섹션 FK'
    AFTER `lecture_id`;

UPDATE `lecture_resource` lr
INNER JOIN `lecture` l ON lr.lecture_id = l.id
SET lr.section_id = l.section_id
WHERE lr.section_id IS NULL;

ALTER TABLE `lecture_resource`
    MODIFY COLUMN `section_id` BIGINT NOT NULL COMMENT '섹션 FK';

ALTER TABLE `lecture_resource`
    ADD CONSTRAINT `fk_lecture_resource_section` 
    FOREIGN KEY (`section_id`) 
    REFERENCES `section` (`id`) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE;

CREATE INDEX `idx_lecture_resource_section_id` ON `lecture_resource` (`section_id`);

-- wishlist 테이블
ALTER TABLE `wishlist`
    ADD COLUMN `section_id` BIGINT NULL COMMENT '섹션 FK'
    AFTER `lecture_id`;

UPDATE `wishlist` w
INNER JOIN `lecture` l ON w.lecture_id = l.id
SET w.section_id = l.section_id
WHERE w.section_id IS NULL;

ALTER TABLE `wishlist`
    MODIFY COLUMN `section_id` BIGINT NOT NULL COMMENT '섹션 FK';

ALTER TABLE `wishlist`
    ADD CONSTRAINT `fk_wishlist_section` 
    FOREIGN KEY (`section_id`) 
    REFERENCES `section` (`id`) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE;

CREATE INDEX `idx_wishlist_section_id` ON `wishlist` (`section_id`);

-- lecture_ownership 테이블
ALTER TABLE `lecture_ownership`
    ADD COLUMN `section_id` BIGINT NULL COMMENT '섹션 FK'
    AFTER `lecture_id`;

UPDATE `lecture_ownership` lo
INNER JOIN `lecture` l ON lo.lecture_id = l.id
SET lo.section_id = l.section_id
WHERE lo.section_id IS NULL;

ALTER TABLE `lecture_ownership`
    MODIFY COLUMN `section_id` BIGINT NOT NULL COMMENT '섹션 FK';

ALTER TABLE `lecture_ownership`
    ADD CONSTRAINT `fk_lecture_ownership_section` 
    FOREIGN KEY (`section_id`) 
    REFERENCES `section` (`id`) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE;

CREATE INDEX `idx_lecture_ownership_section_id` ON `lecture_ownership` (`section_id`);

-- order_items 테이블
ALTER TABLE `order_items`
    ADD COLUMN `section_id` BIGINT NULL COMMENT '섹션 FK'
    AFTER `lecture_id`;

UPDATE `order_items` oi
INNER JOIN `lecture` l ON oi.lecture_id = l.id
SET oi.section_id = l.section_id
WHERE oi.section_id IS NULL AND oi.lecture_id IS NOT NULL;

-- order_items는 NULL 허용 (cookie_bundle 등 다른 타입도 있음)
ALTER TABLE `order_items`
    ADD CONSTRAINT `fk_order_items_section` 
    FOREIGN KEY (`section_id`) 
    REFERENCES `section` (`id`) 
    ON DELETE SET NULL 
    ON UPDATE CASCADE;

CREATE INDEX `idx_order_items_section_id` ON `order_items` (`section_id`);

-- ========================================
-- 6단계: 성능 최적화 인덱스 추가
-- ========================================

-- 섹션별 활성 강의 조회용 복합 인덱스
CREATE INDEX `idx_lecture_section_status` ON `lecture` (`section_id`, `status`);

-- 코스-섹션 조회 최적화
CREATE INDEX `idx_section_course_created` ON `section` (`course_id`, `created_at`);

-- ========================================
-- 마이그레이션 완료
-- ========================================

-- 테이블 구조 확인 쿼리 (주석)
-- DESCRIBE `section`;
-- DESCRIBE `lecture`;
-- SHOW INDEX FROM `section`;
-- SHOW INDEX FROM `lecture`;

-- 섹션별 강의 수 확인 쿼리 (주석)
-- SELECT 
--     c.title as course_title,
--     s.title as section_title,
--     s.sequence,
--     COUNT(l.id) as lecture_count
-- FROM course c
-- LEFT JOIN section s ON c.id = s.course_id
-- LEFT JOIN lecture l ON s.id = l.section_id
-- GROUP BY c.id, s.id
-- ORDER BY c.id, s.sequence;