-- V4: 결제 및 주문 테이블 생성

CREATE TABLE cookie_bundle
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255) NOT NULL,
    price_krw           INT          NOT NULL,
    base_cookie_amount  INT          NOT NULL,
    bonus_cookie_amount INT          NOT NULL DEFAULT 0,
    total_cookie_amount INT GENERATED ALWAYS AS (base_cookie_amount + bonus_cookie_amount) STORED,
    is_active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX               idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    status       ENUM('PENDING', 'PAID', 'CANCELLED', 'REFUNDED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    paid_at      DATETIME NULL,
    currency     CHAR(3)  NOT NULL DEFAULT 'KRW',
    payment_type ENUM('CASH', 'COOKIE') NOT NULL,
    cookie_spent INT NULL,
    total_amount INT      NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX        idx_user_id (user_id),
    INDEX        idx_status (status),
    INDEX        idx_created_at (created_at),
    CONSTRAINT FK_user_TO_orders_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items
(
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    orders_id        BIGINT   NOT NULL,
    course_id        BIGINT NULL,
    lecture_id       BIGINT NULL,
    cookie_bundle_id BIGINT NULL,
    item_type        ENUM('COURSE', 'LECTURE', 'COOKIE_BUNDLE') NOT NULL,
    unit_amount      INT      NOT NULL,
    amount           INT      NOT NULL,
    status           ENUM('PENDING', 'PAID', 'REFUNDED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    pay_mode         ENUM('CASH', 'COOKIE') NOT NULL,
    cookie_amount    INT NULL,
    currency         CHAR(3)  NOT NULL DEFAULT 'KRW',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX            idx_orders_id (orders_id),
    INDEX            idx_item_type (item_type),
    INDEX            idx_status (status),
    CONSTRAINT FK_orders_TO_order_items_1 FOREIGN KEY (orders_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT FK_course_TO_order_items_1 FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE SET NULL,
    CONSTRAINT FK_lecture_TO_order_items_1 FOREIGN KEY (lecture_id) REFERENCES lecture (id) ON DELETE SET NULL,
    CONSTRAINT FK_cookie_bundle_TO_order_items_1 FOREIGN KEY (cookie_bundle_id) REFERENCES cookie_bundle (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cookie_batch
(
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    user_id        BIGINT   NOT NULL,
    order_items_id BIGINT NULL,
    qty_total      INT      NOT NULL,
    qty_remain     INT      NOT NULL,
    cookie_type    ENUM('FREE', 'PAID') NOT NULL,
    source         ENUM('PURCHASE', 'BONUS', 'EVENT', 'ADMIN') NOT NULL,
    expires_at     DATETIME NULL,
    is_active      TINYINT(1) NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX          idx_user_id (user_id),
    INDEX          idx_cookie_type (cookie_type),
    INDEX          idx_is_active (is_active),
    INDEX          idx_expires_at (expires_at),
    CONSTRAINT FK_user_TO_cookie_batch_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_TO_cookie_batch_1 FOREIGN KEY (order_items_id) REFERENCES order_items (id) ON DELETE SET NULL,
    CONSTRAINT CHK_qty_valid CHECK (qty_remain >= 0 AND qty_remain <= qty_total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    orders_id       BIGINT       NOT NULL,
    method          ENUM('CARD', 'ACCOUNT', 'VIRTUAL', 'TRANSFER', 'EASYPAY') NOT NULL,
    provider        VARCHAR(20)  NOT NULL DEFAULT 'toss',
    currency        CHAR(3)      NOT NULL DEFAULT 'KRW',
    amount          INT          NOT NULL,
    status          ENUM('INIT', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'FAILED') NOT NULL DEFAULT 'INIT',
    payment_key     VARCHAR(255) NOT NULL,
    merchant_uid    VARCHAR(255) NOT NULL,
    idempotency_key CHAR(36)     NOT NULL,
    approved_at     DATETIME NULL,
    cancelled_at    DATETIME NULL,
    failure_reason  VARCHAR(255) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_key (payment_key),
    UNIQUE KEY uk_merchant_uid (merchant_uid),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    INDEX           idx_orders_id (orders_id),
    INDEX           idx_status (status),
    CONSTRAINT FK_orders_TO_payments_1 FOREIGN KEY (orders_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_allocations
(
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    payments_id    BIGINT   NOT NULL,
    order_items_id BIGINT   NOT NULL,
    amount         INT      NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX          idx_payments_id (payments_id),
    INDEX          idx_order_items_id (order_items_id),
    CONSTRAINT FK_payments_TO_payment_allocations_1 FOREIGN KEY (payments_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_TO_payment_allocations_1 FOREIGN KEY (order_items_id) REFERENCES order_items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refunds
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    payments_id  BIGINT   NOT NULL,
    type         ENUM('FULL', 'PARTIAL') NOT NULL,
    amount       INT      NOT NULL,
    status       ENUM('REQUESTED', 'APPROVED', 'FAILED', 'COMPLETED') NOT NULL DEFAULT 'REQUESTED',
    reason       VARCHAR(255) NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX        idx_payments_id (payments_id),
    INDEX        idx_status (status),
    CONSTRAINT FK_payments_TO_refunds_1 FOREIGN KEY (payments_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT CHK_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refund_allocations
(
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    refunds_id     BIGINT   NOT NULL,
    order_items_id BIGINT   NOT NULL,
    amount         INT      NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX          idx_refunds_id (refunds_id),
    INDEX          idx_order_items_id (order_items_id),
    CONSTRAINT FK_refunds_TO_refund_allocations_1 FOREIGN KEY (refunds_id) REFERENCES refunds (id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_TO_refund_allocations_1 FOREIGN KEY (order_items_id) REFERENCES order_items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
