-- V13: user_profile 테이블 제거 및 컬럼 이동
-- nickname, intro, img, interests -> user 테이블로 이동
-- career, skills -> instructor_profile 테이블로 이동
-- instructor_profile을 user_id로 직접 참조하도록 변경

-- 1단계: user 테이블에 새로운 컬럼 추가
ALTER TABLE `user` 
ADD COLUMN `nickname` VARCHAR(255) NULL AFTER `is_creator`,
ADD COLUMN `intro` VARCHAR(255) NULL AFTER `nickname`,
ADD COLUMN `img` VARCHAR(255) NULL AFTER `intro`,
ADD COLUMN `interests` VARCHAR(255) NULL AFTER `img`;

-- 2단계: instructor_profile 테이블에 새로운 컬럼 추가 및 user_id 컬럼 추가
ALTER TABLE `instructor_profile`
ADD COLUMN `user_id` BIGINT NOT NULL AFTER `id`,
ADD COLUMN `career` VARCHAR(255) NULL AFTER `user_id`,
ADD COLUMN `skills` VARCHAR(255) NULL AFTER `career`;

-- 3단계: 기존 데이터 마이그레이션
-- user_profile의 데이터를 user 테이블로 복사
UPDATE `user` u 
INNER JOIN `user_profile` up ON u.id = up.user_id 
SET 
    u.nickname = up.nickname,
    u.intro = up.intro,
    u.img = up.img,
    u.interests = up.interests;

-- instructor_profile의 user_id 업데이트 및 career, skills 복사
UPDATE `instructor_profile` ip 
INNER JOIN `user_profile` up ON ip.user_profile_id = up.id 
SET 
    ip.user_id = up.user_id,
    ip.career = up.career,
    ip.skills = up.skills;

-- 4단계: course_review 테이블의 외래키 변경
-- user_profile_id를 user_id로 변경
ALTER TABLE `course_review`
ADD COLUMN `user_id` BIGINT NOT NULL AFTER `course_id`;

-- course_review의 user_id 업데이트
UPDATE `course_review` cr 
INNER JOIN `user_profile` up ON cr.user_profile_id = up.id 
SET cr.user_id = up.user_id;

-- 기존 외래키 제약조건 제거
ALTER TABLE `course_review` DROP FOREIGN KEY `fk_course_review_user_profile`;
ALTER TABLE `course_review` DROP INDEX `idx_course_review_user_profile_id`;

-- user_profile_id 컬럼 제거
ALTER TABLE `course_review` DROP COLUMN `user_profile_id`;

-- 새로운 외래키 제약조건 추가
ALTER TABLE `course_review`
ADD INDEX `idx_course_review_user_id` (`user_id`),
ADD CONSTRAINT `fk_course_review_user` FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 5단계: course_question 테이블의 외래키 변경
-- user_profile_id를 user_id로 변경
ALTER TABLE `course_question`
ADD COLUMN `user_id` BIGINT NOT NULL AFTER `course_id`;

-- course_question의 user_id 업데이트
UPDATE `course_question` cq 
INNER JOIN `user_profile` up ON cq.user_profile_id = up.id 
SET cq.user_id = up.user_id;

-- 기존 외래키 제약조건 제거
ALTER TABLE `course_question` DROP FOREIGN KEY `fk_course_question_user_profile`;
ALTER TABLE `course_question` DROP INDEX `idx_course_question_user_profile_id`;

-- user_profile_id 컬럼 제거
ALTER TABLE `course_question` DROP COLUMN `user_profile_id`;

-- 새로운 외래키 제약조건 추가
ALTER TABLE `course_question`
ADD INDEX `idx_course_question_user_id` (`user_id`),
ADD CONSTRAINT `fk_course_question_user` FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 6단계: instructor_profile 테이블의 외래키 변경
-- user_profile_id를 user_id로 변경
-- 기존 외래키 제약조건 제거
ALTER TABLE `instructor_profile` DROP FOREIGN KEY `FK_user_profile_TO_instructor_profile_1`;
ALTER TABLE `instructor_profile` DROP INDEX `uk_user_profile_id`;

-- user_profile_id 컬럼 제거
ALTER TABLE `instructor_profile` DROP COLUMN `user_profile_id`;

-- 새로운 외래키 제약조건 추가
ALTER TABLE `instructor_profile`
ADD UNIQUE KEY `uk_user_id` (`user_id`),
ADD CONSTRAINT `fk_instructor_profile_user` FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 7단계: user_profile 테이블 제거
DROP TABLE `user_profile`;
