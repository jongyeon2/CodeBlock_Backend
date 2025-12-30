-- V51: 장바구니 테이블 생성

CREATE TABLE cart (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '장바구니 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    course_id BIGINT NOT NULL COMMENT '강의 ID',
    name VARCHAR(255) NOT NULL COMMENT '강의명 (장바구니에 담을 당시의 이름)',
    price INT NOT NULL COMMENT '장바구니에 담을 당시의 가격',
    original_price INT NOT NULL COMMENT '원래 가격 (할인 전)',
    discount_percentage INT NOT NULL DEFAULT 0 COMMENT '할인율 (%)',
    has_discount TINYINT(1) NOT NULL DEFAULT 0 COMMENT '할인 여부',
    selected TINYINT(1) NOT NULL DEFAULT 1 COMMENT '선택 여부 (결제 시 사용)',
    added_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '장바구니에 추가된 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간 (BaseTimeEntity)',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 수정 시간',
    PRIMARY KEY (id),
    UNIQUE KEY unique_user_course (user_id, course_id) COMMENT '같은 사용자가 같은 강의를 중복으로 담을 수 없음',
    INDEX idx_user_id (user_id) COMMENT '사용자별 장바구니 조회',
    INDEX idx_course_id (course_id) COMMENT '강의별 장바구니 조회',
    INDEX idx_user_selected (user_id, selected) COMMENT '사용자의 선택된 아이템 조회',
    CONSTRAINT FK_cart_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니 테이블';

