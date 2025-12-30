-- V3: 코스 및 강의 테이블 생성

CREATE TABLE course (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(50) NOT NULL,
    category VARCHAR(50) NULL,
    level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NULL,
    duration_minutes INT NULL,
    thumbnail_url VARCHAR(255) NULL,
    enrollment_count BIGINT NOT NULL DEFAULT 0,
    is_published TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_category (category),
    INDEX idx_level (level),
    INDEX idx_is_published (is_published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lecture (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    thumbnail_url VARCHAR(255) NULL,
    upload_date DATE NOT NULL,
    price_cookie BIGINT NULL,
    sequence INT NOT NULL,
    is_free TINYINT(1) NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    discount_percentage INT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_course_id (course_id),
    INDEX idx_status (status),
    INDEX idx_sequence (sequence),
    CONSTRAINT FK_course_TO_lecture_1 FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT CHK_discount_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE video (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    video_url VARCHAR(255) NOT NULL,
    duration_seconds INT NULL,
    resolution VARCHAR(20) NULL,
    file_size BIGINT NULL,
    encoding_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    thumbnail_url VARCHAR(255) NULL,
    subtitle_url VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_lecture_id (lecture_id),
    INDEX idx_encoding_status (encoding_status),
    CONSTRAINT FK_lecture_TO_video_1 FOREIGN KEY (lecture_id) REFERENCES lecture (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NULL,
    passing_score INT NOT NULL DEFAULT 60,
    max_attempts INT NOT NULL DEFAULT 3,
    sequence INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_lecture_id (lecture_id),
    CONSTRAINT FK_lecture_TO_quiz_1 FOREIGN KEY (lecture_id) REFERENCES lecture (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER') NOT NULL,
    explanation TEXT NULL,
    points INT NOT NULL DEFAULT 1,
    sequence INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_quiz_id (quiz_id),
    CONSTRAINT FK_quiz_TO_quiz_question_1 FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_question_id BIGINT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    is_correct TINYINT(1) NOT NULL DEFAULT 0,
    sequence INT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_quiz_question_id (quiz_question_id),
    CONSTRAINT FK_quiz_question_TO_quiz_option_1 FOREIGN KEY (quiz_question_id) REFERENCES quiz_question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lecture_resource (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    title VARCHAR(50) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NULL,
    description TEXT NULL,
    download_count INT NOT NULL DEFAULT 0,
    sequence INT NULL,
    upload_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_lecture_id (lecture_id),
    CONSTRAINT FK_lecture_TO_lecture_resource_1 FOREIGN KEY (lecture_id) REFERENCES lecture (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lecture_ownership (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    status ENUM('ACTIVE', 'REVOKED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    source ENUM('PURCHASE_PAID_COOKIE', 'PURCHASE_FREE_COOKIE', 'PURCHASE_CASH', 'ADMIN') NOT NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_lecture (user_id, lecture_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    CONSTRAINT FK_user_TO_lecture_ownership_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT FK_lecture_TO_lecture_ownership_1 FOREIGN KEY (lecture_id) REFERENCES lecture (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
