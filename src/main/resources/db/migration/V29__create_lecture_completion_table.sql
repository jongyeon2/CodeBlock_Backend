-- V29: Create lecture_completion table for tracking lecture completion status
-- This enables progress calculation for course_enrollment
-- Tracks both video completion and quiz passing
CREATE TABLE lecture_completion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Relationships
    user_id BIGINT NOT NULL COMMENT 'User who completed the lecture',
    lecture_id BIGINT NOT NULL COMMENT 'Lecture that was completed',
    course_enrollment_id BIGINT NULL COMMENT 'Link to course enrollment (if course-level purchase)',

    -- Completion status
    is_completed BIT(1) NOT NULL DEFAULT b'0'
        COMMENT '1: Lecture is completed, 0: Not completed',

    completion_type ENUM('VIDEO_WATCHED', 'QUIZ_PASSED', 'MANUAL', 'AUTO') NOT NULL DEFAULT 'VIDEO_WATCHED'
        COMMENT 'How the lecture was marked as complete',

    -- Timing
    first_viewed_at DATETIME NULL
        COMMENT 'When user first opened this lecture',

    completed_at DATETIME NULL
        COMMENT 'When lecture was marked as complete',

    last_accessed_at DATETIME NULL
        COMMENT 'Most recent access to this lecture',

    -- Progress details
    total_time_spent_seconds INT DEFAULT 0
        COMMENT 'Total time spent on this lecture (in seconds)',

    video_watch_percentage DECIMAL(5,2) DEFAULT 0.00
        COMMENT 'Percentage of video watched (if applicable)',

    quiz_score DECIMAL(5,2) NULL
        COMMENT 'Quiz score if lecture has a quiz',

    quiz_attempts INT DEFAULT 0
        COMMENT 'Number of quiz attempts',

    -- Audit timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE KEY uk_user_lecture (user_id, lecture_id),

    -- Indexes
    INDEX idx_user_completed (user_id, is_completed),
    INDEX idx_lecture_completed (lecture_id, is_completed),
    INDEX idx_enrollment (course_enrollment_id),
    INDEX idx_completion_type (completion_type),
    INDEX idx_completed_at (completed_at),

    -- Foreign keys
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (lecture_id) REFERENCES lecture (id) ON DELETE CASCADE,
    FOREIGN KEY (course_enrollment_id) REFERENCES course_enrollment (id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks individual lecture completion for progress calculation';

-- Create index for progress calculation queries
CREATE INDEX idx_enrollment_progress ON lecture_completion(course_enrollment_id, is_completed, completed_at);

-- Create index for user's completed lectures
CREATE INDEX idx_user_completion_history ON lecture_completion(user_id, is_completed, completed_at);
