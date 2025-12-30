-- V12: 코스 상세 정보 확장 및 관련 테이블 추가

-- 1. course 테이블 확장
ALTER TABLE `course`
    ADD COLUMN `summary` TEXT NULL AFTER `title`,
    ADD COLUMN `price_cookie` BIGINT NULL AFTER `thumbnail_url`,
    ADD COLUMN `discount_percentage` INT NOT NULL DEFAULT 0 AFTER `price_cookie`;

-- 2. 학습 목표 테이블
CREATE TABLE `course_learning_outcome` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `content` VARCHAR(255) NOT NULL,
    `display_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_course_learning_outcome_course_id` (`course_id`),
    CONSTRAINT `fk_course_learning_outcome_course` FOREIGN KEY (`course_id`)
        REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 수강 후기 테이블
CREATE TABLE `course_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `user_profile_id` BIGINT NOT NULL,
    `lecture_id` BIGINT NULL,
    `rating` INT NOT NULL,
    `content` TEXT NOT NULL,
    `is_lecture_specific` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_course_review_course_id` (`course_id`),
    INDEX `idx_course_review_user_profile_id` (`user_profile_id`),
    INDEX `idx_course_review_lecture_id` (`lecture_id`),
    CONSTRAINT `fk_course_review_course` FOREIGN KEY (`course_id`)
        REFERENCES `course` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_course_review_user_profile` FOREIGN KEY (`user_profile_id`)
        REFERENCES `user_profile` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_course_review_lecture` FOREIGN KEY (`lecture_id`)
        REFERENCES `lecture` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_course_review_rating` CHECK (`rating` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. FAQ 테이블
CREATE TABLE `course_faq` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `question` VARCHAR(255) NOT NULL,
    `answer` TEXT NOT NULL,
    `tag` VARCHAR(50) NULL,
    `display_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_course_faq_course_id` (`course_id`),
    CONSTRAINT `fk_course_faq_course` FOREIGN KEY (`course_id`)
        REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Q&A 게시판 테이블
CREATE TABLE `course_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `user_profile_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `answer` TEXT NULL,
    `status` ENUM('PENDING', 'ANSWERED') NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_course_question_course_id` (`course_id`),
    INDEX `idx_course_question_user_profile_id` (`user_profile_id`),
    INDEX `idx_course_question_status` (`status`),
    CONSTRAINT `fk_course_question_course` FOREIGN KEY (`course_id`)
        REFERENCES `course` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_course_question_user_profile` FOREIGN KEY (`user_profile_id`)
        REFERENCES `user_profile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 선수 지식/준비물 테이블
CREATE TABLE `course_prerequisite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `type` ENUM('REQUIRED', 'RECOMMENDED', 'MATERIAL') NOT NULL,
    `description` TEXT NOT NULL,
    `display_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_course_prerequisite_course_id` (`course_id`),
    CONSTRAINT `fk_course_prerequisite_course` FOREIGN KEY (`course_id`)
        REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
