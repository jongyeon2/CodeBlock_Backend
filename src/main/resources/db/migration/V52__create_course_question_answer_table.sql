CREATE TABLE course_question_answer (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_course_question_answer_question
        FOREIGN KEY (question_id) REFERENCES course_question (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_course_question_answer_author
        FOREIGN KEY (author_id) REFERENCES user (id),
    INDEX idx_course_question_answer_question (question_id, created_at),
    INDEX idx_course_question_answer_author (author_id)
);

ALTER TABLE post
    MODIFY image_url TEXT NULL COMMENT '이미지 URL - 기존 NULL 허용 호환 유지';

