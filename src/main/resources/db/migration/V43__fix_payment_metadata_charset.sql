-- V44: payment_metadata 및 orders 테이블 utf8mb4 적용 및 VARCHAR 크기 조정
-- 한글 인코딩 문제 해결 및 충분한 데이터 저장 공간 확보

-- payment_metadata 테이블 수정
ALTER TABLE `payment_metadata`
    MODIFY COLUMN `order_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '주문명',
    MODIFY COLUMN `user_agent` VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '사용자 에이전트',
    MODIFY COLUMN `payment_source` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '결제 소스';

-- payment_metadata 테이블 기본 문자셋 설정
ALTER TABLE `payment_metadata`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- orders 테이블 수정
ALTER TABLE `orders`
    MODIFY COLUMN `order_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '주문명',
    MODIFY COLUMN `order_number` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '주문번호',
    MODIFY COLUMN `order_type` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '주문 타입',
    MODIFY COLUMN `customer_key` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '고객 식별키',
    MODIFY COLUMN `toss_order_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '토스페이먼츠 orderId',
    MODIFY COLUMN `fail_reason` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '실패 사유';

-- orders 테이블 기본 문자셋 설정
ALTER TABLE `orders`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;