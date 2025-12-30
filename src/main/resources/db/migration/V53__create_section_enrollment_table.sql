-- V53: Create section_enrollment table for section-level progress tracking
-- Aligns with LectureOwnership and CourseEnrollment to handle section purchases

CREATE TABLE section_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Relationships
    user_id BIGINT NOT NULL COMMENT 'User who purchased or has access to the section',
    course_id BIGINT NOT NULL COMMENT 'Parent course of the section',
    section_id BIGINT NOT NULL COMMENT 'Section being tracked',
    order_id BIGINT NULL COMMENT 'Origin order (if created via purchase)',

    -- Progress tracking
    progress_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00
        COMMENT 'Completion percentage (0.00 ~ 100.00) based on course lectures',
    completed_lectures_count INT NOT NULL DEFAULT 0
        COMMENT 'Number of course lectures completed by the user',
    total_lectures_count INT NOT NULL DEFAULT 0
        COMMENT 'Total number of lectures in the parent course (cached)',

    -- Timeline
    started_at DATETIME NULL COMMENT 'When the user first interacted with the section',
    completed_at DATETIME NULL COMMENT 'When section progress reached 100%',
    last_accessed_at DATETIME NULL COMMENT 'Most recent interaction timestamp',

    -- Audit timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints & Indexes
    UNIQUE KEY uk_section_enrollment_user_section (user_id, section_id),
    INDEX idx_section_enrollment_user_course (user_id, course_id),
    INDEX idx_section_enrollment_course (course_id),
    INDEX idx_section_enrollment_section (section_id),
    INDEX idx_section_enrollment_last_accessed (last_accessed_at),
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES section (id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks progress for section-level purchases and access';

