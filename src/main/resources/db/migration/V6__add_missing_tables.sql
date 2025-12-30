-- V6: 누락된 테이블 추가 (admin_profile, instructor_profile 등)

CREATE TABLE admin_profile (
    user_id BIGINT NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    department VARCHAR(60) NULL,
    phone VARCHAR(30) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id),
    CONSTRAINT FK_user_TO_admin_profile_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE instructor_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_profile_id BIGINT NOT NULL,
    channel_name VARCHAR(70) NOT NULL,
    channel_url VARCHAR(255) NOT NULL,
    bio TEXT NULL,
    contact_email VARCHAR(255) NULL,
    pay_status ENUM('ACTIVE', 'SUSPENDED', 'READY') NOT NULL DEFAULT 'READY',
    channel_status ENUM('INACTIVE', 'ACTIVE') NOT NULL DEFAULT 'INACTIVE',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profile_id (user_profile_id),
    UNIQUE KEY uk_channel_url (channel_url),
    INDEX idx_channel_status (channel_status),
    CONSTRAINT FK_user_profile_TO_instructor_profile_1 FOREIGN KEY (user_profile_id) REFERENCES user_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    zipcode VARCHAR(10) NULL,
    base_address VARCHAR(255) NULL,
    detail_address VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    CONSTRAINT FK_user_TO_address_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT FK_user_TO_wallet_1 FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_balance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'KRW',
    amount BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_currency (wallet_id, currency_code),
    CONSTRAINT FK_wallet_TO_wallet_balance_1 FOREIGN KEY (wallet_id) REFERENCES wallet (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    direction ENUM('CHARGE', 'SPEND', 'REFUND', 'ADJUST') NOT NULL,
    cookie_type ENUM('FREE', 'PAID') NOT NULL,
    cookies INT NOT NULL,
    ref_type ENUM('ORDER_ITEM', 'REFUND', 'ADMIN') NULL,
    ref_id BIGINT NULL,
    memo VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_direction (direction),
    INDEX idx_created_at (created_at),
    CONSTRAINT FK_wallet_TO_wallet_ledger_1 FOREIGN KEY (wallet_id) REFERENCES wallet (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
