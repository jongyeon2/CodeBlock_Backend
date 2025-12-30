-- V31: post 테이블 image_url, hit 컬럼 추가
-- 작성일: 2025-11-03
-- 목적: 게시글에 이미지 업로드 하기 위해서 image_url 컬럼 추가, 조회수 카운트 위해 hit 컬럼 추가

-- ========================================
-- 1단계: post 테이블에 image_url, hit 컬럼 추가
-- ========================================
ALTER TABLE `post`
    ADD COLUMN `image_url` VARCHAR(255) NULL COMMENT '이미지 URL' AFTER `edited_content`,
    ADD COLUMN `hit` BIGINT NOT NULL DEFAULT 0 AFTER `image_url`;