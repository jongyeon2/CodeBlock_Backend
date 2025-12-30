-- V41: wallet_ledger 강한 결합 제거 (3단계: FK/컬럼 정리)

-- 1) FK 삭제 (존재할 때만)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND CONSTRAINT_NAME = 'fk_wallet_ledger_order') > 0,
    'ALTER TABLE `wallet_ledger` DROP FOREIGN KEY `fk_wallet_ledger_order`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND CONSTRAINT_NAME = 'fk_wallet_ledger_payment') > 0,
    'ALTER TABLE `wallet_ledger` DROP FOREIGN KEY `fk_wallet_ledger_payment`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND CONSTRAINT_NAME = 'fk_wallet_ledger_batch') > 0,
    'ALTER TABLE `wallet_ledger` DROP FOREIGN KEY `fk_wallet_ledger_batch`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 인덱스 정리 (있을 때만)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND INDEX_NAME = 'idx_wallet_ledger_batch') > 0,
    'DROP INDEX `idx_wallet_ledger_batch` ON `wallet_ledger`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 컬럼 삭제 (존재할 때만)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND COLUMN_NAME = 'order_id') > 0,
    'ALTER TABLE `wallet_ledger` DROP COLUMN `order_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND COLUMN_NAME = 'payment_id') > 0,
    'ALTER TABLE `wallet_ledger` DROP COLUMN `payment_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wallet_ledger'
        AND COLUMN_NAME = 'batch_id') > 0,
    'ALTER TABLE `wallet_ledger` DROP COLUMN `batch_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


ALTER TABLE `cookie_bundle` MODIFY COLUMN `price_krw` BIGINT NOT NULL;


ALTER TABLE `payments` DROP COLUMN `version`;
ALTER TABLE `payments` DROP COLUMN `card_number_masked`;
ALTER TABLE `payments` DROP COLUMN `card_company`;
ALTER TABLE `payments` DROP COLUMN `installment_plan_months`;
ALTER TABLE `payments` DROP COLUMN `card_type`;
ALTER TABLE `payments` DROP COLUMN `owner_type`;
ALTER TABLE `payments` DROP COLUMN `virtual_account_number`;
ALTER TABLE `payments` DROP COLUMN `virtual_account_bank`;
ALTER TABLE `payments` DROP COLUMN `virtual_account_due_date`;
ALTER TABLE `payments` DROP COLUMN `virtual_account_holder_name`;
ALTER TABLE `payments` DROP COLUMN `transfer_bank`;
ALTER TABLE `payments` DROP COLUMN `transfer_account_number`;
ALTER TABLE `payments` DROP COLUMN `easy_pay_provider`;
ALTER TABLE `payments` DROP COLUMN `easy_pay_method`;
ALTER TABLE `payments` DROP COLUMN `mobile_phone_carrier`;
ALTER TABLE `payments` DROP COLUMN `mobile_phone_number`;


ALTER TABLE order_items MODIFY COLUMN item_type ENUM('COURSE', 'SECTION', 'COOKIE_BUNDLE') NOT NULL;