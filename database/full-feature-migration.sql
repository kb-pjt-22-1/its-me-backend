USE benepay;

ALTER TABLE user_cards ADD COLUMN card_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER recommendation_enabled;
ALTER TABLE payment_tokens MODIFY merchant_id BIGINT NULL, MODIFY expected_amount DECIMAL(10,0) NULL;
ALTER TABLE payments ADD COLUMN cancellation_reason VARCHAR(255) NULL AFTER canceled_at;

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

UPDATE user_cards SET card_status='ACTIVE' WHERE card_status IS NULL OR card_status='';
