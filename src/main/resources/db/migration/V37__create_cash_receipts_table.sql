-- ========================================
-- V37: payments 테이블 슬림화 (MySQL 5.7 + Flyway용)
-- ========================================

-- 1단계: 별도 테이블 생성
CREATE TABLE IF NOT EXISTS `cash_receipts` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `payment_id` BIGINT NOT NULL UNIQUE COMMENT '결제 FK (1:1)',
        `issued` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '발급 여부',
        `type` ENUM('INCOME_DEDUCTION', 'PROOF_OF_EXPENSE') NULL COMMENT '소득공제/지출증빙',
        `receipt_number` VARCHAR(50) NULL COMMENT '현금영수증 번호',
        `issued_at` DATETIME NULL COMMENT '발급 시각',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_payment_id` (`payment_id`),
    CONSTRAINT `fk_cash_receipts_payment`
    FOREIGN KEY (`payment_id`)
    REFERENCES `payments` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='현금영수증 정보';

CREATE TABLE IF NOT EXISTS `payment_webhook_events` (
                                                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                        `payment_id` BIGINT NULL COMMENT '결제 FK (1:N)',
                                                        `event_type` VARCHAR(50) NOT NULL COMMENT '이벤트 타입',
    `webhook_payload` JSON NOT NULL COMMENT '웹훅 페이로드',
    `received_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수신 시각',
    `processed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '처리 여부',
    `processed_at` DATETIME NULL COMMENT '처리 시각',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_payment_id` (`payment_id`),
    INDEX `idx_received_at` (`received_at`),
    INDEX `idx_processed` (`processed`),
    CONSTRAINT `fk_payment_webhook_events_payment`
    FOREIGN KEY (`payment_id`)
    REFERENCES `payments` (`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='웹훅 이벤트 이력';

-- ========================================
-- 2단계: 인덱스 제거 (개별 처리)
-- ========================================
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'payments'
       AND index_name = 'idx_payments_card_company') > 0,
    'DROP INDEX `idx_payments_card_company` ON `payments`',
    'SELECT "Index does not exist"'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE `payments` DROP CHECK `CHK_payments_retry_count_valid`;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'payments'
       AND index_name = 'idx_payments_retry_count') > 0,
    'DROP INDEX `idx_payments_retry_count` ON `payments`',
    'SELECT "Index does not exist"'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'payments'
       AND index_name = 'idx_payments_ip_address') > 0,
    'DROP INDEX `idx_payments_ip_address` ON `payments`',
    'SELECT "Index does not exist"'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'payments'
       AND index_name = 'idx_payments_payment_source') > 0,
    'DROP INDEX `idx_payments_payment_source` ON `payments`',
    'SELECT "Index does not exist"'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'payments'
       AND index_name = 'idx_payments_virtual_account_number') > 0,
    'DROP INDEX `idx_payments_virtual_account_number` ON `payments`',
    'SELECT "Index does not exist"'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'payments'
       AND index_name = 'idx_payments_cash_receipt_issued') > 0,
    'DROP INDEX `idx_payments_cash_receipt_issued` ON `payments`',
    'SELECT "Index does not exist"'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========================================
-- 3단계: 컬럼 제거 (개별 처리)
-- ========================================

-- order_name
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `order_name`',
    'SELECT "order_name does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'order_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- type
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `type`',
    'SELECT "type does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'type');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- country
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `country`',
    'SELECT "country does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'country');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- webhook_received_at
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `webhook_received_at`',
    'SELECT "webhook_received_at does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'webhook_received_at');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- receipt_url
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `receipt_url`',
    'SELECT "receipt_url does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'receipt_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- m_id
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `m_id`',
    'SELECT "m_id does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'm_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- balance_amount
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `balance_amount`',
    'SELECT "balance_amount does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'balance_amount');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ip_address
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `ip_address`',
    'SELECT "ip_address does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'ip_address');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_agent
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `user_agent`',
    'SELECT "user_agent does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'user_agent');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- payment_source
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `payment_source`',
    'SELECT "payment_source does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'payment_source');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- retry_count
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `retry_count`',
    'SELECT "retry_count does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'retry_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- last_retry_at
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `last_retry_at`',
    'SELECT "last_retry_at does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'last_retry_at');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- max_retry_count
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `max_retry_count`',
    'SELECT "max_retry_count does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'max_retry_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cash_receipt_issued
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `cash_receipt_issued`',
    'SELECT "cash_receipt_issued does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'cash_receipt_issued');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cash_receipt_type
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `cash_receipt_type`',
    'SELECT "cash_receipt_type does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'cash_receipt_type');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cash_receipt_number
SET @sql = (SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `payments` DROP COLUMN `cash_receipt_number`',
    'SELECT "cash_receipt_number does not exist" AS message'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'cash_receipt_number');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========================================
-- 4단계: 체크 제약조건 추가
-- ========================================
SET @constraint_name = 'CHK_payments_amount_positive';
SET @sql = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `payments` ADD CONSTRAINT `CHK_payments_amount_positive` CHECK (`amount` > 0)',
    'SELECT "Constraint already exists"'
) FROM information_schema.table_constraints
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND constraint_name = @constraint_name);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_name = 'CHK_cash_receipt_issued_complete';
SET @sql = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `cash_receipts` ADD CONSTRAINT `CHK_cash_receipt_issued_complete` CHECK ((`issued` = 0) OR (`issued` = 1 AND `type` IS NOT NULL AND `receipt_number` IS NOT NULL AND `issued_at` IS NOT NULL))',
    'SELECT "Constraint already exists"'
) FROM information_schema.table_constraints
 WHERE table_schema = DATABASE()
   AND table_name = 'cash_receipts'
   AND constraint_name = @constraint_name);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========================================
-- 5단계: payments.version 컬럼 보강
-- ========================================
SET @sql = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `payments` ADD COLUMN `version` VARCHAR(20) NULL AFTER `toss_response`',
    'SELECT "version already exists"'
) FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'payments'
   AND column_name = 'version');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
