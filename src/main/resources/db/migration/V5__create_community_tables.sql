-- V5: 커뮤니티(게시판, 댓글, 신고) 테이블 생성

CREATE TABLE board (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type TINYINT(1) NOT NULL COMMENT '0: 일반게시판, 1: 공지사항, 2: FAQ',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post (
    id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    original_content TEXT NOT NULL,
    edited_content TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    is_edited TINYINT(1) NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    INDEX idx_board_id (board_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    CONSTRAINT FK_board_TO_post_1 FOREIGN KEY (board_id) REFERENCES board (id) ON DELETE CASCADE,
    CONSTRAINT FK_user_TO_post_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    parent_comment_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_comment_id (parent_comment_id),
    INDEX idx_status (status),
    CONSTRAINT FK_post_TO_comment_1 FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT FK_user_TO_comment_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT FK_comment_TO_comment_1 FOREIGN KEY (parent_comment_id) REFERENCES comment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reported_user_id BIGINT NOT NULL,
    target_type ENUM('POST', 'COMMENT', 'USER') NOT NULL,
    report_reason VARCHAR(255) NULL,
    status ENUM('PENDING', 'REVIEWING', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reported_acted_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_reported_user_id (reported_user_id),
    INDEX idx_status (status),
    CONSTRAINT FK_user_TO_report_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
