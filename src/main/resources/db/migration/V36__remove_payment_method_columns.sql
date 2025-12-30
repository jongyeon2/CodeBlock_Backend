-- ========================================
-- V36: payments 테이블 슬림화 (수정 버전)
-- ========================================

-- 1단계: payment_metadata 테이블 생성
CREATE TABLE IF NOT EXISTS payment_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL UNIQUE,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    payment_source VARCHAR(50) NULL,
    order_name VARCHAR(100) NULL,
    country VARCHAR(2) NULL,
    receipt_url VARCHAR(255) NULL,
    toss_api_version VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_id (payment_id),
    CONSTRAINT FK_payment_metadata
        FOREIGN KEY (payment_id)
        REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2단계: 데이터 마이그레이션
INSERT INTO payment_metadata
    (payment_id, order_name, country, receipt_url, toss_api_version)
SELECT
    id,
    order_name,
    country,
    receipt_url,
    version
FROM payments
WHERE order_name IS NOT NULL
    OR country IS NOT NULL
    OR receipt_url IS NOT NULL
    OR version IS NOT NULL;

-- 3단계: payments에서 부가 정보 컬럼만 제거
ALTER TABLE payments DROP COLUMN order_name;
ALTER TABLE payments DROP COLUMN country;
ALTER TABLE payments DROP COLUMN receipt_url;
ALTER TABLE payments DROP COLUMN version;
ALTER TABLE payments DROP COLUMN type;
ALTER TABLE payments DROP COLUMN balance_amount;
ALTER TABLE payments DROP COLUMN requested_at;

-- 4단계: m_id 인덱스 추가 (제거 X)
CREATE INDEX idx_payments_m_id ON payments(m_id);

-- 5단계: 최종 확인
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('payments', 'payment_metadata')
ORDER BY TABLE_NAME, ORDINAL_POSITION;