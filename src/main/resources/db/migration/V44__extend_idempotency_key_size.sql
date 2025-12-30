-- V40: idempotency_key 컬럼 크기 확장
-- 작성일: 2025-11-04
-- 목적: 부분 환불 시 생성되는 긴 idempotency_key 저장을 위해 컬럼 크기 확장

-- idempotency_keys 테이블의 idempotency_key 컬럼 크기 확장 (36 -> 128)
ALTER TABLE `idempotency_keys`
    MODIFY COLUMN `idempotency_key` VARCHAR(255) NOT NULL COMMENT '멱등성 키 (중복 요청 방지용)';

-- refunds 테이블의 idempotency_key 컬럼 크기 확장 (36 -> 128)
ALTER TABLE `refunds`
    MODIFY COLUMN `idempotency_key` VARCHAR(255) NULL COMMENT '멱등성 키 (중복 요청 방지용)';

-- orders 테이블의 idempotency_key 컬럼 크기 확장 (36 -> 128)
ALTER TABLE `orders`
    MODIFY COLUMN `idempotency_key` VARCHAR(255) NULL COMMENT '멱등성 키 (중복 요청 방지용)';

