-- V25: Course 테이블에 현금 가격(price) 컬럼 추가
-- 작성일: 2025-10-30
-- 목적: 정규코스는 현금 결제, 섹션은 쿠키 결제로 구분
-- 비즈니스 로직:
--   - Course (정규코스): 현금(KRW) 결제만 가능 → price 컬럼 사용
--   - Section (섹션): 쿠키 결제만 가능 → cookie_price 컬럼 사용 (이미 존재)
--   - Lecture (강의): 개별 구매 불가, 섹션 구매로만 접근

-- ========================================
-- 1단계: course 테이블에 price 컬럼 추가
-- ========================================

ALTER TABLE `course`
    ADD COLUMN `price` BIGINT NULL COMMENT '정규코스 현금 가격 (KRW)'
    AFTER `price_cookie`;

-- ========================================
-- 2단계: 기존 데이터 마이그레이션
-- ========================================

-- 기존 price_cookie 값을 price로 복사 (임시로 쿠키를 원화로 환산: 1쿠키 = 100원)
UPDATE `course`
SET `price` = `price_cookie` * 100
WHERE `price_cookie` IS NOT NULL;

-- price가 여전히 NULL인 레코드는 0으로 설정 (무료 강의)
UPDATE `course`
SET `price` = 0
WHERE `price` IS NULL;

-- ========================================
-- 3단계: 성능 최적화 인덱스 추가
-- ========================================

-- 가격별 정렬 및 필터링용 인덱스
CREATE INDEX `idx_course_price` ON `course` (`price`);

-- 가격 범위 검색용 복합 인덱스 (공개 + 가격)
CREATE INDEX `idx_course_published_price` ON `course` (`is_published`, `price`);

-- ========================================
-- 4단계: 마이그레이션 검증 (주석)
-- ========================================

-- 테이블 구조 확인
-- DESCRIBE `course`;

-- price 컬럼 데이터 확인
-- SELECT 
--     id, 
--     title, 
--     price_cookie, 
--     price,
--     discount_percentage,
--     (price - (price * discount_percentage / 100)) as discounted_price
-- FROM `course`
-- LIMIT 10;

-- price가 NULL인 레코드 확인 (없어야 함)
-- SELECT COUNT(*) as null_price_count
-- FROM `course`
-- WHERE `price` IS NULL;

-- ========================================
-- 참고사항
-- ========================================

-- Q: price_cookie는 삭제하지 않나요?
-- A: 아직 삭제하지 않습니다. 이유:
--    1. 안전성: 섹션 구조가 완전히 안정화될 때까지 유지
--    2. 호환성: 기존 코드가 price_cookie를 참조할 수 있음
--    3. 점진적 전환: price를 먼저 사용하고, 나중에 price_cookie 제거

-- Q: 1쿠키 = 100원 환산 비율은?
-- A: 임시 환산입니다. 실제 비즈니스 정책에 맞게 조정하세요.
--    예: UPDATE course SET price = price_cookie * 1000; -- 1쿠키 = 1000원

-- Q: 나중에 price_cookie를 NULL로 만들려면?
-- A: 다음 마이그레이션에서 실행:
--    UPDATE `course` SET `price_cookie` = NULL;
--    ALTER TABLE `course` MODIFY COLUMN `price_cookie` BIGINT NULL COMMENT '(Deprecated) 쿠키 가격';



