-- V16: 결제 및 환불 관련 필수 컬럼 추가
-- 토스페이먼츠 연동 및 비즈니스 로직 완성을 위한 추가 컬럼들

-- 1) payments 테이블에 카드 정보 및 보안 컬럼 추가
ALTER TABLE `payments`
    -- 카드 정보 (토스 응답에서 파싱해서 저장)
    ADD COLUMN `card_number_masked` VARCHAR(20) NULL COMMENT '마스킹된 카드번호',
    ADD COLUMN `card_company` VARCHAR(50) NULL COMMENT '카드사명', 
    ADD COLUMN `installment_plan_months` INT NULL COMMENT '할부 개월 수',
    ADD COLUMN `card_type` VARCHAR(20) NULL COMMENT '카드 타입 (CREDIT, DEBIT, GIFT)',
    ADD COLUMN `owner_type` VARCHAR(20) NULL COMMENT '소유자 타입 (PERSONAL, CORPORATE)',
    
    -- 가상계좌 정보 (가상계좌 결제 시)
    ADD COLUMN `virtual_account_number` VARCHAR(50) NULL COMMENT '가상계좌 번호',
    ADD COLUMN `virtual_account_bank` VARCHAR(50) NULL COMMENT '가상계좌 은행',
    ADD COLUMN `virtual_account_due_date` DATETIME NULL COMMENT '가상계좌 입금 만료일',
    ADD COLUMN `virtual_account_holder_name` VARCHAR(100) NULL COMMENT '가상계좌 예금주명',
    
    -- 계좌이체 정보
    ADD COLUMN `transfer_bank` VARCHAR(50) NULL COMMENT '이체 은행',
    ADD COLUMN `transfer_account_number` VARCHAR(50) NULL COMMENT '이체 계좌번호',
    
    -- 간편결제 정보
    ADD COLUMN `easy_pay_provider` VARCHAR(50) NULL COMMENT '간편결제 제공업체',
    ADD COLUMN `easy_pay_method` VARCHAR(50) NULL COMMENT '간편결제 수단',
    
    -- 휴대폰 결제 정보
    ADD COLUMN `mobile_phone_carrier` VARCHAR(50) NULL COMMENT '통신사',
    ADD COLUMN `mobile_phone_number` VARCHAR(20) NULL COMMENT '휴대폰 번호',
    
    -- 보안/추적 정보 (사기 방지용)
    ADD COLUMN `ip_address` VARCHAR(45) NULL COMMENT '결제 요청 IP 주소',
    ADD COLUMN `user_agent` VARCHAR(500) NULL COMMENT '결제 요청 User-Agent',
    ADD COLUMN `payment_source` VARCHAR(50) NULL COMMENT '결제 소스 (WEB, MOBILE, API)',
    
    -- 재시도 관련 (향후 확장용)
    ADD COLUMN `retry_count` INT NOT NULL DEFAULT 0 COMMENT '결제 재시도 횟수',
    ADD COLUMN `last_retry_at` DATETIME NULL COMMENT '마지막 재시도 시간',
    ADD COLUMN `max_retry_count` INT NOT NULL DEFAULT 3 COMMENT '최대 재시도 횟수',
    
    -- 현금영수증 정보
    ADD COLUMN `cash_receipt_issued` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '현금영수증 발급 여부',
    ADD COLUMN `cash_receipt_type` VARCHAR(20) NULL COMMENT '현금영수증 타입 (소득공제, 지출증빙)',
    ADD COLUMN `cash_receipt_number` VARCHAR(50) NULL COMMENT '현금영수증 번호';

    -- 2) refunds 테이블에 비즈니스 로직용 컬럼 추가
    ALTER TABLE `refunds`
    ADD COLUMN `order_id` BIGINT NOT NULL COMMENT '주문 ID',
    ADD COLUMN `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
    ADD COLUMN `refund_key` VARCHAR(255) NULL COMMENT '환불 키 (토스 refundKey)',
    ADD COLUMN `refund_reason` VARCHAR(255) NULL COMMENT '환불 사유',
    ADD COLUMN `refund_method` VARCHAR(50) NULL COMMENT '환불 방법',
    ADD COLUMN `refund_bank` VARCHAR(50) NULL COMMENT '환불 은행',
    ADD COLUMN `refund_account_number` VARCHAR(50) NULL COMMENT '환불 계좌번호',
    ADD COLUMN `refund_holder_name` VARCHAR(100) NULL COMMENT '환불 예금주명',
    ADD CONSTRAINT `FK_refunds_TO_orders` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
    ADD CONSTRAINT `FK_refunds_TO_users` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 3) order_items 테이블에 수량 컬럼 추가 (V15에는 coupon_id, original_amount, discount_amount, final_amount가 이미 있음)
ALTER TABLE `order_items`
    ADD COLUMN `quantity` INT NOT NULL DEFAULT 1 COMMENT '수량';

-- 4) wallet_balance 테이블에 동결 금액 관리 추가
ALTER TABLE `wallet_balance`
    ADD COLUMN `frozen_amount` BIGINT NOT NULL DEFAULT 0 COMMENT '동결된 금액',
    ADD COLUMN `available_amount` BIGINT GENERATED ALWAYS AS (`amount` - `frozen_amount`) STORED COMMENT '사용 가능 금액',
    ADD COLUMN `last_updated_by` VARCHAR(50) NULL COMMENT '마지막 업데이트 주체',
    ADD COLUMN `update_reason` VARCHAR(255) NULL COMMENT '업데이트 사유',
    ADD COLUMN `daily_limit` BIGINT NULL COMMENT '일일 한도',
    ADD COLUMN `monthly_limit` BIGINT NULL COMMENT '월 한도';

-- 5) 인덱스 추가 (성능 최적화)
CREATE INDEX `idx_payments_card_company` ON `payments` (`card_company`);
CREATE INDEX `idx_payments_ip_address` ON `payments` (`ip_address`);
CREATE INDEX `idx_payments_payment_source` ON `payments` (`payment_source`);
CREATE INDEX `idx_payments_retry_count` ON `payments` (`retry_count`);
CREATE INDEX `idx_payments_virtual_account_number` ON `payments` (`virtual_account_number`);
CREATE INDEX `idx_payments_cash_receipt_issued` ON `payments` (`cash_receipt_issued`);

CREATE INDEX `idx_refunds_order_id` ON `refunds` (`order_id`);
CREATE INDEX `idx_refunds_user_id` ON `refunds` (`user_id`);
CREATE INDEX `idx_refunds_user_status` ON `refunds` (`user_id`, `status`);
CREATE INDEX `idx_refunds_refund_key` ON `refunds` (`refund_key`);

-- idx_order_items_coupon_id는 V15에서 이미 생성됨
CREATE INDEX `idx_order_items_quantity` ON `order_items` (`quantity`);

CREATE INDEX `idx_wallet_balance_frozen_amount` ON `wallet_balance` (`frozen_amount`);

-- 6) 제약조건 추가 (데이터 무결성)
ALTER TABLE `order_items`
    ADD CONSTRAINT `CHK_order_items_quantity_positive` CHECK (`quantity` > 0);

ALTER TABLE `wallet_balance`
    ADD CONSTRAINT `CHK_wallet_balance_frozen_valid` CHECK (`frozen_amount` >= 0 AND `frozen_amount` <= `amount`),
    ADD CONSTRAINT `CHK_wallet_balance_limits_valid` CHECK (`daily_limit` IS NULL OR `daily_limit` > 0),
    ADD CONSTRAINT `CHK_wallet_balance_monthly_limit_valid` CHECK (`monthly_limit` IS NULL OR `monthly_limit` > 0);

ALTER TABLE `payments`
    ADD CONSTRAINT `CHK_payments_retry_count_valid` CHECK (`retry_count` >= 0 AND `retry_count` <= `max_retry_count`),
    ADD CONSTRAINT `CHK_payments_installment_valid` CHECK (`installment_plan_months` IS NULL OR `installment_plan_months` > 0);

-- 7) 테스트용 더미 데이터는 주석 처리 (프로덕션 환경에서 불필요)
-- 개발 환경에서는 R__insert_test_data.sql을 사용하세요

-- INSERT INTO `orders` (
--     `user_id`, `status`, `currency`, `payment_type`, `total_amount`, 
--     `order_number`, `order_type`, `is_mixed_forbidden_violation`, `total_discount_amount`, `coupon_count`,
--     `created_at`, `updated_at`
-- ) VALUES (
--     1, 'PAID', 'KRW', 'CASH', 50000,
--     'TEST-ORDER-V16-001', 'PURCHASE', 0, 0, 0,
--     NOW(), NOW()
-- );

-- INSERT INTO `order_items` (
--     `orders_id`, `course_id`, `item_type`, `unit_amount`, `amount`, 
--     `status`, `pay_mode`, `currency`, `original_amount`, `discount_amount`, 
--     `final_amount`, `quantity`, `created_at`
-- ) VALUES (
--     LAST_INSERT_ID(), 1, 'COURSE', 50000, 50000,
--     'PAID', 'CASH', 'KRW', 50000, 0, 50000, 1, NOW()
-- );

-- INSERT INTO `payments` (
--     `orders_id`, `method`, `provider`, `currency`, `amount`, `status`,
--     `payment_key`, `merchant_uid`, `idempotency_key`, `approved_at`,
--     `card_number_masked`, `card_company`, `ip_address`, `payment_source`,
--     `retry_count`, `max_retry_count`, `cash_receipt_issued`,
--     `created_at`, `updated_at`
-- ) VALUES (
--     LAST_INSERT_ID(), 'CARD', 'toss', 'KRW', 50000, 'CAPTURED',
--     'test_payment_key_v16_001', 'test_merchant_v16_001', UUID(), NOW(),
--     '1234-****-****-5678', '신한카드', '127.0.0.1', 'WEB',
--     0, 3, 0,
--     NOW(), NOW()
-- );

-- INSERT INTO `refunds` (
--     `payments_id`, `type`, `amount`, `status`, `reason`, `created_at`,
--     `order_id`, `user_id`, `refund_route`, `refund_amount_cash`
-- ) VALUES (
--     LAST_INSERT_ID(), 'FULL', 50000, 'PENDING', '테스트 환불', NOW(),
--     (SELECT id FROM orders WHERE order_number = 'TEST-ORDER-V16-001'), 1, 'CASH', 50000
-- );
