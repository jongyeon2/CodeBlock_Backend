-- V40: Course 테이블에 instructor_id 추가
-- 작성일: 2025-01-27
-- 목적: 강사가 자신이 생성한 정규코스 목록을 직접 조회할 수 있도록 개선
--
-- 변경 사항:
-- 1. course 테이블에 instructor_id 컬럼 추가
-- 2. 기존 데이터 마이그레이션 (lecture의 instructor를 course로 복사)
-- 3. Foreign Key 및 인덱스 추가
--
-- 주의: lecture 테이블의 instructor_id는 유지 (삭제하지 않음)

-- ========================================
-- 1단계: Course 테이블에 instructor_id 컬럼 추가
-- ========================================

ALTER TABLE `course`
    ADD COLUMN `instructor_id` BIGINT NULL
    COMMENT '정규코스 생성자 강사 ID (instructor_profile 참조)'
    AFTER `id`;

-- ========================================
-- 2단계: 기존 데이터 마이그레이션
-- ========================================

-- 각 Course의 첫 번째 Lecture의 instructor를 Course의 instructor로 설정
-- (한 정규코스는 한 명의 강사만 소유하므로, 첫 번째 강의의 강사가 코스 소유자)

UPDATE `course` c
SET c.instructor_id = (
    SELECT l.instructor_id
    FROM `lecture` l
    WHERE l.course_id = c.id
      AND l.instructor_id IS NOT NULL
    ORDER BY l.sequence ASC
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1
    FROM `lecture` l
    WHERE l.course_id = c.id
      AND l.instructor_id IS NOT NULL
);

-- ========================================
-- 3단계: Foreign Key 제약조건 추가
-- ========================================

ALTER TABLE `course`
    ADD CONSTRAINT `fk_course_instructor`
        FOREIGN KEY (`instructor_id`)
        REFERENCES `instructor_profile` (`id`)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

-- ========================================
-- 4단계: 성능 최적화를 위한 인덱스 추가
-- ========================================

-- 강사별 정규코스 조회를 위한 인덱스
CREATE INDEX `idx_course_instructor_id`
    ON `course` (`instructor_id`);

-- 강사 + 공개 여부 조합 인덱스 (강사별 공개 코스 조회용)
CREATE INDEX `idx_course_instructor_published`
    ON `course` (`instructor_id`, `is_published`);

-- ========================================
-- 5단계: 마이그레이션 검증 (주석)
-- ========================================

-- 테이블 구조 확인
-- DESCRIBE `course`;

-- 인덱스 확인
-- SHOW INDEX FROM `course`;

-- 강사별 코스 수 확인
-- SELECT
--     ip.id AS instructor_id,
--     ip.channel_name,
--     COUNT(c.id) AS course_count,
--     SUM(CASE WHEN c.is_published = 1 THEN 1 ELSE 0 END) AS published_count
-- FROM instructor_profile ip
-- LEFT JOIN course c ON ip.id = c.instructor_id
-- GROUP BY ip.id, ip.channel_name
-- ORDER BY course_count DESC;

-- 마이그레이션 결과 확인 (instructor가 설정된 코스 수)
-- SELECT
--     COUNT(*) AS total_courses,
--     COUNT(instructor_id) AS courses_with_instructor,
--     COUNT(*) - COUNT(instructor_id) AS courses_without_instructor
-- FROM course;

-- ========================================
-- 참고사항
-- ========================================

-- Q: lecture 테이블의 instructor_id는 왜 삭제하지 않나요?
-- A: 1. 안전성: 기존 코드 동작 보장, R파일 수정 불필요
--    2. 유연성: 나중에 "게스트 강사" 기능 추가 가능
--    3. 점진적 전환: course.instructor_id를 우선 사용하되,
--                   lecture.instructor_id는 optional로 유지

-- Q: course.instructor_id가 NULL인 경우는?
-- A: 강의가 없는 신규 코스이거나, 강의의 instructor가 설정되지 않은 경우
--    코스 생성 시 instructor를 필수로 받도록 로직 개선 권장