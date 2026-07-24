USE benepay;

-- 기존 database.sql을 이미 실행한 경우 한 번만 실행하는 결제 모듈 확장 스크립트
ALTER TABLE `users`
    ADD COLUMN `pin_fail_count` INT NOT NULL DEFAULT 0 AFTER `pin_hash`,
    ADD COLUMN `pin_locked_until` DATETIME NULL AFTER `pin_fail_count`;

CREATE TABLE `payment_tokens` (
    `payment_token_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `user_card_id` BIGINT NOT NULL,
    `merchant_id` BIGINT NOT NULL,
    `expected_amount` DECIMAL(10,0) NOT NULL,
    `token_hash` CHAR(64) NOT NULL,
    `token_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, USED, EXPIRED',
    `expires_at` DATETIME NOT NULL,
    `used_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`payment_token_id`),
    UNIQUE KEY `UQ_payment_tokens_token_hash` (`token_hash`),
    KEY `IDX_payment_tokens_status_expires` (`token_status`, `expires_at`),
    CONSTRAINT `FK_users_TO_payment_tokens` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `FK_user_cards_TO_payment_tokens` FOREIGN KEY (`user_card_id`) REFERENCES `user_cards` (`user_card_id`),
    CONSTRAINT `FK_merchants_TO_payment_tokens` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`merchant_id`)
);

ALTER TABLE `payments`
    ADD COLUMN `payment_token_id` BIGINT NULL AFTER `merchant_id`,
    ADD COLUMN `pos_request_id` VARCHAR(64) NULL AFTER `payment_token_id`,
    ADD COLUMN `reward_amount` DECIMAL(10,0) NOT NULL DEFAULT 0 AFTER `discount_amount`,
    ADD COLUMN `approval_code` VARCHAR(40) NULL AFTER `payment_status`,
    ADD COLUMN `benefit_id` BIGINT NULL AFTER `approval_code`,
    ADD COLUMN `benefit_description` VARCHAR(255) NULL AFTER `benefit_id`,
    ADD COLUMN `failure_reason` VARCHAR(255) NULL AFTER `benefit_description`,
    ADD COLUMN `canceled_at` DATETIME NULL AFTER `failure_reason`;

ALTER TABLE `payments`
    ADD CONSTRAINT `UQ_payments_pos_request_id` UNIQUE (`pos_request_id`),
    ADD CONSTRAINT `FK_payment_tokens_TO_payments` FOREIGN KEY (`payment_token_id`) REFERENCES `payment_tokens` (`payment_token_id`),
    ADD CONSTRAINT `FK_card_benefits_TO_payments` FOREIGN KEY (`benefit_id`) REFERENCES `card_benefits` (`benefit_id`);

CREATE INDEX `IDX_payments_user_card_time`
    ON `payments` (`user_card_id`, `payment_time`);
