CREATE TABLE IF NOT EXISTS merchant_categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(30) NOT NULL UNIQUE,
    category_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    login_id VARCHAR(30) NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    pin_hash VARCHAR(100) NULL,
    pin_fail_count INT NOT NULL DEFAULT 0,
    pin_locked_until DATETIME NULL,
    name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20) NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    di VARCHAR(255) NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cards (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_name VARCHAR(50) NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    card_image_url VARCHAR(255) NULL,
    description TEXT NULL,
    is_supported BOOLEAN NOT NULL DEFAULT TRUE,
    card_company_name VARCHAR(30) NOT NULL,
    min_benefit_amount DECIMAL(8,0) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_cards (
    user_card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    card_token VARCHAR(255) NOT NULL UNIQUE,
    card_last4 CHAR(4) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    recommendation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    card_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    annual_fee DECIMAL(8,0) NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_cards_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_cards_card FOREIGN KEY (card_id) REFERENCES cards(card_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS card_benefits (
    benefit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    benefit_type VARCHAR(20) NOT NULL,
    benefit_name VARCHAR(100) NULL,
    description TEXT NULL,
    benefits_info JSON NOT NULL,
    CONSTRAINT fk_card_benefits_card FOREIGN KEY (card_id) REFERENCES cards(card_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS benefit_per_user_card (
    user_card_id BIGINT NOT NULL,
    benefit_id BIGINT NOT NULL,
    PRIMARY KEY (user_card_id, benefit_id),
    CONSTRAINT fk_benefit_user_card FOREIGN KEY (user_card_id) REFERENCES user_cards(user_card_id),
    CONSTRAINT fk_benefit_card_benefit FOREIGN KEY (benefit_id) REFERENCES card_benefits(benefit_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS merchants (
    merchant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    merchant_code VARCHAR(50) NOT NULL UNIQUE,
    merchant_name VARCHAR(100) NOT NULL,
    brand_name VARCHAR(255) NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    phone VARCHAR(20) NULL,
    CONSTRAINT fk_merchants_category FOREIGN KEY (category_id) REFERENCES merchant_categories(category_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS card_monthly_status (
    card_monthly_status_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_card_id BIGINT NOT NULL,
    total_spending_amount DECIMAL(12,0) NOT NULL DEFAULT 0,
    is_benefit_eligible BOOLEAN NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    target_year_month CHAR(6) NOT NULL,
    CONSTRAINT uk_card_monthly_status UNIQUE (user_card_id, target_year_month),
    CONSTRAINT fk_card_monthly_status_card FOREIGN KEY (user_card_id) REFERENCES user_cards(user_card_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_tokens (
    payment_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_card_id BIGINT NOT NULL,
    merchant_id BIGINT NULL,
    expected_amount DECIMAL(10,0) NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_payment_tokens_card FOREIGN KEY (user_card_id) REFERENCES user_cards(user_card_id),
    CONSTRAINT fk_payment_tokens_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    INDEX idx_payment_tokens_status_expire (token_status, expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_card_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    payment_token_id BIGINT NULL,
    pos_request_id VARCHAR(64) NULL UNIQUE,
    payment_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    original_amount DECIMAL(10,0) NOT NULL,
    discount_amount DECIMAL(10,0) NOT NULL DEFAULT 0,
    reward_amount DECIMAL(10,0) NOT NULL DEFAULT 0,
    final_amount DECIMAL(10,0) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_code VARCHAR(40) NULL,
    benefit_id BIGINT NULL,
    benefit_description VARCHAR(255) NULL,
    failure_reason VARCHAR(255) NULL,
    canceled_at DATETIME NULL,
    cancellation_reason VARCHAR(255) NULL,
    CONSTRAINT fk_payments_card FOREIGN KEY (user_card_id) REFERENCES user_cards(user_card_id),
    CONSTRAINT fk_payments_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    CONSTRAINT fk_payments_token FOREIGN KEY (payment_token_id) REFERENCES payment_tokens(payment_token_id),
    CONSTRAINT fk_payments_benefit FOREIGN KEY (benefit_id) REFERENCES card_benefits(benefit_id),
    INDEX idx_payments_card_time (user_card_id, payment_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_delivery_outbox (
    outbox_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    payload_json TEXT NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at DATETIME NULL,
    CONSTRAINT uk_payment_outbox UNIQUE (payment_id, event_type),
    CONSTRAINT fk_payment_outbox_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    INDEX idx_outbox_retry (delivery_status, next_retry_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS card_performance_events (
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id, event_type),
    CONSTRAINT fk_card_performance_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookmarked_stores (
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, merchant_id),
    CONSTRAINT fk_bookmarked_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_bookmarked_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id)
) ENGINE=InnoDB;
