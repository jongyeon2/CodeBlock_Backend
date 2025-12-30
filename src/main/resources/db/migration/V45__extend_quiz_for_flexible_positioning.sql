-- V45: Quiz 테이블 확장 - 유연한 위치 설정 및 Sequence 관리
-- - QuestionType에 SUBJECTIVE 추가
-- - Quiz의 위치를 섹션 시작/강의 뒤/섹션 끝으로 설정 가능
-- - Course ID 추가 (성능 최적화)
-- - Sequence를 10배 단위로 변경 (중간 삽입 용이)

-- 1. QuestionType enum에 SUBJECTIVE 추가
ALTER TABLE quiz_question
    MODIFY COLUMN question_type ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER', 'SUBJECTIVE') NOT NULL;

-- 2. Quiz 테이블에 course_id 컬럼 추가 (임시로 nullable)
ALTER TABLE quiz
    ADD COLUMN course_id BIGINT NULL COMMENT 'Course ID for performance optimization';

-- 3. 기존 quiz 데이터의 course_id 설정 (lecture를 통해 조회)
UPDATE quiz q
    INNER JOIN lecture l ON q.lecture_id = l.id
    SET q.course_id = l.course_id
WHERE q.course_id IS NULL;

-- 4. course_id를 NOT NULL로 변경
ALTER TABLE quiz
    MODIFY COLUMN course_id BIGINT NOT NULL;

-- 5. Quiz 테이블에 position, target_lecture_id 컬럼 추가
ALTER TABLE quiz
    ADD COLUMN position ENUM('SECTION_START', 'AFTER_LECTURE', 'SECTION_END') NOT NULL DEFAULT 'AFTER_LECTURE'
        COMMENT 'Quiz position in section',
    ADD COLUMN target_lecture_id BIGINT NULL
        COMMENT 'Target lecture ID when position is AFTER_LECTURE';

-- 6. lecture_id를 nullable로 변경 (SECTION_START, SECTION_END는 lecture 없음)
ALTER TABLE quiz
    MODIFY COLUMN lecture_id BIGINT NULL;

-- 7. 기존 quiz 데이터 마이그레이션
--    - position = 'AFTER_LECTURE' (기본값)
--    - target_lecture_id = lecture_id (기존 lecture_id 복사)
UPDATE quiz
    SET target_lecture_id = lecture_id,
        position = 'AFTER_LECTURE'
WHERE lecture_id IS NOT NULL;

-- 8. Sequence를 10배 단위로 변경 (기존 1, 2, 3 → 10, 20, 30)
--    중간 삽입을 위해 여유 공간 확보
UPDATE quiz
    SET sequence = sequence * 10;

-- 9. 외래 키 추가
ALTER TABLE quiz
    ADD CONSTRAINT fk_quiz_target_lecture
        FOREIGN KEY (target_lecture_id) REFERENCES lecture(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_quiz_course
        FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE CASCADE;

-- 10. 인덱스 추가 (성능 최적화)
CREATE INDEX idx_quiz_course_id ON quiz(course_id);
CREATE INDEX idx_quiz_section_position ON quiz(section_id, position);
CREATE INDEX idx_quiz_target_lecture_id ON quiz(target_lecture_id);

-- 11. 주석 추가
ALTER TABLE quiz
    MODIFY COLUMN lecture_id BIGINT NULL
        COMMENT 'Deprecated - use target_lecture_id instead',
    MODIFY COLUMN position ENUM('SECTION_START', 'AFTER_LECTURE', 'SECTION_END') NOT NULL DEFAULT 'AFTER_LECTURE'
        COMMENT 'Quiz position: SECTION_START (before first lecture), AFTER_LECTURE (after specific lecture), SECTION_END (after last lecture)',
    MODIFY COLUMN sequence INT NOT NULL
        COMMENT 'Sequence in section (10-based: 10, 15, 20, 25... for easier insertion)';