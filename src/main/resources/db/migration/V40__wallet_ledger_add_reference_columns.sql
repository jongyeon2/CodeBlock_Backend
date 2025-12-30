-- V33: wallet_ledger 느슨한 참조 컬럼 추가 (1단계: 이중 기록)

ALTER TABLE `wallet_ledger`
    ADD COLUMN `wallet_id` BIGINT NULL AFTER `id`,
    ADD COLUMN `reference_type` VARCHAR(30) NULL AFTER `notes`,
    ADD COLUMN `reference_id`   BIGINT NULL AFTER `reference_type`,
    ADD COLUMN `currency` CHAR(3) NOT NULL DEFAULT 'KRW' AFTER `cookie_amount`;

-- 조회 최적화 인덱스 추가
CREATE INDEX `idx_wallet_ledger_ref` ON `wallet_ledger` (`reference_type`, `reference_id`);

-- 과거 데이터 백필: 기존 강 참조를 느슨한 참조로 옮김
UPDATE `wallet_ledger`
SET `reference_type` = 'ORDER', `reference_id` = `order_id`
WHERE `order_id` IS NOT NULL AND `reference_type` IS NULL;

UPDATE `wallet_ledger`
SET `reference_type` = 'PAYMENT', `reference_id` = `payment_id`
WHERE `payment_id` IS NOT NULL AND `reference_type` IS NULL;

UPDATE `wallet_ledger`
SET `reference_type` = 'BATCH', `reference_id` = `batch_id`
WHERE `batch_id` IS NOT NULL AND `reference_type` IS NULL;


--  환불 테이블에 멱등성 키 컬럼 추가

-- refunds 테이블에 idempotency_key 컬럼 추가
ALTER TABLE `refunds`
    ADD COLUMN `idempotency_key` VARCHAR(36) NULL COMMENT '멱등성 키 (중복 요청 방지용)'
    AFTER `refund_key`;

-- idempotency_key에 인덱스 추가 (조회 성능 향상)
CREATE INDEX `idx_refunds_idempotency_key` ON `refunds` (`idempotency_key`);

