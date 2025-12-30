-- V34: order_items 테이블 금액 필드들을 BIGINT로 변경
-- 작성일: 2025-01-29
-- 목적: order_items 테이블의 금액 관련 필드들을 Long 타입으로 변경

-- ========================================
-- order_items 테이블 금액 필드 BIGINT 변경
-- ========================================

-- 단가 및 금액 필드들을 BIGINT로 변경
ALTER TABLE order_items MODIFY COLUMN unit_amount BIGINT NOT NULL COMMENT '단가';
ALTER TABLE order_items MODIFY COLUMN amount BIGINT NOT NULL COMMENT '총 금액';
ALTER TABLE order_items MODIFY COLUMN original_amount BIGINT NOT NULL COMMENT '원래 금액';
ALTER TABLE order_items MODIFY COLUMN discount_amount BIGINT NOT NULL COMMENT '할인 금액';
ALTER TABLE order_items MODIFY COLUMN final_amount BIGINT NOT NULL COMMENT '최종 금액';

-- ========================================
-- orders 테이블 금액 필드 BIGINT 변경
-- ========================================

-- orders 테이블의 total_amount를 BIGINT로 변경
ALTER TABLE orders MODIFY COLUMN total_amount BIGINT NOT NULL COMMENT '총 주문 금액';

-- ========================================
-- payments 테이블 금액 필드 BIGINT 변경
-- ========================================

-- payments 테이블의 amount를 BIGINT로 변경
ALTER TABLE payments MODIFY COLUMN amount BIGINT NOT NULL COMMENT '결제 금액';

-- ========================================
-- 마이그레이션 완료
-- ========================================

-- 최종 테이블 구조 확인 (주석)
-- DESCRIBE order_items;
-- DESCRIBE orders;
-- DESCRIBE payments;
