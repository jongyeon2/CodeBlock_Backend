-- V15: 결제 관련해서 추가되는 테이블들과 칼럼들 
-- 쿠폰 시스템 테이블 추가(쿠폰 생성, 발급, 사용, 관리 시스템 구축)

-- 1) ALTER TABLE orders(order 테이블 추가)
ALTER TABLE `orders`
  ADD COLUMN `order_number` VARCHAR(64) NOT NULL,
  ADD COLUMN `order_type` VARCHAR(20) NOT NULL,
  ADD COLUMN `refundable_until` DATETIME NULL,
  ADD COLUMN `first_viewed_at` DATETIME NULL,
  ADD COLUMN `idempotency_key` VARCHAR(36) NULL,
  ADD COLUMN `is_mixed_forbidden_violation` TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN `policy_snapshot` JSON NULL,
  ADD COLUMN `cancelled_at` DATETIME NULL,
  ADD COLUMN `fail_reason` LONGTEXT NULL,
  -- 토스페이먼츠 연동 필드 추가
  ADD COLUMN `toss_order_id` VARCHAR(64) NULL COMMENT '토스페이먼츠 orderId',
  ADD COLUMN `order_name` VARCHAR(100) NULL COMMENT '주문명',
  ADD COLUMN `customer_key` VARCHAR(100) NULL COMMENT '고객 식별키',
  -- 쿠폰 관련
  ADD COLUMN `total_discount_amount` INT NOT NULL DEFAULT 0 COMMENT '전체 할인 금액',
  ADD COLUMN `coupon_count` INT NOT NULL DEFAULT 0 COMMENT '사용된 쿠폰 수';

-- unique로 설정한 인덱스 추가(결제 중복방지를 위함)
ALTER TABLE `orders`
  ADD CONSTRAINT `uk_orders_order_number` UNIQUE (`order_number`);

CREATE INDEX `idx_orders_refundable_until` ON `orders` (`refundable_until`);
CREATE INDEX `idx_orders_idempotency_key` ON `orders` (`idempotency_key`);
CREATE INDEX `idx_orders_toss_order_id` ON `orders` (`toss_order_id`);

-- 2) 결제 정보 추가 (Toss 메타데이터 & 웹훅 트레이스)
ALTER TABLE `payments`
  ADD COLUMN `webhook_received_at` DATETIME NULL,
  ADD COLUMN `receipt_url` VARCHAR(255) NULL,
  -- 토스페이먼츠 API 연동 필드 추가
  ADD COLUMN `order_name` VARCHAR(100) NULL COMMENT '구매상품명',
  ADD COLUMN `m_id` VARCHAR(14) NULL COMMENT '상점아이디(MID)',
  ADD COLUMN `balance_amount` INT NULL COMMENT '취소 가능 금액',
  ADD COLUMN `requested_at` DATETIME NULL COMMENT '결제 요청 시간',
  ADD COLUMN `supplied_amount` INT NULL COMMENT '공급가액',
  ADD COLUMN `vat` INT NULL COMMENT '부가세',
  ADD COLUMN `tax_free_amount` INT NULL COMMENT '면세금액',
  ADD COLUMN `type` VARCHAR(20) NULL COMMENT '결제 타입 (NORMAL, BILLING, BRANDPAY)',
  ADD COLUMN `country` VARCHAR(2) NULL COMMENT '결제 국가 코드',
  ADD COLUMN `toss_response` JSON NULL COMMENT '토스페이먼츠 전체 응답 저장',
  ADD COLUMN `version` VARCHAR(20) NULL COMMENT 'Payment 객체 버전';

CREATE INDEX `idx_payments_webhook_received_at` ON `payments` (`webhook_received_at`);
CREATE INDEX `idx_payments_type` ON `payments` (`type`);

-- 3) 환불 정보 추가 (route/amounts/timeline)
ALTER TABLE `refunds`
  ADD COLUMN `refund_route` VARCHAR(10) NOT NULL DEFAULT 'CASH',
  ADD COLUMN `refund_amount_cash` INT NULL,
  ADD COLUMN `refund_amount_cookie` INT NULL,
  ADD COLUMN `processed_at` DATETIME NULL,
  ADD COLUMN `processor_admin_id` BIGINT NULL,
  ADD COLUMN `requested_at` DATETIME NULL;
  

CREATE INDEX `idx_refunds_requested_at` ON `refunds` (`requested_at`);
CREATE INDEX `idx_refunds_processed_at` ON `refunds` (`processed_at`);

-- 4) order_items 테이블에 쿠폰 할인 정보 추가
ALTER TABLE `order_items`
  ADD COLUMN `coupon_id` BIGINT NULL COMMENT '사용된 쿠폰 ID',
  ADD COLUMN `original_amount` INT NOT NULL DEFAULT 0 COMMENT '원래 금액',
  ADD COLUMN `discount_amount` INT NOT NULL DEFAULT 0 COMMENT '할인 금액',
  ADD COLUMN `final_amount` INT NOT NULL DEFAULT 0 COMMENT '최종 금액';

-- 5) 정산 보류 (7일 보류 및 해제)
CREATE TABLE `settlement_hold` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `hold_until` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `released_at` DATETIME NULL,
  `cancelled_at` DATETIME NULL,
  CONSTRAINT `fk_settlement_hold_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
  CONSTRAINT `fk_settlement_hold_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB;

CREATE INDEX `idx_settlement_hold_hold_until` ON `settlement_hold` (`hold_until`, `status`);

-- 6) 정산 정보 추가 (강사 정산)
CREATE TABLE `settlement_ledger` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `instructor_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `net_amount` INT NOT NULL,
  `fee_amount` INT NOT NULL,
  `rate` DOUBLE NOT NULL,
  `eligible_flag` TINYINT(1) NOT NULL DEFAULT 0,
  `settled_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_settlement_ledger_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
  CONSTRAINT `fk_settlement_ledger_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB;

CREATE INDEX `idx_settlement_ledger_instructor` ON `settlement_ledger` (`instructor_id`, `settled_at`);

-- 7) 쿠키 지갑 이동 정보 수정 (V6의 wallet_ledger를 V15 스키마로 변경)

-- V6의 wallet_ledger 테이블을 V15 스키마로 수정
-- 1) 새 컬럼 추가 (NULL 허용으로 시작)
ALTER TABLE `wallet_ledger`
  ADD COLUMN `user_id` BIGINT NULL AFTER `id`,
  ADD COLUMN `order_id` BIGINT NULL AFTER `user_id`,
  ADD COLUMN `payment_id` BIGINT NULL AFTER `order_id`,
  ADD COLUMN `batch_id` BIGINT NULL AFTER `payment_id`,
  ADD COLUMN `type` VARCHAR(20) NULL AFTER `batch_id`,
  ADD COLUMN `balance_after` INT NULL AFTER `cookies`;

-- 2) 컬럼명 변경
ALTER TABLE `wallet_ledger`
  CHANGE COLUMN `cookies` `cookie_amount` INT NOT NULL,
  CHANGE COLUMN `memo` `notes` LONGTEXT NULL;

-- 3) user_id 백필 (wallet 테이블에서 가져오기)
UPDATE `wallet_ledger` wl
  JOIN `wallet` w ON wl.`wallet_id` = w.`id`
SET wl.`user_id` = w.`user_id`
WHERE wl.`user_id` IS NULL;

-- 4) NOT NULL 제약 추가
ALTER TABLE `wallet_ledger`
  MODIFY COLUMN `user_id` BIGINT NOT NULL,
  MODIFY COLUMN `type` VARCHAR(20) NOT NULL DEFAULT 'CHARGE',
  MODIFY COLUMN `balance_after` INT NOT NULL DEFAULT 0;

-- 5) 레거시 컬럼 제거
ALTER TABLE `wallet_ledger`
  DROP FOREIGN KEY `FK_wallet_TO_wallet_ledger_1`;

ALTER TABLE `wallet_ledger`
  DROP COLUMN `wallet_id`,
  DROP COLUMN `direction`,
  DROP COLUMN `cookie_type`,
  DROP COLUMN `ref_type`,
  DROP COLUMN `ref_id`,
  DROP COLUMN `is_active`;

-- 6) 레거시 인덱스 제거 (에러 무시)
SET @sql = 'DROP INDEX `idx_wallet_id` ON `wallet_ledger`';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'wallet_ledger' 
   AND INDEX_NAME = 'idx_wallet_id') > 0,
  @sql, 'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'DROP INDEX `idx_direction` ON `wallet_ledger`';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
   WHERE TABLE_SCHEMA = DATABASE() 
   AND TABLE_NAME = 'wallet_ledger' 
   AND INDEX_NAME = 'idx_direction') > 0,
  @sql, 'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7) 새 외래키 추가
ALTER TABLE `wallet_ledger`
  ADD CONSTRAINT `fk_wallet_ledger_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  ADD CONSTRAINT `fk_wallet_ledger_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
  ADD CONSTRAINT `fk_wallet_ledger_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments`(`id`),
  ADD CONSTRAINT `fk_wallet_ledger_batch` FOREIGN KEY (`batch_id`) REFERENCES `cookie_batch`(`id`);

-- 8) 새 인덱스 추가
CREATE INDEX `idx_wallet_ledger_user_created` ON `wallet_ledger` (`user_id`, `created_at`);
CREATE INDEX `idx_wallet_ledger_batch` ON `wallet_ledger` (`batch_id`);

-- 8) 쿠키 충전 로그 추가 (충전 정산 로그)
CREATE TABLE `cookie_charge_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `payment_id` BIGINT NOT NULL,
  `cookie_quantity` INT NOT NULL,
  `cash_amount` INT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_cookie_charge_log_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_cookie_charge_log_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
  CONSTRAINT `fk_cookie_charge_log_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments`(`id`)
) ENGINE=InnoDB;

CREATE INDEX `idx_cookie_charge_log_user_created` ON `cookie_charge_log` (`user_id`, `created_at`);
CREATE INDEX `idx_cookie_charge_log_order` ON `cookie_charge_log` (`order_id`);
CREATE INDEX `idx_cookie_charge_log_payment` ON `cookie_charge_log` (`payment_id`);

-- 9) 환불 요청 정보 추가 (환불 감사 로그)
CREATE TABLE `refund_requests` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `refund_route` VARCHAR(10) NOT NULL, -- CASH or COOKIE
  `reason` LONGTEXT NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
  `refund_amount_cash` INT NULL,
  `refund_amount_cookie` INT NULL,
  `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` DATETIME NULL,
  `processor_admin_id` BIGINT NULL,
  CONSTRAINT `fk_refund_requests_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
  CONSTRAINT `fk_refund_requests_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB;

CREATE INDEX `idx_refund_requests_order` ON `refund_requests` (`order_id`);
CREATE INDEX `idx_refund_requests_user_requested` ON `refund_requests` (`user_id`, `requested_at`);

-- 10) 중복 요청 방지 키 추가 (중복 요청 방지)
CREATE TABLE `idempotency_keys` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `idempotency_key` VARCHAR(36) NOT NULL,
  `request_hash` VARCHAR(128) NULL,
  `response_snapshot` JSON NULL,
  `status` VARCHAR(20) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NULL,
  CONSTRAINT `fk_idempotency_keys_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `uk_idempotency_keys_user_key` UNIQUE (`user_id`, `idempotency_key`)
) ENGINE=InnoDB;

-- 11) 일일 한도 집계 추가 (일일 한도 집계)
CREATE TABLE `daily_limit_aggregate` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `date` DATE NOT NULL,
  `cash_sum` INT NOT NULL DEFAULT 0,
  `cookie_sum` INT NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_daily_limit_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `uk_daily_limit_user_date` UNIQUE (`user_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12) 토스페이먼츠 연동을 위한 추가 테이블들

-- 프로모션 정보 테이블 (정규화)
CREATE TABLE `payment_promotions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_id` BIGINT NOT NULL,
  `promotion_type` ENUM('CARD_DISCOUNT', 'CARD_INTEREST_FREE', 'BANK_DISCOUNT', 'CARD_POINT') NOT NULL,
  `issuer_code` VARCHAR(10) NULL COMMENT '카드사/은행 코드',
  `discount_amount` INT NULL COMMENT '할인 금액',
  `discount_rate` DOUBLE NULL COMMENT '할인 비율',
  `minimum_amount` INT NULL COMMENT '최소 결제 금액',
  `maximum_amount` INT NULL COMMENT '최대 결제 금액',
  `due_date` DATETIME NULL COMMENT '프로모션 종료일',
  `discount_code` VARCHAR(50) NULL COMMENT '프로모션 코드',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_payment_promotions_payment_id` (`payment_id`),
  CONSTRAINT `fk_payment_promotions_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 결제수단별 상세 정보 테이블 (정규화)
CREATE TABLE `payment_method_details` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_id` BIGINT NOT NULL,
  `method_type` ENUM('CARD', 'VIRTUAL_ACCOUNT', 'TRANSFER', 'MOBILE', 'EASYPAY', 'GIFT_CERTIFICATE') NOT NULL,
  `method_data` JSON NOT NULL COMMENT '결제수단별 상세 정보',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_method` (`payment_id`, `method_type`),
  CONSTRAINT `fk_payment_method_details_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 웹훅 이벤트 로그 테이블
CREATE TABLE `webhook_events` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_id` BIGINT NOT NULL,
  `event_type` VARCHAR(50) NOT NULL,
  `event_data` JSON NOT NULL,
  `processed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_webhook_events_payment_id` (`payment_id`),
  INDEX `idx_webhook_events_event_type` (`event_type`),
  CONSTRAINT `fk_webhook_events_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13) 쿠폰 시스템 테이블들

-- 쿠폰 마스터 테이블 생성
CREATE TABLE `coupons` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '쿠폰명',
  `description` LONGTEXT NULL COMMENT '쿠폰 설명',
  `type` ENUM('DISCOUNT_PERCENTAGE', 'DISCOUNT_AMOUNT') NOT NULL COMMENT '쿠폰 타입',
  `discount_value` INT NOT NULL COMMENT '할인 값 (퍼센트 또는 금액)',
  `minimum_amount` INT NULL COMMENT '최소 사용 금액',
  `maximum_discount` INT NULL COMMENT '최대 할인 금액',
  `valid_from` DATETIME NOT NULL COMMENT '유효 시작일',
  `valid_until` DATETIME NOT NULL COMMENT '유효 종료일',
  `usage_limit` INT NULL COMMENT '사용 제한 횟수 (NULL = 무제한)',
  `used_count` INT NOT NULL DEFAULT 0 COMMENT '사용된 횟수',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
  `created_by` BIGINT NULL COMMENT '생성자 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_coupons_type` (`type`),
  INDEX `idx_coupons_valid_period` (`valid_from`, `valid_until`),
  INDEX `idx_coupons_is_active` (`is_active`),
  INDEX `idx_coupons_created_by` (`created_by`),
  CONSTRAINT `fk_coupons_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 쿠폰 보유 테이블 생성 (user와 1:N)
CREATE TABLE `user_coupons` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
  `coupon_id` BIGINT NOT NULL COMMENT '쿠폰 ID',
  `coupon_code` VARCHAR(50) NOT NULL COMMENT '쿠폰 코드',
  `is_used` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '사용 여부',
  `used_at` DATETIME NULL COMMENT '사용 일시',
  `expires_at` DATETIME NOT NULL COMMENT '만료 일시',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon_code` (`user_id`, `coupon_code`),
  INDEX `idx_user_coupons_user_id` (`user_id`),
  INDEX `idx_user_coupons_coupon_id` (`coupon_id`),
  INDEX `idx_user_coupons_is_used` (`is_used`),
  INDEX `idx_user_coupons_expires_at` (`expires_at`),
  CONSTRAINT `fk_user_coupons_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_coupons_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 쿠폰 사용 내역 테이블 생성
CREATE TABLE `coupon_usage_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_coupon_id` BIGINT NOT NULL COMMENT '사용자 쿠폰 ID',
  `order_id` BIGINT NOT NULL COMMENT '주문 ID',
  `order_item_id` BIGINT NULL COMMENT '주문 아이템 ID (NULL = 전체 주문 할인)',
  `discount_amount` INT NOT NULL COMMENT '할인 금액',
  `used_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_coupon_usage_user_coupon` (`user_coupon_id`),
  INDEX `idx_coupon_usage_order` (`order_id`),
  INDEX `idx_coupon_usage_order_item` (`order_item_id`),
  INDEX `idx_coupon_usage_used_at` (`used_at`),
  CONSTRAINT `fk_coupon_usage_user_coupon` FOREIGN KEY (`user_coupon_id`) REFERENCES `user_coupons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coupon_usage_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coupon_usage_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 쿠폰 발급 이벤트 테이블 생성 (관리자용)
CREATE TABLE `coupon_events` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '이벤트명',
  `description` LONGTEXT NULL COMMENT '이벤트 설명',
  `coupon_id` BIGINT NOT NULL COMMENT '쿠폰 ID',
  `target_users` ENUM('ALL', 'NEW', 'EXISTING', 'SPECIFIC') NOT NULL COMMENT '대상 사용자',
  `target_user_ids` JSON NULL COMMENT '특정 사용자 ID 목록',
  `issued_count` INT NOT NULL DEFAULT 0 COMMENT '발급된 수',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
  `created_by` BIGINT NULL COMMENT '생성자 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_coupon_events_coupon_id` (`coupon_id`),
  INDEX `idx_coupon_events_is_active` (`is_active`),
  INDEX `idx_coupon_events_created_by` (`created_by`),
  CONSTRAINT `fk_coupon_events_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coupon_events_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14) 외래키 제약조건 추가
ALTER TABLE `order_items`
  ADD CONSTRAINT `fk_order_items_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`) ON DELETE SET NULL;

-- 15) 인덱스 추가 (성능 최적화)
CREATE INDEX `idx_order_items_coupon_id` ON `order_items` (`coupon_id`);
CREATE INDEX `idx_orders_total_discount` ON `orders` (`total_discount_amount`);
CREATE INDEX `idx_orders_coupon_count` ON `orders` (`coupon_count`);

-- 16) 초기 데이터 삽입 (테스트용)
INSERT INTO `coupons` (`name`, `description`, `type`, `discount_value`, `minimum_amount`, `maximum_discount`, `valid_from`, `valid_until`, `usage_limit`, `is_active`, `created_by`) VALUES
('신규회원 10% 할인', '신규회원을 위한 10% 할인 쿠폰', 'DISCOUNT_PERCENTAGE', 10, 10000, 5000, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1000, 1, NULL),
('5,000원 할인 쿠폰', '20,000원 이상 구매 시 5,000원 할인', 'DISCOUNT_AMOUNT', 5000, 20000, 5000, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 500, 1, NULL),
('15% 할인 쿠폰', '30,000원 이상 구매 시 15% 할인 (최대 10,000원)', 'DISCOUNT_PERCENTAGE', 15, 30000, 10000, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 200, 1, NULL);