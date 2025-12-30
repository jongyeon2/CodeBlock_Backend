
-- 1. cookie_charge_log 테이블의 데이터를 wallet_ledger로 마이그레이션 (필요한 경우)
-- 기존 cookie_charge_log 데이터가 있다면 wallet_ledger로 이관
INSERT INTO wallet_ledger (user_id, order_id, payment_id, type, cookie_amount, balance_after, notes, created_at)
SELECT
    ccl.user_id,
    ccl.order_id,
    ccl.payment_id,
    'CHARGE' as type,
    ccl.cookie_quantity as cookie_amount,
    0 as balance_after, -- 기존 데이터이므로 balance_after는 0으로 설정
    CONCAT('Migrated from cookie_charge_log: ', ccl.cash_amount, '원 충전') as notes,
    ccl.created_at
FROM cookie_charge_log ccl
WHERE NOT EXISTS (
    SELECT 1 FROM wallet_ledger wl
    WHERE wl.user_id = ccl.user_id
    AND wl.order_id = ccl.order_id
    AND wl.payment_id = ccl.payment_id
);

-- 2. cookie_charge_log 테이블 삭제
DROP TABLE IF EXISTS cookie_charge_log;
-- Course는 현금 결제만 가능하도록 수정


-- 쿠폰 상태 필드 추가
ALTER TABLE user_coupons ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

-- 기존 데이터 상태 설정
-- is_used = true인 경우 USED
UPDATE user_coupons SET status = 'USED' WHERE is_used = true;

-- is_used = false이고 expires_at이 지난 경우 EXPIRED
UPDATE user_coupons SET status = 'EXPIRED' WHERE is_used = false AND expires_at < NOW();

-- 나머지는 AVAILABLE (이미 DEFAULT이므로 불필요하지만 명시적으로 설정)
UPDATE user_coupons SET status = 'AVAILABLE' WHERE status = 'AVAILABLE' OR status IS NULL;

-- 인덱스 추가 (쿠폰 목록 조회 성능 개선)
CREATE INDEX idx_user_coupons_status ON user_coupons(status);
CREATE INDEX idx_user_coupons_user_status ON user_coupons(user_id, status);