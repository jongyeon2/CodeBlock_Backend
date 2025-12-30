-- V__46: 정산 지급 및 세금계산서 테이블 추가
-- settlement_payment: 실제 지급 실행 이력 관리
-- settlement_tax_invoice: 세금계산서 발행 및 관리

-- 1) 정산 지급 테이블 생성
CREATE TABLE `settlement_payment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `settlement_ledger_id` BIGINT NOT NULL COMMENT '정산 레코드 ID',
  `payment_date` DATETIME NOT NULL COMMENT '지급 일시',
  `payment_method` VARCHAR(50) NOT NULL COMMENT '지급 방법 (BANK_TRANSFER, OTHER)',
  `bank_account_info` VARCHAR(500) NULL COMMENT '계좌 정보',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '지급 상태 (PENDING, COMPLETED, FAILED, CANCELLED)',
  `confirmation_number` VARCHAR(100) NULL COMMENT '지급 확인 번호',
  `notes` LONGTEXT NULL COMMENT '메모',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_settlement_payment_ledger` FOREIGN KEY (`settlement_ledger_id`) REFERENCES `settlement_ledger`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- settlement_payment 인덱스 추가
CREATE INDEX `idx_settlement_payment_ledger_id` ON `settlement_payment` (`settlement_ledger_id`);
CREATE INDEX `idx_settlement_payment_status` ON `settlement_payment` (`status`);
CREATE INDEX `idx_settlement_payment_date` ON `settlement_payment` (`payment_date`);
CREATE INDEX `idx_settlement_payment_confirmation` ON `settlement_payment` (`confirmation_number`);

-- 2) 세금계산서 테이블 생성
CREATE TABLE `settlement_tax_invoice` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `settlement_payment_id` BIGINT NOT NULL COMMENT '지급 레코드 ID',
  `invoice_number` VARCHAR(50) NOT NULL UNIQUE COMMENT '세금계산서 번호',
  `issue_date` DATE NOT NULL COMMENT '발행일',
  `supply_amount` INT NOT NULL COMMENT '공급가액',
  `tax_amount` INT NOT NULL COMMENT '부가세',
  `total_amount` INT NOT NULL COMMENT '합계 금액',
  `invoice_file_url` VARCHAR(500) NULL COMMENT '세금계산서 파일 URL',
  `period_start` DATE NOT NULL COMMENT '정산 시작일',
  `period_end` DATE NOT NULL COMMENT '정산 종료일',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_settlement_tax_invoice_payment` FOREIGN KEY (`settlement_payment_id`) REFERENCES `settlement_payment`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- settlement_tax_invoice 인덱스 추가
CREATE INDEX `idx_settlement_tax_invoice_payment_id` ON `settlement_tax_invoice` (`settlement_payment_id`);
CREATE INDEX `idx_settlement_tax_invoice_issue_date` ON `settlement_tax_invoice` (`issue_date`);
CREATE INDEX `idx_settlement_tax_invoice_period` ON `settlement_tax_invoice` (`period_start`, `period_end`);

-- 주석:
-- 1. settlement_payment: 정산 레코드(settlement_ledger)에 대한 실제 지급 실행 이력
--    - 한 정산 레코드에 여러 지급 시도가 있을 수 있음 (재지급 등)
--    - bank_account_info: 향후 암호화 필요 시 고려
--    - confirmation_number: 은행 이체 확인 번호 등 저장
--
-- 2. settlement_tax_invoice: 지급 건에 대한 세금계산서 발행 관리
--    - settlement_payment와 1:1 관계
--    - invoice_file_url: 로컬 파일시스템 경로 저장 (예: /uploads/tax-invoices/2024/01/INV-2024-01-00001.pdf)
--    - S3 사용 시: S3 URL 저장 (예: https://bucket.s3.region.amazonaws.com/tax-invoices/INV-2024-01-00001.pdf)
--    - invoice_number: INV-YYYY-MM-NNNNN 형식 (예: INV-2024-01-00001)
--
-- 3. 정산 흐름:
--    settlement_ledger (계산) → settlement_payment (지급 실행) → settlement_tax_invoice (세금계산서 발행)
