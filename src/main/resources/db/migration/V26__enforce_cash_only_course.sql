-- V26: Enforce cash-only pricing for courses (price only; no price_cookie)
-- 목적: 정규코스는 현금 결제만 허용. course.price_cookie 사용 금지 및 정리

-- 1) 누락된 price 채우기 (기존 데이터 보호)
UPDATE `course`
SET `price` = COALESCE(`price`, `price_cookie` * 100)
WHERE `price` IS NULL;

-- 2) 쿠키 가격 제거
UPDATE `course`
SET `price_cookie` = NULL
WHERE `price_cookie` IS NOT NULL;

-- 3) 제약 추가: price_cookie는 항상 NULL (MySQL 8.0.16+ 에서만 엄격 적용)
--    구버전 MySQL에서는 CHECK가 무시될 수 있음. 이 경우 애플리케이션/시드로 강제됨
ALTER TABLE `course`
    ADD CONSTRAINT `chk_course_no_cookie`
    CHECK (`price_cookie` IS NULL);

-- 4) 컬럼 주석 갱신(선택) - 남겨두되 Deprecated 명시
ALTER TABLE `course`
    MODIFY COLUMN `price_cookie` BIGINT NULL COMMENT '(Deprecated) 정규코스는 사용하지 않음';


