DROP DATABASE IF EXISTS benepay;
CREATE DATABASE benepay;
USE benepay;

CREATE TABLE `card_benefits` (
                                 `benefit_id`	BIGINT	NOT NULL,
                                 `card_id`	BIGINT	NOT NULL,
                                 `benefit_type`	VARCHAR(20)	NOT NULL	COMMENT '환급, 청구, 현장',
                                 `benefit_name`	VARCHAR(100)	NULL,
                                 `description`	TEXT	NULL,
                                 `benefits_info`	JSON	NOT NULL
);

CREATE TABLE `merchants` (
                             `merchant_id`	BIGINT	NOT NULL,
                             `category_id`	BIGINT	NOT NULL,
                             `merchant_code`	VARCHAR(50)	NOT NULL,
                             `merchant_name`	VARCHAR(100)	NOT NULL,
                             `brand_name`	VARCHAR(255)	NULL,
                             `address`	VARCHAR(255)	NOT NULL,
                             `latitude`	DECIMAL(10,7)	NOT NULL,
                             `longitude`	DECIMAL(10,7)	NOT NULL,
                             `phone`	VARCHAR(20)	NULL
);

CREATE TABLE `payments` (
                            `payment_id`	BIGINT	NOT NULL,
                            `user_card_id`	BIGINT	NOT NULL,
                            `merchant_id`	BIGINT	NOT NULL,
                            `payment_time`	DATETIME	NOT NULL,
                            `original_amount`	DECIMAL(10,0)	NOT NULL,
                            `discount_amount`	DECIMAL(10,0)	NOT NULL	DEFAULT 0,
                            `final_amount`	DECIMAL(10,0)	NOT NULL,
                            `payment_status`	VARCHAR(20)	NOT NULL	DEFAULT 'PENDING'	COMMENT 'APPROVED
CANCELED'
);

CREATE TABLE `merchant_categories` (
                                       `category_id`	BIGINT	NOT NULL,
                                       `category_code`	VARCHAR(30)	NOT NULL,
                                       `category_name`	VARCHAR(50)	NOT NULL
);

CREATE TABLE `user_cards` (
                              `user_card_id`	BIGINT	NOT NULL,
                              `user_id`	BIGINT	NOT NULL,
                              `card_id`	BIGINT	NOT NULL,
                              `card_token`	VARCHAR(255)	NOT NULL,
                              `card_last4`	CHAR(4)	NOT NULL,
                              `is_primary`	BOOLEAN	NOT NULL	DEFAULT FALSE,
                              `recommendation_enabled`	BOOLEAN	NOT NULL	DEFAULT TRUE,
                              `registered_at`	DATETIME	NOT NULL,
                              `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE,
                              `annual_fee`	DECIMAL(8,0)	NOT NULL	DEFAULT 0
);

CREATE TABLE `card_monthly_status` (
                                       `card_monthly_status_id`	BIGINT	NOT NULL,
                                       `user_card_id`	BIGINT	NOT NULL,
                                       `total_spending_amount`	DECIMAL(12,0)	NOT NULL,
                                       `is_benefit_eligible`	BOOLEAN	NULL,
                                       `updated_at`	DATETIME	NOT NULL	COMMENT '해당 월별 실적 데이터가 마지막으로 변경된 시간',
                                       `target_year_month`	CHAR(6)	NOT NULL
);

CREATE TABLE `benefit_per_user_card` (
                                         `user_card_id`	BIGINT	NOT NULL,
                                         `benefit_id`	BIGINT	NOT NULL
);

CREATE TABLE `users` (
                         `user_id`	BIGINT	NOT NULL,
                         `email`	VARCHAR(255)	NOT NULL	COMMENT 'UNIQUE',
                         `login_id`	VARCHAR(30)	NULL,
                         `password_hash`	VARCHAR(100)	NOT NULL,
                         `pin_hash`	VARCHAR(100)	NULL,
                         `name`	VARCHAR(50)	NOT NULL,
                         `phone_number`	VARCHAR(20)	NULL,
                         `role`	VARCHAR(20)	NOT NULL	DEFAULT 'USER',
                         `di`	VARCHAR(255)	NULL,
                         `created_at`	DATETIME	NOT NULL,
                         `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE
);

CREATE TABLE `cards` (
                         `card_id`	BIGINT	NOT NULL,
                         `card_name`	VARCHAR(50)	NOT NULL,
                         `card_type`	VARCHAR(20)	NOT NULL	COMMENT 'CREDIT
CHECK',
                         `card_image_url`	VARCHAR(255)	NULL,
                         `description`	TEXT	NULL,
                         `is_supported`	BOOLEAN	NOT NULL	DEFAULT TRUE,
                         `card_company_name`	VARCHAR(30)	NOT NULL,
                         `min_benefit_amount`	DECIMAL(8,0)	NULL
);

CREATE TABLE `bookmarked_stores` (
                                     `user_id`	BIGINT	NOT NULL,
                                     `merchant_id`	BIGINT	NOT NULL,
                                     `created_at`	DATETIME	NOT NULL,
                                     `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE
);

ALTER TABLE `card_benefits` ADD CONSTRAINT `PK_CARD_BENEFITS` PRIMARY KEY (
                                                                           `benefit_id`
    );

ALTER TABLE `merchants` ADD CONSTRAINT `PK_MERCHANTS` PRIMARY KEY (
                                                                   `merchant_id`
    );

ALTER TABLE `payments` ADD CONSTRAINT `PK_PAYMENTS` PRIMARY KEY (
                                                                 `payment_id`
    );

ALTER TABLE `merchant_categories` ADD CONSTRAINT `PK_MERCHANT_CATEGORIES` PRIMARY KEY (
                                                                                       `category_id`
    );

ALTER TABLE `user_cards` ADD CONSTRAINT `PK_USER_CARDS` PRIMARY KEY (
                                                                     `user_card_id`
    );

ALTER TABLE `card_monthly_status` ADD CONSTRAINT `PK_CARD_MONTHLY_STATUS` PRIMARY KEY (
                                                                                       `card_monthly_status_id`
    );

ALTER TABLE `benefit_per_user_card` ADD CONSTRAINT `PK_BENEFIT_PER_USER_CARD` PRIMARY KEY (
                                                                                           `user_card_id`,
                                                                                           `benefit_id`
    );

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
                                                           `user_id`
    );

ALTER TABLE `cards` ADD CONSTRAINT `PK_CARDS` PRIMARY KEY (
                                                           `card_id`
    );

ALTER TABLE `bookmarked_stores` ADD CONSTRAINT `PK_BOOKMARKED_STORES` PRIMARY KEY (
                                                                                   `user_id`,
                                                                                   `merchant_id`
    );

ALTER TABLE `card_benefits` ADD CONSTRAINT `FK_cards_TO_card_benefits_1` FOREIGN KEY (
                                                                                      `card_id`
    )
    REFERENCES `cards` (
                        `card_id`
        );

ALTER TABLE `merchants` ADD CONSTRAINT `FK_merchant_categories_TO_merchants_1` FOREIGN KEY (
                                                                                            `category_id`
    )
    REFERENCES `merchant_categories` (
                                      `category_id`
        );

ALTER TABLE `payments` ADD CONSTRAINT `FK_user_cards_TO_payments_1` FOREIGN KEY (
                                                                                 `user_card_id`
    )
    REFERENCES `user_cards` (
                             `user_card_id`
        );

ALTER TABLE `payments` ADD CONSTRAINT `FK_merchants_TO_payments_1` FOREIGN KEY (
                                                                                `merchant_id`
    )
    REFERENCES `merchants` (
                            `merchant_id`
        );

ALTER TABLE `user_cards` ADD CONSTRAINT `FK_users_TO_user_cards_1` FOREIGN KEY (
                                                                                `user_id`
    )
    REFERENCES `users` (
                        `user_id`
        );

ALTER TABLE `user_cards` ADD CONSTRAINT `FK_cards_TO_user_cards_1` FOREIGN KEY (
                                                                                `card_id`
    )
    REFERENCES `cards` (
                        `card_id`
        );

ALTER TABLE `card_monthly_status` ADD CONSTRAINT `FK_user_cards_TO_card_monthly_status_1` FOREIGN KEY (
                                                                                                       `user_card_id`
    )
    REFERENCES `user_cards` (
                             `user_card_id`
        );

ALTER TABLE `benefit_per_user_card` ADD CONSTRAINT `FK_user_cards_TO_benefit_per_user_card_1` FOREIGN KEY (
                                                                                                           `user_card_id`
    )
    REFERENCES `user_cards` (
                             `user_card_id`
        );

ALTER TABLE `benefit_per_user_card` ADD CONSTRAINT `FK_card_benefits_TO_benefit_per_user_card_1` FOREIGN KEY (
                                                                                                              `benefit_id`
    )
    REFERENCES `card_benefits` (
                                `benefit_id`
        );

ALTER TABLE `bookmarked_stores` ADD CONSTRAINT `FK_users_TO_bookmarked_stores_1` FOREIGN KEY (
                                                                                              `user_id`
    )
    REFERENCES `users` (
                        `user_id`
        );

ALTER TABLE `bookmarked_stores` ADD CONSTRAINT `FK_merchants_TO_bookmarked_stores_1` FOREIGN KEY (
                                                                                                  `merchant_id`
    )
    REFERENCES `merchants` (
                            `merchant_id`
        );

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `users` MODIFY `user_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `user_cards` MODIFY `user_card_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `card_benefits` MODIFY `benefit_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `cards` MODIFY `card_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `merchants` MODIFY `merchant_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `payments` MODIFY `payment_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `card_monthly_status` MODIFY `card_monthly_status_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `merchant_categories` MODIFY `category_id` BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `users` ADD CONSTRAINT `UQ_users_login_id` UNIQUE (`login_id`);
ALTER TABLE `users` ADD CONSTRAINT `UQ_users_email` UNIQUE (`email`);
ALTER TABLE `users` ADD CONSTRAINT `UQ_users_phone_number` UNIQUE (`phone_number`);
ALTER TABLE `users` ADD CONSTRAINT `UQ_users_di` UNIQUE (`di`);
ALTER TABLE `user_cards` ADD CONSTRAINT `UQ_user_cards_card_token` UNIQUE (`card_token`);
ALTER TABLE `merchants` ADD CONSTRAINT `UQ_merchants_merchant_code` UNIQUE (`merchant_code`);
ALTER TABLE `merchant_categories` ADD CONSTRAINT `UQ_merchant_categories_category_code` UNIQUE (`category_code`);

-- ===== Mock Data =====

INSERT INTO `merchant_categories` (`category_id`, `category_code`, `category_name`) VALUES
(1, 'FOOD', '카페/음식점'),
(2, 'MART', '마트/편의점'),
(3, 'CULTURE', '문화/여가'),
(4, 'TRANSPORT', '교통'),
(5, 'SHOPPING', '쇼핑');

INSERT INTO `cards` (`card_id`, `card_name`, `card_type`, `card_image_url`, `description`, `is_supported`, `card_company_name`, `min_benefit_amount`) VALUES
(1, 'Deep Dream', 'CREDIT', NULL, '온라인 쇼핑 및 카페 특화 혜택 카드', TRUE, '신한카드', 300000),
(2, 'taptap O', 'CREDIT', NULL, '교통 및 마트 할인 특화 카드', TRUE, '삼성카드', 300000),
(3, 'Zero Edition2', 'CREDIT', NULL, '적립형 무이자 할부 특화 카드', TRUE, '현대카드', 0),
(4, '톡톡체크', 'CHECK', NULL, '생활 밀착형 체크카드', TRUE, 'KB국민카드', 0),
(5, '카드의정석', 'CREDIT', NULL, '문화/여가 특화 혜택 카드', TRUE, '우리카드', 400000);

INSERT INTO `card_benefits` (`benefit_id`, `card_id`, `benefit_type`, `benefit_name`, `description`, `benefits_info`) VALUES
(1, 1, '청구', '카페 할인', '스타벅스 등 주요 카페 10% 청구할인', '{"category":"CAFE","discount_rate":10,"monthly_limit":15000}'),
(2, 1, '환급', '온라인쇼핑 캐시백', '온라인 쇼핑몰 결제액 5% 환급', '{"category":"SHOPPING","cashback_rate":5,"monthly_limit":20000}'),
(3, 2, '현장', '대중교통 할인', '버스/지하철 결제 시 10% 현장 할인', '{"category":"TRANSPORT","discount_rate":10,"monthly_limit":10000}'),
(4, 3, '환급', '전 가맹점 적립', '모든 가맹점 결제액 0.7% 포인트 적립', '{"category":"ALL","point_rate":0.7}'),
(5, 3, '청구', '무이자 할부', '2~3개월 무이자 할부 지원', '{"installment_months":[2,3]}'),
(6, 4, '현장', '편의점 할인', '편의점 결제 시 5% 즉시 할인', '{"category":"MART","discount_rate":5,"monthly_limit":5000}'),
(7, 5, '청구', '영화 할인', '영화 예매 시 최대 6000원 청구 할인', '{"category":"CULTURE","discount_amount":6000,"monthly_count":2}'),
(8, 5, '환급', '공연 캐시백', '공연/전시 결제액 8% 환급', '{"category":"CULTURE","cashback_rate":8,"monthly_limit":30000}');

INSERT INTO `merchants` (`merchant_id`, `category_id`, `merchant_code`, `merchant_name`, `brand_name`, `address`, `latitude`, `longitude`, `phone`) VALUES
(1, 1, 'MC-0001', '스타벅스 강남점', '스타벅스', '서울특별시 강남구 테헤란로 123', 37.4980000, 127.0276000, '02-1234-5601'),
(2, 2, 'MC-0002', 'GS25 역삼점', 'GS25', '서울특별시 강남구 역삼로 45', 37.5006000, 127.0364000, '02-1234-5602'),
(3, 3, 'MC-0003', 'CGV 강남', 'CGV', '서울특별시 강남구 강남대로 438', 37.4996000, 127.0281000, '02-1234-5603'),
(4, 4, 'MC-0004', '강남역 교통카드충전소', NULL, '서울특별시 강남구 강남대로 396', 37.4979000, 127.0276000, NULL),
(5, 5, 'MC-0005', '롯데백화점 잠실점', '롯데백화점', '서울특별시 송파구 올림픽로 240', 37.5113000, 127.0980000, '02-1234-5605'),
(6, 1, 'MC-0006', '투썸플레이스 홍대점', '투썸플레이스', '서울특별시 마포구 양화로 152', 37.5563000, 126.9236000, '02-1234-5606'),
(7, 2, 'MC-0007', '이마트 홍대점', '이마트', '서울특별시 마포구 양화로 176', 37.5570000, 126.9245000, '02-1234-5607'),
(8, 3, 'MC-0008', '롯데시네마 홍대입구', '롯데시네마', '서울특별시 마포구 양화로 188', 37.5575000, 126.9250000, '02-1234-5608'),
(9, 4, 'MC-0009', '홍대입구역 버스환승센터', NULL, '서울특별시 마포구 양화로 200', 37.5578000, 126.9255000, NULL),
(10, 5, 'MC-0010', '현대백화점 신촌점', '현대백화점', '서울특별시 서대문구 신촌로 83', 37.5585000, 126.9368000, '02-1234-5610');

INSERT INTO `users` (`user_id`, `email`, `login_id`, `password_hash`, `pin_hash`, `name`, `phone_number`, `role`, `di`, `created_at`, `is_deleted`) VALUES
(1, 'user01@example.com', 'user01', '$2a$10$mockPasswordHash0000000000000000000001', '$2a$10$mockPinHash00001', '김민준', '010-1000-0001', 'USER', 'DI0000000000000001', '2026-01-04 10:00:00', FALSE),
(2, 'user02@example.com', 'user02', '$2a$10$mockPasswordHash0000000000000000000002', '$2a$10$mockPinHash00002', '이서연', '010-1000-0002', 'USER', 'DI0000000000000002', '2026-02-07 10:00:00', FALSE),
(3, 'user03@example.com', 'user03', '$2a$10$mockPasswordHash0000000000000000000003', NULL, '박도윤', '010-1000-0003', 'USER', 'DI0000000000000003', '2026-03-10 10:00:00', FALSE),
(4, 'user04@example.com', 'user04', '$2a$10$mockPasswordHash0000000000000000000004', '$2a$10$mockPinHash00004', '최지우', '010-1000-0004', 'USER', 'DI0000000000000004', '2026-04-13 10:00:00', FALSE),
(5, 'user05@example.com', 'user05', '$2a$10$mockPasswordHash0000000000000000000005', '$2a$10$mockPinHash00005', '정하윤', '010-1000-0005', 'USER', 'DI0000000000000005', '2026-05-16 10:00:00', FALSE),
(6, 'user06@example.com', 'user06', '$2a$10$mockPasswordHash0000000000000000000006', NULL, '강은우', '010-1000-0006', 'USER', 'DI0000000000000006', '2026-06-19 10:00:00', FALSE),
(7, 'user07@example.com', 'user07', '$2a$10$mockPasswordHash0000000000000000000007', '$2a$10$mockPinHash00007', '조수아', '010-1000-0007', 'USER', 'DI0000000000000007', '2026-01-22 10:00:00', FALSE),
(8, 'user08@example.com', 'user08', '$2a$10$mockPasswordHash0000000000000000000008', '$2a$10$mockPinHash00008', '윤지호', '010-1000-0008', 'USER', 'DI0000000000000008', '2026-02-25 10:00:00', FALSE),
(9, 'user09@example.com', 'user09', '$2a$10$mockPasswordHash0000000000000000000009', NULL, '임서준', '010-1000-0009', 'USER', 'DI0000000000000009', '2026-03-01 10:00:00', FALSE),
(10, 'user10@example.com', 'user10', '$2a$10$mockPasswordHash0000000000000000000010', '$2a$10$mockPinHash00010', '한예은', '010-1000-0010', 'USER', 'DI0000000000000010', '2026-04-04 10:00:00', FALSE);

INSERT INTO `user_cards` (`user_card_id`, `user_id`, `card_id`, `card_token`, `card_last4`, `is_primary`, `recommendation_enabled`, `registered_at`, `is_deleted`, `annual_fee`) VALUES
(1, 1, 1, 'TOKEN-0001', '1001', TRUE, TRUE, '2026-01-05 11:00:00', FALSE, 15000),
(2, 1, 2, 'TOKEN-0002', '1002', FALSE, TRUE, '2026-01-05 11:00:00', FALSE, 10000),
(3, 1, 3, 'TOKEN-0003', '1003', FALSE, TRUE, '2026-01-05 11:00:00', FALSE, 0),
(4, 2, 2, 'TOKEN-0004', '1004', TRUE, TRUE, '2026-02-08 11:00:00', FALSE, 10000),
(5, 2, 3, 'TOKEN-0005', '1005', FALSE, TRUE, '2026-02-08 11:00:00', FALSE, 0),
(6, 2, 4, 'TOKEN-0006', '1006', FALSE, TRUE, '2026-02-08 11:00:00', FALSE, 0),
(7, 3, 3, 'TOKEN-0007', '1007', TRUE, TRUE, '2026-03-11 11:00:00', FALSE, 0),
(8, 3, 4, 'TOKEN-0008', '1008', FALSE, TRUE, '2026-03-11 11:00:00', FALSE, 0),
(9, 3, 5, 'TOKEN-0009', '1009', FALSE, TRUE, '2026-03-11 11:00:00', FALSE, 20000),
(10, 4, 4, 'TOKEN-0010', '1010', TRUE, TRUE, '2026-04-14 11:00:00', FALSE, 0),
(11, 4, 5, 'TOKEN-0011', '1011', FALSE, TRUE, '2026-04-14 11:00:00', FALSE, 20000),
(12, 4, 1, 'TOKEN-0012', '1012', FALSE, TRUE, '2026-04-14 11:00:00', FALSE, 15000),
(13, 5, 5, 'TOKEN-0013', '1013', TRUE, TRUE, '2026-05-17 11:00:00', FALSE, 20000),
(14, 5, 1, 'TOKEN-0014', '1014', FALSE, TRUE, '2026-05-17 11:00:00', FALSE, 15000),
(15, 5, 2, 'TOKEN-0015', '1015', FALSE, TRUE, '2026-05-17 11:00:00', FALSE, 10000),
(16, 6, 1, 'TOKEN-0016', '1016', TRUE, TRUE, '2026-06-20 11:00:00', FALSE, 15000),
(17, 6, 2, 'TOKEN-0017', '1017', FALSE, TRUE, '2026-06-20 11:00:00', FALSE, 10000),
(18, 6, 3, 'TOKEN-0018', '1018', FALSE, TRUE, '2026-06-20 11:00:00', FALSE, 0),
(19, 7, 2, 'TOKEN-0019', '1019', TRUE, TRUE, '2026-01-23 11:00:00', FALSE, 10000),
(20, 7, 3, 'TOKEN-0020', '1020', FALSE, TRUE, '2026-01-23 11:00:00', FALSE, 0),
(21, 7, 4, 'TOKEN-0021', '1021', FALSE, TRUE, '2026-01-23 11:00:00', FALSE, 0),
(22, 8, 3, 'TOKEN-0022', '1022', TRUE, TRUE, '2026-02-26 11:00:00', FALSE, 0),
(23, 8, 4, 'TOKEN-0023', '1023', FALSE, TRUE, '2026-02-26 11:00:00', FALSE, 0),
(24, 8, 5, 'TOKEN-0024', '1024', FALSE, TRUE, '2026-02-26 11:00:00', FALSE, 20000),
(25, 9, 4, 'TOKEN-0025', '1025', TRUE, TRUE, '2026-03-02 11:00:00', FALSE, 0),
(26, 9, 5, 'TOKEN-0026', '1026', FALSE, TRUE, '2026-03-02 11:00:00', FALSE, 20000),
(27, 9, 1, 'TOKEN-0027', '1027', FALSE, TRUE, '2026-03-02 11:00:00', FALSE, 15000),
(28, 10, 5, 'TOKEN-0028', '1028', TRUE, TRUE, '2026-04-05 11:00:00', FALSE, 20000),
(29, 10, 1, 'TOKEN-0029', '1029', FALSE, TRUE, '2026-04-05 11:00:00', FALSE, 15000),
(30, 10, 2, 'TOKEN-0030', '1030', FALSE, TRUE, '2026-04-05 11:00:00', FALSE, 10000);

INSERT INTO `card_monthly_status` (`card_monthly_status_id`, `user_card_id`, `total_spending_amount`, `is_benefit_eligible`, `updated_at`, `target_year_month`) VALUES
(1, 1, 80037, FALSE, '2026-05-31 23:59:59', '202605'),
(2, 1, 92382, FALSE, '2026-06-30 23:59:59', '202606'),
(3, 1, 104727, FALSE, '2026-07-22 23:59:59', '202607'),
(4, 2, 80074, FALSE, '2026-05-31 23:59:59', '202605'),
(5, 2, 92419, FALSE, '2026-06-30 23:59:59', '202606'),
(6, 2, 104764, FALSE, '2026-07-22 23:59:59', '202607'),
(7, 3, 80111, TRUE, '2026-05-31 23:59:59', '202605'),
(8, 3, 92456, TRUE, '2026-06-30 23:59:59', '202606'),
(9, 3, 104801, TRUE, '2026-07-22 23:59:59', '202607'),
(10, 4, 80148, FALSE, '2026-05-31 23:59:59', '202605'),
(11, 4, 92493, FALSE, '2026-06-30 23:59:59', '202606'),
(12, 4, 104838, FALSE, '2026-07-22 23:59:59', '202607'),
(13, 5, 80185, TRUE, '2026-05-31 23:59:59', '202605'),
(14, 5, 92530, TRUE, '2026-06-30 23:59:59', '202606'),
(15, 5, 104875, TRUE, '2026-07-22 23:59:59', '202607'),
(16, 6, 80222, TRUE, '2026-05-31 23:59:59', '202605'),
(17, 6, 92567, TRUE, '2026-06-30 23:59:59', '202606'),
(18, 6, 104912, TRUE, '2026-07-22 23:59:59', '202607'),
(19, 7, 80259, TRUE, '2026-05-31 23:59:59', '202605'),
(20, 7, 92604, TRUE, '2026-06-30 23:59:59', '202606'),
(21, 7, 104949, TRUE, '2026-07-22 23:59:59', '202607'),
(22, 8, 80296, TRUE, '2026-05-31 23:59:59', '202605'),
(23, 8, 92641, TRUE, '2026-06-30 23:59:59', '202606'),
(24, 8, 104986, TRUE, '2026-07-22 23:59:59', '202607'),
(25, 9, 80333, FALSE, '2026-05-31 23:59:59', '202605'),
(26, 9, 92678, FALSE, '2026-06-30 23:59:59', '202606'),
(27, 9, 105023, FALSE, '2026-07-22 23:59:59', '202607'),
(28, 10, 80370, TRUE, '2026-05-31 23:59:59', '202605'),
(29, 10, 92715, TRUE, '2026-06-30 23:59:59', '202606'),
(30, 10, 105060, TRUE, '2026-07-22 23:59:59', '202607'),
(31, 11, 80407, FALSE, '2026-05-31 23:59:59', '202605'),
(32, 11, 92752, FALSE, '2026-06-30 23:59:59', '202606'),
(33, 11, 105097, FALSE, '2026-07-22 23:59:59', '202607'),
(34, 12, 80444, FALSE, '2026-05-31 23:59:59', '202605'),
(35, 12, 92789, FALSE, '2026-06-30 23:59:59', '202606'),
(36, 12, 105134, FALSE, '2026-07-22 23:59:59', '202607'),
(37, 13, 80481, FALSE, '2026-05-31 23:59:59', '202605'),
(38, 13, 92826, FALSE, '2026-06-30 23:59:59', '202606'),
(39, 13, 105171, FALSE, '2026-07-22 23:59:59', '202607'),
(40, 14, 80518, FALSE, '2026-05-31 23:59:59', '202605'),
(41, 14, 92863, FALSE, '2026-06-30 23:59:59', '202606'),
(42, 14, 105208, FALSE, '2026-07-22 23:59:59', '202607'),
(43, 15, 80555, FALSE, '2026-05-31 23:59:59', '202605'),
(44, 15, 92900, FALSE, '2026-06-30 23:59:59', '202606'),
(45, 15, 105245, FALSE, '2026-07-22 23:59:59', '202607'),
(46, 16, 80592, FALSE, '2026-05-31 23:59:59', '202605'),
(47, 16, 92937, FALSE, '2026-06-30 23:59:59', '202606'),
(48, 16, 105282, FALSE, '2026-07-22 23:59:59', '202607'),
(49, 17, 80629, FALSE, '2026-05-31 23:59:59', '202605'),
(50, 17, 92974, FALSE, '2026-06-30 23:59:59', '202606'),
(51, 17, 105319, FALSE, '2026-07-22 23:59:59', '202607'),
(52, 18, 80666, TRUE, '2026-05-31 23:59:59', '202605'),
(53, 18, 93011, TRUE, '2026-06-30 23:59:59', '202606'),
(54, 18, 105356, TRUE, '2026-07-22 23:59:59', '202607'),
(55, 19, 80703, FALSE, '2026-05-31 23:59:59', '202605'),
(56, 19, 93048, FALSE, '2026-06-30 23:59:59', '202606'),
(57, 19, 105393, FALSE, '2026-07-22 23:59:59', '202607'),
(58, 20, 80740, TRUE, '2026-05-31 23:59:59', '202605'),
(59, 20, 93085, TRUE, '2026-06-30 23:59:59', '202606'),
(60, 20, 105430, TRUE, '2026-07-22 23:59:59', '202607'),
(61, 21, 80777, TRUE, '2026-05-31 23:59:59', '202605'),
(62, 21, 93122, TRUE, '2026-06-30 23:59:59', '202606'),
(63, 21, 105467, TRUE, '2026-07-22 23:59:59', '202607'),
(64, 22, 80814, TRUE, '2026-05-31 23:59:59', '202605'),
(65, 22, 93159, TRUE, '2026-06-30 23:59:59', '202606'),
(66, 22, 105504, TRUE, '2026-07-22 23:59:59', '202607'),
(67, 23, 80851, TRUE, '2026-05-31 23:59:59', '202605'),
(68, 23, 93196, TRUE, '2026-06-30 23:59:59', '202606'),
(69, 23, 105541, TRUE, '2026-07-22 23:59:59', '202607'),
(70, 24, 80888, FALSE, '2026-05-31 23:59:59', '202605'),
(71, 24, 93233, FALSE, '2026-06-30 23:59:59', '202606'),
(72, 24, 105578, FALSE, '2026-07-22 23:59:59', '202607'),
(73, 25, 80925, TRUE, '2026-05-31 23:59:59', '202605'),
(74, 25, 93270, TRUE, '2026-06-30 23:59:59', '202606'),
(75, 25, 105615, TRUE, '2026-07-22 23:59:59', '202607'),
(76, 26, 80962, FALSE, '2026-05-31 23:59:59', '202605'),
(77, 26, 93307, FALSE, '2026-06-30 23:59:59', '202606'),
(78, 26, 105652, FALSE, '2026-07-22 23:59:59', '202607'),
(79, 27, 80999, FALSE, '2026-05-31 23:59:59', '202605'),
(80, 27, 93344, FALSE, '2026-06-30 23:59:59', '202606'),
(81, 27, 105689, FALSE, '2026-07-22 23:59:59', '202607'),
(82, 28, 81036, FALSE, '2026-05-31 23:59:59', '202605'),
(83, 28, 93381, FALSE, '2026-06-30 23:59:59', '202606'),
(84, 28, 105726, FALSE, '2026-07-22 23:59:59', '202607'),
(85, 29, 81073, FALSE, '2026-05-31 23:59:59', '202605'),
(86, 29, 93418, FALSE, '2026-06-30 23:59:59', '202606'),
(87, 29, 105763, FALSE, '2026-07-22 23:59:59', '202607'),
(88, 30, 81110, FALSE, '2026-05-31 23:59:59', '202605'),
(89, 30, 93455, FALSE, '2026-06-30 23:59:59', '202606'),
(90, 30, 105800, FALSE, '2026-07-22 23:59:59', '202607');

INSERT INTO `benefit_per_user_card` (`user_card_id`, `benefit_id`) VALUES
(1, 1),
(1, 2),
(2, 3),
(3, 4),
(3, 5),
(4, 3),
(5, 4),
(5, 5),
(6, 6),
(7, 4),
(7, 5),
(8, 6),
(9, 7),
(9, 8),
(10, 6),
(11, 7),
(11, 8),
(12, 1),
(12, 2),
(13, 7),
(13, 8),
(14, 1),
(14, 2),
(15, 3),
(16, 1),
(16, 2),
(17, 3),
(18, 4),
(18, 5),
(19, 3),
(20, 4),
(20, 5),
(21, 6),
(22, 4),
(22, 5),
(23, 6),
(24, 7),
(24, 8),
(25, 6),
(26, 7),
(26, 8),
(27, 1),
(27, 2),
(28, 7),
(28, 8),
(29, 1),
(29, 2),
(30, 3);

INSERT INTO `payments` (`payment_id`, `user_card_id`, `merchant_id`, `payment_time`, `original_amount`, `discount_amount`, `final_amount`, `payment_status`) VALUES
(1, 1, 2, '2026-01-13 09:30:00', 10913, 1100, 9813, 'APPROVED'),
(2, 1, 3, '2026-01-20 14:30:00', 11284, 0, 11284, 'APPROVED'),
(3, 2, 3, '2026-01-16 09:30:00', 11826, 1200, 10626, 'APPROVED'),
(4, 2, 4, '2026-01-23 14:30:00', 12197, 0, 12197, 'APPROVED'),
(5, 3, 4, '2026-01-19 09:30:00', 12739, 1300, 11439, 'APPROVED'),
(6, 3, 5, '2026-01-11 14:30:00', 13110, 0, 13110, 'APPROVED'),
(7, 4, 5, '2026-02-22 09:30:00', 13652, 1400, 12252, 'APPROVED'),
(8, 4, 6, '2026-02-14 14:30:00', 14023, 0, 14023, 'APPROVED'),
(9, 5, 6, '2026-02-10 09:30:00', 14565, 1500, 13065, 'APPROVED'),
(10, 5, 7, '2026-02-17 14:30:00', 14936, 0, 14936, 'APPROVED'),
(11, 6, 7, '2026-02-13 09:30:00', 15478, 1500, 13978, 'APPROVED'),
(12, 6, 8, '2026-02-20 14:30:00', 15849, 0, 15849, 'APPROVED'),
(13, 7, 8, '2026-03-16 09:30:00', 16391, 1600, 14791, 'APPROVED'),
(14, 7, 9, '2026-03-23 14:30:00', 16762, 0, 16762, 'APPROVED'),
(15, 8, 9, '2026-03-19 09:30:00', 17304, 1700, 15604, 'APPROVED'),
(16, 8, 10, '2026-03-11 14:30:00', 17675, 0, 17675, 'APPROVED'),
(17, 9, 10, '2026-03-22 09:30:00', 18217, 1800, 16417, 'APPROVED'),
(18, 9, 1, '2026-03-14 14:30:00', 18588, 0, 18588, 'APPROVED'),
(19, 10, 1, '2026-04-10 09:30:00', 19130, 1900, 17230, 'APPROVED'),
(20, 10, 2, '2026-04-17 14:30:00', 19501, 0, 19501, 'CANCELED'),
(21, 11, 2, '2026-04-13 09:30:00', 20043, 2000, 18043, 'CANCELED'),
(22, 11, 3, '2026-04-20 14:30:00', 20414, 0, 20414, 'APPROVED'),
(23, 12, 3, '2026-04-16 09:30:00', 20956, 2100, 18856, 'APPROVED'),
(24, 12, 4, '2026-04-23 14:30:00', 21327, 0, 21327, 'APPROVED'),
(25, 13, 4, '2026-05-19 09:30:00', 21869, 2200, 19669, 'APPROVED'),
(26, 13, 5, '2026-05-11 14:30:00', 22240, 0, 22240, 'APPROVED'),
(27, 14, 5, '2026-05-22 09:30:00', 22782, 2300, 20482, 'APPROVED'),
(28, 14, 6, '2026-05-14 14:30:00', 23153, 0, 23153, 'APPROVED'),
(29, 15, 6, '2026-05-10 09:30:00', 23695, 2400, 21295, 'APPROVED'),
(30, 15, 7, '2026-05-17 14:30:00', 24066, 0, 24066, 'APPROVED'),
(31, 16, 7, '2026-06-13 09:30:00', 24608, 2500, 22108, 'APPROVED'),
(32, 16, 8, '2026-06-20 14:30:00', 24979, 0, 24979, 'APPROVED'),
(33, 17, 8, '2026-06-16 09:30:00', 25521, 2600, 22921, 'APPROVED'),
(34, 17, 9, '2026-06-23 14:30:00', 25892, 0, 25892, 'APPROVED'),
(35, 18, 9, '2026-06-19 09:30:00', 26434, 2600, 23834, 'APPROVED'),
(36, 18, 10, '2026-06-11 14:30:00', 26805, 0, 26805, 'APPROVED'),
(37, 19, 10, '2026-01-22 09:30:00', 27347, 2700, 24647, 'APPROVED'),
(38, 19, 1, '2026-01-14 14:30:00', 27718, 0, 27718, 'APPROVED'),
(39, 20, 1, '2026-01-10 09:30:00', 28260, 2800, 25460, 'APPROVED'),
(40, 20, 2, '2026-01-17 14:30:00', 28631, 0, 28631, 'APPROVED'),
(41, 21, 2, '2026-01-13 09:30:00', 29173, 2900, 26273, 'APPROVED'),
(42, 21, 3, '2026-01-20 14:30:00', 29544, 0, 29544, 'CANCELED'),
(43, 22, 3, '2026-02-16 09:30:00', 30086, 3000, 27086, 'CANCELED'),
(44, 22, 4, '2026-02-23 14:30:00', 30457, 0, 30457, 'APPROVED'),
(45, 23, 4, '2026-02-19 09:30:00', 30999, 3100, 27899, 'APPROVED'),
(46, 23, 5, '2026-02-11 14:30:00', 31370, 0, 31370, 'APPROVED'),
(47, 24, 5, '2026-02-22 09:30:00', 31912, 3200, 28712, 'APPROVED'),
(48, 24, 6, '2026-02-14 14:30:00', 32283, 0, 32283, 'APPROVED'),
(49, 25, 6, '2026-03-10 09:30:00', 32825, 3300, 29525, 'APPROVED'),
(50, 25, 7, '2026-03-17 14:30:00', 33196, 0, 33196, 'APPROVED'),
(51, 26, 7, '2026-03-13 09:30:00', 33738, 3400, 30338, 'APPROVED'),
(52, 26, 8, '2026-03-20 14:30:00', 34109, 0, 34109, 'APPROVED'),
(53, 27, 8, '2026-03-16 09:30:00', 34651, 3500, 31151, 'APPROVED'),
(54, 27, 9, '2026-03-23 14:30:00', 35022, 0, 35022, 'APPROVED'),
(55, 28, 9, '2026-04-19 09:30:00', 35564, 3600, 31964, 'APPROVED'),
(56, 28, 10, '2026-04-11 14:30:00', 35935, 0, 35935, 'APPROVED'),
(57, 29, 10, '2026-04-22 09:30:00', 36477, 3600, 32877, 'APPROVED'),
(58, 29, 1, '2026-04-14 14:30:00', 36848, 0, 36848, 'APPROVED'),
(59, 30, 1, '2026-04-10 09:30:00', 37390, 3700, 33690, 'APPROVED'),
(60, 30, 2, '2026-04-17 14:30:00', 37761, 0, 37761, 'APPROVED');

