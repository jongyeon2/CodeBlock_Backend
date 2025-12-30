-- V46: settlement_ledger 테이블에 order_items_id 컬럼 추가
-- 정산 레코드가 어떤 order_item에 해당하는지 추적하기 위해 추가

-- 1) settlement_ledger 테이블에 order_items_id 컬럼 추가
ALTER TABLE `settlement_ledger`
  ADD COLUMN `order_items_id` BIGINT NULL AFTER `order_id`,
  ADD CONSTRAINT `fk_settlement_ledger_order_items` FOREIGN KEY (`order_items_id`) REFERENCES `order_items`(`id`);

-- 2) 인덱스 추가
CREATE INDEX `idx_settlement_ledger_order_items` ON `settlement_ledger` (`order_items_id`);

-- order_items_id 컬럼 추가 (NULL 허용으로 먼저 추가)
ALTER TABLE settlement_hold 
ADD COLUMN order_items_id BIGINT NULL COMMENT 'OrderItems ID' AFTER order_id;

-- 기존 데이터 백필
-- 각 order_id에 대해 첫 번째 정산 대상 order_item을 매핑
UPDATE settlement_hold sh
INNER JOIN (
    SELECT
        sl.order_id,
        MIN(sl.order_items_id) as first_order_items_id
    FROM settlement_ledger sl
    WHERE sl.eligible_flag = false
      AND sl.settled_at IS NULL
    GROUP BY sl.order_id
) ledger_map ON sh.order_id = ledger_map.order_id
SET sh.order_items_id = ledger_map.first_order_items_id
WHERE sh.order_items_id IS NULL;

-- order_items_id를 NOT NULL로 변경
ALTER TABLE settlement_hold
MODIFY COLUMN order_items_id BIGINT NOT NULL COMMENT 'OrderItems ID';

-- FK 및 인덱스 추가
ALTER TABLE settlement_hold
ADD CONSTRAINT fk_settlement_hold_order_items
FOREIGN KEY (order_items_id) REFERENCES order_items(id) ON DELETE CASCADE;

ALTER TABLE settlement_hold
ADD UNIQUE INDEX uk_settlement_hold_order_items (order_items_id);

-- 기존 order_id FK 및 인덱스 제거
ALTER TABLE settlement_hold
DROP FOREIGN KEY fk_settlement_hold_order;

ALTER TABLE settlement_hold
DROP INDEX fk_settlement_hold_order;

-- order_id 컬럼 제거
ALTER TABLE settlement_hold DROP COLUMN order_id;

-- 신고 테이블 컬럼 추가
ALTER TABLE report ADD COLUMN content_id BIGINT NOT NULL COMMENT '컨텐츠ID' AFTER target_type;
