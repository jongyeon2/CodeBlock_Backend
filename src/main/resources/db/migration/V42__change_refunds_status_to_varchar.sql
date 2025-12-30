-- V42: refunds 테이블의 status 컬럼을 ENUM에서 VARCHAR로 변경
-- Java enum 값(PENDING, APPROVED, PROCESSED, REJECTED, FAILED)을 사용하기 위해

-- 1. 기존 ENUM 값을 VARCHAR로 변환하면서 값 매핑
-- REQUESTED -> PENDING (기존 데이터 유지)
-- APPROVED -> APPROVED (변경 없음)
-- FAILED -> FAILED (변경 없음)
-- COMPLETED -> PROCESSED (새로운 값으로 매핑)

-- 2. 기존 데이터 변환
UPDATE `refunds`
SET `status` = CASE
    WHEN `status` = 'REQUESTED' THEN 'PENDING'
    WHEN `status` = 'APPROVED' THEN 'APPROVED'
    WHEN `status` = 'FAILED' THEN 'FAILED'
    WHEN `status` = 'COMPLETED' THEN 'PROCESSED'
    ELSE 'PENDING'
END;

-- 3. 컬럼 타입 변경 (ENUM -> VARCHAR)
ALTER TABLE `refunds`
MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- 4. 토스 환불 응답 JSON 저장 컬럼 추가 (원본 데이터 보존용)
ALTER TABLE `refunds`
ADD COLUMN `toss_refund_response` JSON NULL COMMENT '토스페이먼츠 환불 API 원본 응답 JSON';



