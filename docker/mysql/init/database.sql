DROP DATABASE IF EXISTS benepay;
CREATE DATABASE benepay;
USE benepay;

-- ===== Tables (structure: "Copy of IT'S ME.sql" / constraints: existing database.sql) =====

CREATE TABLE `merchant_categories` (
    `category_id`        BIGINT        NOT NULL,
    `category_code`      VARCHAR(30)   NOT NULL,
    `category_name`      VARCHAR(50)   NOT NULL
);

CREATE TABLE `cards` (
    `card_id`             BIGINT         NOT NULL,
    `card_name`           VARCHAR(50)    NOT NULL,
    `card_type`           VARCHAR(20)    NOT NULL    COMMENT 'CREDIT / CHECK',
    `card_image_url`      VARCHAR(255)   NULL,
    `description`         TEXT           NULL,
    `is_supported`        BOOLEAN        NOT NULL    DEFAULT TRUE,
    `min_benefit_amount`  DECIMAL(8,0)   NULL
);

CREATE TABLE `card_variants` (
    `card_variant_id`  BIGINT        NOT NULL,
    `card_id`          BIGINT        NOT NULL,
    `card_network`     VARCHAR(20)   NOT NULL    COMMENT '국내전용, visa',
    `annual_fee`       DECIMAL(8,0)  NOT NULL    DEFAULT 0
);

CREATE TABLE `users` (
    `user_id`        BIGINT        NOT NULL,
    `login_id`       VARCHAR(30)   NULL,
    `password_hash`  VARCHAR(100)  NOT NULL,
    `pin_hash`       VARCHAR(100)  NULL,
    `name`           VARCHAR(50)   NOT NULL,
    `phone_number`   VARCHAR(20)   NULL,
    `role`           VARCHAR(20)   NOT NULL    DEFAULT 'USER',
    `di`             VARCHAR(255)  NULL        COMMENT '포트원 DI, SHA-256+salt 해시',
    `ci_hash`        VARCHAR(255)  NULL        COMMENT '포트원 CI, bcrypt 해시',
    `created_at`     DATETIME      NOT NULL,
    `is_deleted`     BOOLEAN       NOT NULL    DEFAULT FALSE
);

CREATE TABLE `card_benefits` (
    `benefit_id`      BIGINT        NOT NULL,
    `card_id`         BIGINT        NOT NULL,
    `benefit_type`    VARCHAR(20)   NOT NULL    COMMENT '환급, 청구, 현장',
    `benefit_name`    VARCHAR(100)  NULL,
    `description`     TEXT          NULL,
    `benefits_info`   JSON          NOT NULL
);

CREATE TABLE `benefit_per_user_card` (
    `user_card_id`  BIGINT  NOT NULL,
    `benefit_id`    BIGINT  NOT NULL
);

CREATE TABLE `user_cards` (
    `user_card_id`             BIGINT        NOT NULL,
    `user_id`                  BIGINT        NOT NULL,
    `card_variant_id`          BIGINT        NOT NULL,
    `card_token`               VARCHAR(255)  NOT NULL,
    `card_last4`               CHAR(4)       NOT NULL,
    `is_primary`               BOOLEAN       NOT NULL    DEFAULT FALSE,
    `recommendation_enabled`   BOOLEAN       NOT NULL    DEFAULT TRUE,
    `registered_at`            DATETIME      NOT NULL,
    `card_status`              VARCHAR(20)   NOT NULL    DEFAULT 'ACTIVE'    COMMENT 'ACTIVE / SUSPENDED / UNLINKED'
);

CREATE TABLE `payments` (
    `payment_id`       BIGINT         NOT NULL,
    `user_card_id`     BIGINT         NOT NULL,
    `merchant_id`      BIGINT         NOT NULL,
    `payment_time`     DATETIME       NOT NULL,
    `original_amount`  DECIMAL(10,0)  NOT NULL,
    `discount_amount`  DECIMAL(10,0)  NOT NULL    DEFAULT 0,
    `final_amount`     DECIMAL(10,0)  NOT NULL,
    `payment_status`   VARCHAR(20)    NOT NULL    DEFAULT 'PENDING'    COMMENT 'APPROVED / CANCELED',
    `payment_method`   VARCHAR(20)    NOT NULL    COMMENT 'BARCODE / QR'
);

CREATE TABLE `card_monthly_status` (
    `card_monthly_status_id`  BIGINT         NOT NULL,
    `user_card_id`            BIGINT         NOT NULL,
    `target_year_month`       CHAR(6)        NOT NULL,
    `total_spending_amount`   DECIMAL(12,0)  NOT NULL,
    `updated_at`              DATETIME       NOT NULL    COMMENT '해당 월별 실적 데이터가 마지막으로 변경된 시간'
);

CREATE TABLE `merchants` (
    `merchant_id`    BIGINT         NOT NULL,
    `category_id`    BIGINT         NOT NULL,
    `merchant_code`  VARCHAR(50)    NOT NULL,
    `merchant_name`  VARCHAR(100)   NOT NULL,
    `brand_name`     VARCHAR(255)   NULL,
    `address`        VARCHAR(255)   NOT NULL,
    `latitude`       DECIMAL(10,7)  NOT NULL,
    `longitude`      DECIMAL(10,7)  NOT NULL,
    `phone`          VARCHAR(20)    NULL
);

CREATE TABLE `bookmarked_stores` (
    `user_id`      BIGINT    NOT NULL,
    `merchant_id`  BIGINT    NOT NULL,
    `created_at`   DATETIME  NOT NULL,
    `is_deleted`   BOOLEAN   NOT NULL    DEFAULT FALSE
);

-- ===== Primary Keys =====

ALTER TABLE `merchant_categories` ADD CONSTRAINT `PK_MERCHANT_CATEGORIES` PRIMARY KEY (`category_id`);
ALTER TABLE `cards` ADD CONSTRAINT `PK_CARDS` PRIMARY KEY (`card_id`);
ALTER TABLE `card_variants` ADD CONSTRAINT `PK_CARD_VARIANTS` PRIMARY KEY (`card_variant_id`);
ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (`user_id`);
ALTER TABLE `card_benefits` ADD CONSTRAINT `PK_CARD_BENEFITS` PRIMARY KEY (`benefit_id`);
ALTER TABLE `benefit_per_user_card` ADD CONSTRAINT `PK_BENEFIT_PER_USER_CARD` PRIMARY KEY (`user_card_id`, `benefit_id`);
ALTER TABLE `user_cards` ADD CONSTRAINT `PK_USER_CARDS` PRIMARY KEY (`user_card_id`);
ALTER TABLE `payments` ADD CONSTRAINT `PK_PAYMENTS` PRIMARY KEY (`payment_id`);
ALTER TABLE `card_monthly_status` ADD CONSTRAINT `PK_CARD_MONTHLY_STATUS` PRIMARY KEY (`card_monthly_status_id`);
ALTER TABLE `merchants` ADD CONSTRAINT `PK_MERCHANTS` PRIMARY KEY (`merchant_id`);
ALTER TABLE `bookmarked_stores` ADD CONSTRAINT `PK_BOOKMARKED_STORES` PRIMARY KEY (`user_id`, `merchant_id`);

-- ===== Foreign Keys =====

ALTER TABLE `card_variants` ADD CONSTRAINT `FK_cards_TO_card_variants_1` FOREIGN KEY (`card_id`) REFERENCES `cards` (`card_id`);
ALTER TABLE `card_benefits` ADD CONSTRAINT `FK_cards_TO_card_benefits_1` FOREIGN KEY (`card_id`) REFERENCES `cards` (`card_id`);
ALTER TABLE `merchants` ADD CONSTRAINT `FK_merchant_categories_TO_merchants_1` FOREIGN KEY (`category_id`) REFERENCES `merchant_categories` (`category_id`);
ALTER TABLE `user_cards` ADD CONSTRAINT `FK_users_TO_user_cards_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);
ALTER TABLE `user_cards` ADD CONSTRAINT `FK_card_variants_TO_user_cards_1` FOREIGN KEY (`card_variant_id`) REFERENCES `card_variants` (`card_variant_id`);
ALTER TABLE `card_monthly_status` ADD CONSTRAINT `FK_user_cards_TO_card_monthly_status_1` FOREIGN KEY (`user_card_id`) REFERENCES `user_cards` (`user_card_id`);
ALTER TABLE `benefit_per_user_card` ADD CONSTRAINT `FK_user_cards_TO_benefit_per_user_card_1` FOREIGN KEY (`user_card_id`) REFERENCES `user_cards` (`user_card_id`);
ALTER TABLE `benefit_per_user_card` ADD CONSTRAINT `FK_card_benefits_TO_benefit_per_user_card_1` FOREIGN KEY (`benefit_id`) REFERENCES `card_benefits` (`benefit_id`);
ALTER TABLE `payments` ADD CONSTRAINT `FK_user_cards_TO_payments_1` FOREIGN KEY (`user_card_id`) REFERENCES `user_cards` (`user_card_id`);
ALTER TABLE `payments` ADD CONSTRAINT `FK_merchants_TO_payments_1` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`merchant_id`);
ALTER TABLE `bookmarked_stores` ADD CONSTRAINT `FK_users_TO_bookmarked_stores_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);
ALTER TABLE `bookmarked_stores` ADD CONSTRAINT `FK_merchants_TO_bookmarked_stores_1` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`merchant_id`);

-- ===== Auto Increment =====

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `merchant_categories` MODIFY `category_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `cards` MODIFY `card_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `card_variants` MODIFY `card_variant_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `users` MODIFY `user_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `card_benefits` MODIFY `benefit_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `user_cards` MODIFY `user_card_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `payments` MODIFY `payment_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `card_monthly_status` MODIFY `card_monthly_status_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `merchants` MODIFY `merchant_id` BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== Unique Constraints =====

ALTER TABLE `users` ADD CONSTRAINT `UQ_users_login_id` UNIQUE (`login_id`);
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

INSERT INTO `cards` (`card_id`, `card_name`, `card_type`, `card_image_url`, `description`, `is_supported`, `min_benefit_amount`) VALUES
(1, 'Deep Dream', 'CREDIT', NULL, '온라인 쇼핑 및 카페 특화 혜택 카드', TRUE, 300000),
(2, 'taptap O', 'CREDIT', NULL, '교통 및 마트 할인 특화 카드', TRUE, 300000),
(3, 'Zero Edition2', 'CREDIT', NULL, '적립형 무이자 할부 특화 카드', TRUE, 0),
(4, '톡톡체크', 'CHECK', NULL, '생활 밀착형 체크카드', TRUE, 0),
(5, '카드의정석', 'CREDIT', NULL, '문화/여가 특화 혜택 카드', TRUE, 400000);

INSERT INTO `card_variants` (`card_variant_id`, `card_id`, `card_network`, `annual_fee`) VALUES
(1, 1, '국내전용', 15000),
(2, 1, 'VISA', 18000),
(3, 2, '국내전용', 10000),
(4, 2, 'VISA', 12000),
(5, 3, '국내전용', 0),
(6, 4, '국내전용', 0),
(7, 5, '국내전용', 20000),
(8, 5, 'VISA', 23000);

INSERT INTO `users` (`user_id`, `login_id`, `password_hash`, `pin_hash`, `name`, `phone_number`, `role`, `di`, `ci_hash`, `created_at`, `is_deleted`) VALUES
(1, 'user01', '$2a$10$mockPasswordHash0000000000000000000001', '$2a$10$mockPinHash00001', '김민준', '010-1000-0001', 'USER', 'DI0000000000000001', '$2a$10$mockCiHash0000000000000000000001', '2026-01-04 10:00:00', FALSE),
(2, 'user02', '$2a$10$mockPasswordHash0000000000000000000002', '$2a$10$mockPinHash00002', '이서연', '010-1000-0002', 'USER', 'DI0000000000000002', '$2a$10$mockCiHash0000000000000000000002', '2026-02-07 10:00:00', FALSE),
(3, 'user03', '$2a$10$mockPasswordHash0000000000000000000003', NULL, '박도윤', '010-1000-0003', 'USER', 'DI0000000000000003', '$2a$10$mockCiHash0000000000000000000003', '2026-03-10 10:00:00', FALSE),
(4, 'user04', '$2a$10$mockPasswordHash0000000000000000000004', '$2a$10$mockPinHash00004', '최지우', '010-1000-0004', 'USER', 'DI0000000000000004', '$2a$10$mockCiHash0000000000000000000004', '2026-04-13 10:00:00', FALSE),
(5, 'user05', '$2a$10$mockPasswordHash0000000000000000000005', '$2a$10$mockPinHash00005', '정하윤', '010-1000-0005', 'USER', 'DI0000000000000005', '$2a$10$mockCiHash0000000000000000000005', '2026-05-16 10:00:00', FALSE),
(6, 'user06', '$2a$10$mockPasswordHash0000000000000000000006', NULL, '강은우', '010-1000-0006', 'USER', 'DI0000000000000006', '$2a$10$mockCiHash0000000000000000000006', '2026-06-19 10:00:00', FALSE),
(7, 'user07', '$2a$10$mockPasswordHash0000000000000000000007', '$2a$10$mockPinHash00007', '조수아', '010-1000-0007', 'USER', 'DI0000000000000007', '$2a$10$mockCiHash0000000000000000000007', '2026-01-22 10:00:00', FALSE),
(8, 'user08', '$2a$10$mockPasswordHash0000000000000000000008', '$2a$10$mockPinHash00008', '윤지호', '010-1000-0008', 'USER', 'DI0000000000000008', '$2a$10$mockCiHash0000000000000000000008', '2026-02-25 10:00:00', FALSE),
(9, 'user09', '$2a$10$mockPasswordHash0000000000000000000009', NULL, '임서준', '010-1000-0009', 'USER', 'DI0000000000000009', '$2a$10$mockCiHash0000000000000000000009', '2026-03-01 10:00:00', FALSE),
(10, 'user10', '$2a$10$mockPasswordHash0000000000000000000010', '$2a$10$mockPinHash00010', '한예은', '010-1000-0010', 'USER', 'DI0000000000000010', '$2a$10$mockCiHash0000000000000000000010', '2026-04-04 10:00:00', FALSE);

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

INSERT INTO `user_cards` (`user_card_id`, `user_id`, `card_variant_id`, `card_token`, `card_last4`, `is_primary`, `recommendation_enabled`, `registered_at`, `card_status`) VALUES
(1, 1, 1, 'TOKEN-0001', '1001', TRUE, TRUE, '2026-01-05 11:00:00', 'ACTIVE'),
(2, 1, 3, 'TOKEN-0002', '1002', FALSE, TRUE, '2026-01-05 11:00:00', 'ACTIVE'),
(3, 1, 5, 'TOKEN-0003', '1003', FALSE, TRUE, '2026-01-05 11:00:00', 'ACTIVE'),
(4, 2, 3, 'TOKEN-0004', '1004', TRUE, TRUE, '2026-02-08 11:00:00', 'ACTIVE'),
(5, 2, 5, 'TOKEN-0005', '1005', FALSE, TRUE, '2026-02-08 11:00:00', 'ACTIVE'),
(6, 2, 6, 'TOKEN-0006', '1006', FALSE, TRUE, '2026-02-08 11:00:00', 'ACTIVE'),
(7, 3, 5, 'TOKEN-0007', '1007', TRUE, TRUE, '2026-03-11 11:00:00', 'ACTIVE'),
(8, 3, 6, 'TOKEN-0008', '1008', FALSE, TRUE, '2026-03-11 11:00:00', 'ACTIVE'),
(9, 3, 7, 'TOKEN-0009', '1009', FALSE, TRUE, '2026-03-11 11:00:00', 'ACTIVE'),
(10, 4, 6, 'TOKEN-0010', '1010', TRUE, TRUE, '2026-04-14 11:00:00', 'ACTIVE'),
(11, 4, 7, 'TOKEN-0011', '1011', FALSE, TRUE, '2026-04-14 11:00:00', 'ACTIVE'),
(12, 4, 1, 'TOKEN-0012', '1012', FALSE, TRUE, '2026-04-14 11:00:00', 'ACTIVE'),
(13, 5, 7, 'TOKEN-0013', '1013', TRUE, TRUE, '2026-05-17 11:00:00', 'ACTIVE'),
(14, 5, 1, 'TOKEN-0014', '1014', FALSE, TRUE, '2026-05-17 11:00:00', 'ACTIVE'),
(15, 5, 3, 'TOKEN-0015', '1015', FALSE, TRUE, '2026-05-17 11:00:00', 'ACTIVE'),
(16, 6, 1, 'TOKEN-0016', '1016', TRUE, TRUE, '2026-06-20 11:00:00', 'ACTIVE'),
(17, 6, 3, 'TOKEN-0017', '1017', FALSE, TRUE, '2026-06-20 11:00:00', 'ACTIVE'),
(18, 6, 5, 'TOKEN-0018', '1018', FALSE, TRUE, '2026-06-20 11:00:00', 'ACTIVE'),
(19, 7, 3, 'TOKEN-0019', '1019', TRUE, TRUE, '2026-01-23 11:00:00', 'ACTIVE'),
(20, 7, 5, 'TOKEN-0020', '1020', FALSE, TRUE, '2026-01-23 11:00:00', 'ACTIVE'),
(21, 7, 6, 'TOKEN-0021', '1021', FALSE, TRUE, '2026-01-23 11:00:00', 'ACTIVE'),
(22, 8, 5, 'TOKEN-0022', '1022', TRUE, TRUE, '2026-02-26 11:00:00', 'ACTIVE'),
(23, 8, 6, 'TOKEN-0023', '1023', FALSE, TRUE, '2026-02-26 11:00:00', 'ACTIVE'),
(24, 8, 7, 'TOKEN-0024', '1024', FALSE, TRUE, '2026-02-26 11:00:00', 'ACTIVE'),
(25, 9, 6, 'TOKEN-0025', '1025', TRUE, TRUE, '2026-03-02 11:00:00', 'ACTIVE'),
(26, 9, 7, 'TOKEN-0026', '1026', FALSE, TRUE, '2026-03-02 11:00:00', 'ACTIVE'),
(27, 9, 1, 'TOKEN-0027', '1027', FALSE, TRUE, '2026-03-02 11:00:00', 'ACTIVE'),
(28, 10, 7, 'TOKEN-0028', '1028', TRUE, TRUE, '2026-04-05 11:00:00', 'ACTIVE'),
(29, 10, 1, 'TOKEN-0029', '1029', FALSE, TRUE, '2026-04-05 11:00:00', 'ACTIVE'),
(30, 10, 3, 'TOKEN-0030', '1030', FALSE, TRUE, '2026-04-05 11:00:00', 'ACTIVE');

INSERT INTO `card_monthly_status` (`card_monthly_status_id`, `user_card_id`, `target_year_month`, `total_spending_amount`, `updated_at`) VALUES
(1, 1, '202605', 80037, '2026-05-31 23:59:59'),
(2, 1, '202606', 92382, '2026-06-30 23:59:59'),
(3, 1, '202607', 104727, '2026-07-22 23:59:59'),
(4, 2, '202605', 80074, '2026-05-31 23:59:59'),
(5, 2, '202606', 92419, '2026-06-30 23:59:59'),
(6, 2, '202607', 104764, '2026-07-22 23:59:59'),
(7, 3, '202605', 80111, '2026-05-31 23:59:59'),
(8, 3, '202606', 92456, '2026-06-30 23:59:59'),
(9, 3, '202607', 104801, '2026-07-22 23:59:59'),
(10, 4, '202605', 80148, '2026-05-31 23:59:59'),
(11, 4, '202606', 92493, '2026-06-30 23:59:59'),
(12, 4, '202607', 104838, '2026-07-22 23:59:59'),
(13, 5, '202605', 80185, '2026-05-31 23:59:59'),
(14, 5, '202606', 92530, '2026-06-30 23:59:59'),
(15, 5, '202607', 104875, '2026-07-22 23:59:59'),
(16, 6, '202605', 80222, '2026-05-31 23:59:59'),
(17, 6, '202606', 92567, '2026-06-30 23:59:59'),
(18, 6, '202607', 104912, '2026-07-22 23:59:59'),
(19, 7, '202605', 80259, '2026-05-31 23:59:59'),
(20, 7, '202606', 92604, '2026-06-30 23:59:59'),
(21, 7, '202607', 104949, '2026-07-22 23:59:59'),
(22, 8, '202605', 80296, '2026-05-31 23:59:59'),
(23, 8, '202606', 92641, '2026-06-30 23:59:59'),
(24, 8, '202607', 104986, '2026-07-22 23:59:59'),
(25, 9, '202605', 80333, '2026-05-31 23:59:59'),
(26, 9, '202606', 92678, '2026-06-30 23:59:59'),
(27, 9, '202607', 105023, '2026-07-22 23:59:59'),
(28, 10, '202605', 80370, '2026-05-31 23:59:59'),
(29, 10, '202606', 92715, '2026-06-30 23:59:59'),
(30, 10, '202607', 105060, '2026-07-22 23:59:59'),
(31, 11, '202605', 80407, '2026-05-31 23:59:59'),
(32, 11, '202606', 92752, '2026-06-30 23:59:59'),
(33, 11, '202607', 105097, '2026-07-22 23:59:59'),
(34, 12, '202605', 80444, '2026-05-31 23:59:59'),
(35, 12, '202606', 92789, '2026-06-30 23:59:59'),
(36, 12, '202607', 105134, '2026-07-22 23:59:59'),
(37, 13, '202605', 80481, '2026-05-31 23:59:59'),
(38, 13, '202606', 92826, '2026-06-30 23:59:59'),
(39, 13, '202607', 105171, '2026-07-22 23:59:59'),
(40, 14, '202605', 80518, '2026-05-31 23:59:59'),
(41, 14, '202606', 92863, '2026-06-30 23:59:59'),
(42, 14, '202607', 105208, '2026-07-22 23:59:59'),
(43, 15, '202605', 80555, '2026-05-31 23:59:59'),
(44, 15, '202606', 92900, '2026-06-30 23:59:59'),
(45, 15, '202607', 105245, '2026-07-22 23:59:59'),
(46, 16, '202605', 80592, '2026-05-31 23:59:59'),
(47, 16, '202606', 92937, '2026-06-30 23:59:59'),
(48, 16, '202607', 105282, '2026-07-22 23:59:59'),
(49, 17, '202605', 80629, '2026-05-31 23:59:59'),
(50, 17, '202606', 92974, '2026-06-30 23:59:59'),
(51, 17, '202607', 105319, '2026-07-22 23:59:59'),
(52, 18, '202605', 80666, '2026-05-31 23:59:59'),
(53, 18, '202606', 93011, '2026-06-30 23:59:59'),
(54, 18, '202607', 105356, '2026-07-22 23:59:59'),
(55, 19, '202605', 80703, '2026-05-31 23:59:59'),
(56, 19, '202606', 93048, '2026-06-30 23:59:59'),
(57, 19, '202607', 105393, '2026-07-22 23:59:59'),
(58, 20, '202605', 80740, '2026-05-31 23:59:59'),
(59, 20, '202606', 93085, '2026-06-30 23:59:59'),
(60, 20, '202607', 105430, '2026-07-22 23:59:59'),
(61, 21, '202605', 80777, '2026-05-31 23:59:59'),
(62, 21, '202606', 93122, '2026-06-30 23:59:59'),
(63, 21, '202607', 105467, '2026-07-22 23:59:59'),
(64, 22, '202605', 80814, '2026-05-31 23:59:59'),
(65, 22, '202606', 93159, '2026-06-30 23:59:59'),
(66, 22, '202607', 105504, '2026-07-22 23:59:59'),
(67, 23, '202605', 80851, '2026-05-31 23:59:59'),
(68, 23, '202606', 93196, '2026-06-30 23:59:59'),
(69, 23, '202607', 105541, '2026-07-22 23:59:59'),
(70, 24, '202605', 80888, '2026-05-31 23:59:59'),
(71, 24, '202606', 93233, '2026-06-30 23:59:59'),
(72, 24, '202607', 105578, '2026-07-22 23:59:59'),
(73, 25, '202605', 80925, '2026-05-31 23:59:59'),
(74, 25, '202606', 93270, '2026-06-30 23:59:59'),
(75, 25, '202607', 105615, '2026-07-22 23:59:59'),
(76, 26, '202605', 80962, '2026-05-31 23:59:59'),
(77, 26, '202606', 93307, '2026-06-30 23:59:59'),
(78, 26, '202607', 105652, '2026-07-22 23:59:59'),
(79, 27, '202605', 80999, '2026-05-31 23:59:59'),
(80, 27, '202606', 93344, '2026-06-30 23:59:59'),
(81, 27, '202607', 105689, '2026-07-22 23:59:59'),
(82, 28, '202605', 81036, '2026-05-31 23:59:59'),
(83, 28, '202606', 93381, '2026-06-30 23:59:59'),
(84, 28, '202607', 105726, '2026-07-22 23:59:59'),
(85, 29, '202605', 81073, '2026-05-31 23:59:59'),
(86, 29, '202606', 93418, '2026-06-30 23:59:59'),
(87, 29, '202607', 105763, '2026-07-22 23:59:59'),
(88, 30, '202605', 81110, '2026-05-31 23:59:59'),
(89, 30, '202606', 93455, '2026-06-30 23:59:59'),
(90, 30, '202607', 105800, '2026-07-22 23:59:59');

INSERT INTO `benefit_per_user_card` (`user_card_id`, `benefit_id`) VALUES
(1, 1), (1, 2), (2, 3), (3, 4), (3, 5), (4, 3), (5, 4), (5, 5), (6, 6), (7, 4),
(7, 5), (8, 6), (9, 7), (9, 8), (10, 6), (11, 7), (11, 8), (12, 1), (12, 2), (13, 7),
(13, 8), (14, 1), (14, 2), (15, 3), (16, 1), (16, 2), (17, 3), (18, 4), (18, 5), (19, 3),
(20, 4), (20, 5), (21, 6), (22, 4), (22, 5), (23, 6), (24, 7), (24, 8), (25, 6), (26, 7),
(26, 8), (27, 1), (27, 2), (28, 7), (28, 8), (29, 1), (29, 2), (30, 3);

INSERT INTO `payments` (`payment_id`, `user_card_id`, `merchant_id`, `payment_time`, `original_amount`, `discount_amount`, `final_amount`, `payment_status`, `payment_method`) VALUES
(1, 1, 2, '2026-01-13 09:30:00', 10913, 1100, 9813, 'APPROVED', 'BARCODE'),
(2, 1, 3, '2026-01-20 14:30:00', 11284, 0, 11284, 'APPROVED', 'QR'),
(3, 2, 3, '2026-01-16 09:30:00', 11826, 1200, 10626, 'APPROVED', 'BARCODE'),
(4, 2, 4, '2026-01-23 14:30:00', 12197, 0, 12197, 'APPROVED', 'QR'),
(5, 3, 4, '2026-01-19 09:30:00', 12739, 1300, 11439, 'APPROVED', 'BARCODE'),
(6, 3, 5, '2026-01-11 14:30:00', 13110, 0, 13110, 'APPROVED', 'QR'),
(7, 4, 5, '2026-02-22 09:30:00', 13652, 1400, 12252, 'APPROVED', 'BARCODE'),
(8, 4, 6, '2026-02-14 14:30:00', 14023, 0, 14023, 'APPROVED', 'QR'),
(9, 5, 6, '2026-02-10 09:30:00', 14565, 1500, 13065, 'APPROVED', 'BARCODE'),
(10, 5, 7, '2026-02-17 14:30:00', 14936, 0, 14936, 'APPROVED', 'QR'),
(11, 6, 7, '2026-02-13 09:30:00', 15478, 1500, 13978, 'APPROVED', 'BARCODE'),
(12, 6, 8, '2026-02-20 14:30:00', 15849, 0, 15849, 'APPROVED', 'QR'),
(13, 7, 8, '2026-03-16 09:30:00', 16391, 1600, 14791, 'APPROVED', 'BARCODE'),
(14, 7, 9, '2026-03-23 14:30:00', 16762, 0, 16762, 'APPROVED', 'QR'),
(15, 8, 9, '2026-03-19 09:30:00', 17304, 1700, 15604, 'APPROVED', 'BARCODE'),
(16, 8, 10, '2026-03-11 14:30:00', 17675, 0, 17675, 'APPROVED', 'QR'),
(17, 9, 10, '2026-03-22 09:30:00', 18217, 1800, 16417, 'APPROVED', 'BARCODE'),
(18, 9, 1, '2026-03-14 14:30:00', 18588, 0, 18588, 'APPROVED', 'QR'),
(19, 10, 1, '2026-04-10 09:30:00', 19130, 1900, 17230, 'APPROVED', 'BARCODE'),
(20, 10, 2, '2026-04-17 14:30:00', 19501, 0, 19501, 'CANCELED', 'QR'),
(21, 11, 2, '2026-04-13 09:30:00', 20043, 2000, 18043, 'CANCELED', 'BARCODE'),
(22, 11, 3, '2026-04-20 14:30:00', 20414, 0, 20414, 'APPROVED', 'QR'),
(23, 12, 3, '2026-04-16 09:30:00', 20956, 2100, 18856, 'APPROVED', 'BARCODE'),
(24, 12, 4, '2026-04-23 14:30:00', 21327, 0, 21327, 'APPROVED', 'QR'),
(25, 13, 4, '2026-05-19 09:30:00', 21869, 2200, 19669, 'APPROVED', 'BARCODE'),
(26, 13, 5, '2026-05-11 14:30:00', 22240, 0, 22240, 'APPROVED', 'QR'),
(27, 14, 5, '2026-05-22 09:30:00', 22782, 2300, 20482, 'APPROVED', 'BARCODE'),
(28, 14, 6, '2026-05-14 14:30:00', 23153, 0, 23153, 'APPROVED', 'QR'),
(29, 15, 6, '2026-05-10 09:30:00', 23695, 2400, 21295, 'APPROVED', 'BARCODE'),
(30, 15, 7, '2026-05-17 14:30:00', 24066, 0, 24066, 'APPROVED', 'QR'),
(31, 16, 7, '2026-06-13 09:30:00', 24608, 2500, 22108, 'APPROVED', 'BARCODE'),
(32, 16, 8, '2026-06-20 14:30:00', 24979, 0, 24979, 'APPROVED', 'QR'),
(33, 17, 8, '2026-06-16 09:30:00', 25521, 2600, 22921, 'APPROVED', 'BARCODE'),
(34, 17, 9, '2026-06-23 14:30:00', 25892, 0, 25892, 'APPROVED', 'QR'),
(35, 18, 9, '2026-06-19 09:30:00', 26434, 2600, 23834, 'APPROVED', 'BARCODE'),
(36, 18, 10, '2026-06-11 14:30:00', 26805, 0, 26805, 'APPROVED', 'QR'),
(37, 19, 10, '2026-01-22 09:30:00', 27347, 2700, 24647, 'APPROVED', 'BARCODE'),
(38, 19, 1, '2026-01-14 14:30:00', 27718, 0, 27718, 'APPROVED', 'QR'),
(39, 20, 1, '2026-01-10 09:30:00', 28260, 2800, 25460, 'APPROVED', 'BARCODE'),
(40, 20, 2, '2026-01-17 14:30:00', 28631, 0, 28631, 'APPROVED', 'QR'),
(41, 21, 2, '2026-01-13 09:30:00', 29173, 2900, 26273, 'APPROVED', 'BARCODE'),
(42, 21, 3, '2026-01-20 14:30:00', 29544, 0, 29544, 'CANCELED', 'QR'),
(43, 22, 3, '2026-02-16 09:30:00', 30086, 3000, 27086, 'CANCELED', 'BARCODE'),
(44, 22, 4, '2026-02-23 14:30:00', 30457, 0, 30457, 'APPROVED', 'QR'),
(45, 23, 4, '2026-02-19 09:30:00', 30999, 3100, 27899, 'APPROVED', 'BARCODE'),
(46, 23, 5, '2026-02-11 14:30:00', 31370, 0, 31370, 'APPROVED', 'QR'),
(47, 24, 5, '2026-02-22 09:30:00', 31912, 3200, 28712, 'APPROVED', 'BARCODE'),
(48, 24, 6, '2026-02-14 14:30:00', 32283, 0, 32283, 'APPROVED', 'QR'),
(49, 25, 6, '2026-03-10 09:30:00', 32825, 3300, 29525, 'APPROVED', 'BARCODE'),
(50, 25, 7, '2026-03-17 14:30:00', 33196, 0, 33196, 'APPROVED', 'QR'),
(51, 26, 7, '2026-03-13 09:30:00', 33738, 3400, 30338, 'APPROVED', 'BARCODE'),
(52, 26, 8, '2026-03-20 14:30:00', 34109, 0, 34109, 'APPROVED', 'QR'),
(53, 27, 8, '2026-03-16 09:30:00', 34651, 3500, 31151, 'APPROVED', 'BARCODE'),
(54, 27, 9, '2026-03-23 14:30:00', 35022, 0, 35022, 'APPROVED', 'QR'),
(55, 28, 9, '2026-04-19 09:30:00', 35564, 3600, 31964, 'APPROVED', 'BARCODE'),
(56, 28, 10, '2026-04-11 14:30:00', 35935, 0, 35935, 'APPROVED', 'QR'),
(57, 29, 10, '2026-04-22 09:30:00', 36477, 3600, 32877, 'APPROVED', 'BARCODE'),
(58, 29, 1, '2026-04-14 14:30:00', 36848, 0, 36848, 'APPROVED', 'QR'),
(59, 30, 1, '2026-04-10 09:30:00', 37390, 3700, 33690, 'APPROVED', 'BARCODE'),
(60, 30, 2, '2026-04-17 14:30:00', 37761, 0, 37761, 'APPROVED', 'QR');

INSERT INTO `bookmarked_stores` (`user_id`, `merchant_id`, `created_at`, `is_deleted`) VALUES
(1, 1, '2026-01-06 09:00:00', FALSE),
(1, 3, '2026-01-08 09:00:00', FALSE),
(2, 2, '2026-02-09 09:00:00', FALSE),
(3, 5, '2026-03-12 09:00:00', FALSE),
(4, 4, '2026-04-15 09:00:00', FALSE),
(5, 1, '2026-05-18 09:00:00', FALSE),
(6, 6, '2026-06-21 09:00:00', FALSE),
(7, 7, '2026-01-24 09:00:00', FALSE),
(8, 8, '2026-02-27 09:00:00', FALSE),
(9, 9, '2026-03-03 09:00:00', FALSE),
(10, 10, '2026-04-06 09:00:00', FALSE);
