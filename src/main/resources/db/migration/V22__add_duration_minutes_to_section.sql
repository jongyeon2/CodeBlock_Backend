-- V22: section 테이블에 duration_minutes 컬럼 추가

ALTER TABLE `section`
    ADD COLUMN `duration_minutes` INT NULL AFTER `sequence`;