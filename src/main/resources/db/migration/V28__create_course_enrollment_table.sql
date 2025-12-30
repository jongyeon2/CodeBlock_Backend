-- V28: Create course_enrollment table for course-level ownership tracking
-- This complements the existing lecture_ownership (section-level) system
-- Supports: Full course purchase, conditional refund policy, progress tracking

CREATE TABLE course_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- User and Course relationship
    user_id BIGINT NOT NULL COMMENT 'User who enrolled',
    course_id BIGINT NOT NULL COMMENT 'Course enrolled in',
    order_id BIGINT NULL COMMENT 'Link to the order that created this enrollment',

    -- Enrollment status
    status ENUM('ACTIVE', 'COMPLETED', 'REVOKED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'ACTIVE: Currently enrolled, COMPLETED: Finished course, REVOKED: Refunded/removed, EXPIRED: Time-limited access ended',

    enrollment_source ENUM('PURCHASE_CASH', 'PURCHASE_COOKIE', 'ADMIN_GRANT', 'PROMOTIONAL') NOT NULL
        COMMENT 'How the user got access',

    -- Progress tracking
    progress_percentage DECIMAL(5,2) DEFAULT 0.00
        COMMENT 'Overall course completion % (0.00 to 100.00)',

    completed_lectures_count INT DEFAULT 0
        COMMENT 'Number of lectures completed',

    total_lectures_count INT DEFAULT 0
        COMMENT 'Total lectures in course (cached for performance)',

    completed_quizzes_count INT DEFAULT 0
        COMMENT 'Number of quizzes passed',

    total_quizzes_count INT DEFAULT 0
        COMMENT 'Total quizzes in course (cached for performance)',

    -- Timing
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'When enrollment was created',

    started_at DATETIME NULL
        COMMENT 'When user first accessed any course content',

    completed_at DATETIME NULL
        COMMENT 'When user completed the course (100% progress)',

    last_accessed_at DATETIME NULL
        COMMENT 'Last time user accessed any content in this course',

    expires_at DATETIME NULL
        COMMENT 'For time-limited courses (NULL = permanent access)',

    -- Conditional refund policy (7 days + content view limit)
    refundable_until DATETIME NULL
        COMMENT 'Refund deadline (typically enrolled_at + 7 days)',

    content_view_percentage DECIMAL(5,2) DEFAULT 0.00
        COMMENT 'Percentage of content viewed (affects refund eligibility)',

    first_content_viewed_at DATETIME NULL
        COMMENT 'When user first viewed any content (affects refund)',

    refund_limit_percentage DECIMAL(5,2) DEFAULT 10.00
        COMMENT 'Maximum % of content that can be viewed while still refundable',

    -- Audit timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE KEY uk_user_course (user_id, course_id),

    -- Indexes for common queries
    INDEX idx_user_status (user_id, status),
    INDEX idx_course_status (course_id, status),
    INDEX idx_status (status),
    INDEX idx_enrollment_source (enrollment_source),
    INDEX idx_expires_at (expires_at),
    INDEX idx_last_accessed (last_accessed_at),
    INDEX idx_refundable (refundable_until, content_view_percentage),
    INDEX idx_order (order_id),

    -- Foreign keys
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Course-level enrollment tracking for full course purchases';

-- Add order_id to existing lecture_ownership for consistency
ALTER TABLE lecture_ownership
    ADD COLUMN order_id BIGINT NULL COMMENT 'Link to purchase order' AFTER source,
    ADD INDEX idx_order (order_id),
    ADD CONSTRAINT fk_lecture_ownership_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL;
