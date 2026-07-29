-- BenePay 개발용 스키마 + 목업 데이터.
--
-- 손으로 고치지 말 것. 이 파일은 로컬 benepay DB를 mysqldump로 받아 만든다. 손으로 관리하던
-- 시절에 컬럼명(password_hash/ci_hash)과 테이블 구성이 실제 DB와 어긋나 코드가 뜨지 않는 일이
-- 있었다. 스키마를 바꿨으면 DB에 먼저 적용하고 다시 덤프하는 순서로 간다.
--
-- 이 디렉터리(docker/mysql/init)는 docker-compose.yml이 /docker-entrypoint-initdb.d로 통째로
-- 마운트하므로, 여기 있는 .sql은 로컬이든 EC2 운영이든 컨테이너 최초 기동 시 자동 실행된다.
-- dev-login용 계정(dev1~dev10)을 여기 두지 않는 이유가 그것이다 - 그 계정은 개발자가 명시적으로
-- 적용할 때만 들어가야 한다. 필요하면 docker/mysql/dev-seed/dev-accounts.sql을 참고할 것.
--
-- encryption_keys의 키도 개발 전용이다. 이 파일이 저장소에 그대로 올라가므로 운영 환경에서는
-- 반드시 다른 키로 교체해야 한다.

-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: benepay
-- ------------------------------------------------------
-- Server version	8.0.33
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `benepay`
--

/*!40000 DROP DATABASE IF EXISTS `benepay`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `benepay` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `benepay`;

--
-- Table structure for table `bookmarked_stores`
--

DROP TABLE IF EXISTS `bookmarked_stores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookmarked_stores` (
  `bookmark_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`bookmark_id`),
  UNIQUE KEY `UQ_bookmarked_stores_user_merchant` (`user_id`,`merchant_id`),
  KEY `FK_merchants_TO_bookmarked_stores` (`merchant_id`),
  CONSTRAINT `FK_merchants_TO_bookmarked_stores` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`merchant_id`),
  CONSTRAINT `FK_users_TO_bookmarked_stores` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bookmarked_stores`
--

LOCK TABLES `bookmarked_stores` WRITE;
/*!40000 ALTER TABLE `bookmarked_stores` DISABLE KEYS */;
/*!40000 ALTER TABLE `bookmarked_stores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_issuance_events`
--

DROP TABLE IF EXISTS `card_issuance_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card_issuance_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(36) NOT NULL,
  `ci_hash` char(64) NOT NULL,
  `card_ref_id` varchar(64) NOT NULL,
  `card_last4` char(4) DEFAULT NULL,
  `card_type` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `fail_reason` varchar(30) DEFAULT NULL,
  `processed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ_card_issuance_events_event_id` (`event_id`),
  KEY `FK_common_codes_TO_card_issuance_events_card_type` (`card_type`),
  KEY `FK_common_codes_TO_card_issuance_events_status` (`status`),
  KEY `IDX_card_issuance_events_ci_hash` (`ci_hash`),
  CONSTRAINT `FK_common_codes_TO_card_issuance_events_card_type` FOREIGN KEY (`card_type`) REFERENCES `common_codes` (`code`),
  CONSTRAINT `FK_common_codes_TO_card_issuance_events_status` FOREIGN KEY (`status`) REFERENCES `common_codes` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_issuance_events`
--

LOCK TABLES `card_issuance_events` WRITE;
/*!40000 ALTER TABLE `card_issuance_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `card_issuance_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_monthly_status`
--

DROP TABLE IF EXISTS `card_monthly_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card_monthly_status` (
  `card_monthly_status_id` bigint NOT NULL AUTO_INCREMENT,
  `user_card_id` bigint NOT NULL,
  `target_year_month` char(6) NOT NULL,
  `total_spending_amount` decimal(12,0) NOT NULL DEFAULT '0',
  `updated_at` datetime NOT NULL COMMENT '해당 월별 실적 데이터가 마지막으로 변경된 시간',
  PRIMARY KEY (`card_monthly_status_id`),
  UNIQUE KEY `UQ_card_monthly_status_card_month` (`user_card_id`,`target_year_month`),
  CONSTRAINT `FK_user_cards_TO_card_monthly_status` FOREIGN KEY (`user_card_id`) REFERENCES `user_cards` (`user_card_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_monthly_status`
--

LOCK TABLES `card_monthly_status` WRITE;
/*!40000 ALTER TABLE `card_monthly_status` DISABLE KEYS */;
INSERT INTO `card_monthly_status` VALUES (1,1,'202607',105400,'2026-07-10 15:05:00'),(2,2,'202607',11400,'2026-07-02 19:30:00'),(3,3,'202607',0,'2026-07-01 00:00:00');
/*!40000 ALTER TABLE `card_monthly_status` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_variants`
--

DROP TABLE IF EXISTS `card_variants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card_variants` (
  `card_variant_id` char(2) NOT NULL,
  `card_network` varchar(20) NOT NULL COMMENT '국내전용, visa',
  PRIMARY KEY (`card_variant_id`),
  KEY `FK_common_codes_TO_card_variants_network` (`card_network`),
  CONSTRAINT `FK_common_codes_TO_card_variants_network` FOREIGN KEY (`card_network`) REFERENCES `common_codes` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_variants`
--

LOCK TABLES `card_variants` WRITE;
/*!40000 ALTER TABLE `card_variants` DISABLE KEYS */;
INSERT INTO `card_variants` VALUES ('01','DOMESTIC'),('03','MASTER'),('02','VISA');
/*!40000 ALTER TABLE `card_variants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cards`
--

DROP TABLE IF EXISTS `cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cards` (
  `card_id` bigint NOT NULL AUTO_INCREMENT,
  `card_name` varchar(50) NOT NULL,
  `card_type` varchar(20) NOT NULL COMMENT 'CREDIT\r\nCHECK',
  `card_variant_id` char(2) NOT NULL,
  `annual_fee` bigint NOT NULL DEFAULT '0',
  `card_image_url` varchar(255) DEFAULT NULL,
  `description` text,
  `is_supported` tinyint(1) NOT NULL DEFAULT '1',
  `min_benefit_amount` decimal(8,0) DEFAULT NULL,
  `benefits_info` json NOT NULL,
  PRIMARY KEY (`card_id`),
  KEY `FK_common_codes_TO_cards_card_type` (`card_type`),
  KEY `FK_card_variants_TO_cards` (`card_variant_id`),
  CONSTRAINT `FK_card_variants_TO_cards` FOREIGN KEY (`card_variant_id`) REFERENCES `card_variants` (`card_variant_id`),
  CONSTRAINT `FK_common_codes_TO_cards_card_type` FOREIGN KEY (`card_type`) REFERENCES `common_codes` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cards`
--

LOCK TABLES `cards` WRITE;
/*!40000 ALTER TABLE `cards` DISABLE KEYS */;
INSERT INTO `cards` VALUES (1,'KB국민 노리카드','CREDIT','01',15000,'https://cdn.benepay.com/cards/nori.png','카페·편의점 적립에 특화된 생활밀착형 카드',1,300000,'{\"categories\": [{\"monthlyCap\": 15000, \"categoryCode\": \"CAFE\", \"discountRate\": 10}, {\"monthlyCap\": 10000, \"categoryCode\": \"CVS\", \"discountRate\": 5}]}'),(2,'KB국민 탄탄대로 체크카드','CHECK','01',0,'https://cdn.benepay.com/cards/tantan.png','주유·마트 할인에 특화된 체크카드',1,200000,'{\"categories\": [{\"monthlyCap\": 12000, \"categoryCode\": \"GAS\", \"discountRate\": 8}, {\"monthlyCap\": 8000, \"categoryCode\": \"MART\", \"discountRate\": 3}]}'),(3,'KB국민 청춘대로 카드','CREDIT','02',10000,'https://cdn.benepay.com/cards/chungchun.png','대중교통·구독서비스 할인에 특화된 20대 전용 카드',1,250000,'{\"categories\": [{\"monthlyCap\": 10000, \"categoryCode\": \"CAFE\", \"discountRate\": 15}]}');
/*!40000 ALTER TABLE `cards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `common_code_groups`
--

DROP TABLE IF EXISTS `common_code_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `common_code_groups` (
  `group_code` varchar(30) NOT NULL,
  `group_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `common_code_groups`
--

LOCK TABLES `common_code_groups` WRITE;
/*!40000 ALTER TABLE `common_code_groups` DISABLE KEYS */;
INSERT INTO `common_code_groups` VALUES ('CARD_ISSUANCE_STATUS','카드 발급 이벤트 처리 상태',NULL),('CARD_NETWORK','카드 결제망',NULL),('CARD_STATUS','등록카드 상태',NULL),('CARD_TYPE','카드 종류',NULL),('PAYMENT_METHOD','결제 수단',NULL),('PAYMENT_STATUS','결제 상태',NULL),('USER_ROLE','사용자 권한',NULL);
/*!40000 ALTER TABLE `common_code_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `common_codes`
--

DROP TABLE IF EXISTS `common_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `common_codes` (
  `code` varchar(30) NOT NULL,
  `group_code` varchar(30) NOT NULL,
  `code_name` varchar(100) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`code`,`group_code`),
  UNIQUE KEY `UQ_common_codes_code` (`code`),
  KEY `FK_common_code_groups_TO_common_codes_1` (`group_code`),
  CONSTRAINT `FK_common_code_groups_TO_common_codes_1` FOREIGN KEY (`group_code`) REFERENCES `common_code_groups` (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `common_codes`
--

LOCK TABLES `common_codes` WRITE;
/*!40000 ALTER TABLE `common_codes` DISABLE KEYS */;
INSERT INTO `common_codes` VALUES ('ACTIVE','CARD_STATUS','정상 사용',1,1),('ADMIN','USER_ROLE','관리자',2,1),('APPROVED','PAYMENT_STATUS','승인',2,1),('BARCODE','PAYMENT_METHOD','바코드',1,1),('CANCELED','PAYMENT_STATUS','취소',3,1),('CHECK','CARD_TYPE','체크카드',2,1),('CREDIT','CARD_TYPE','신용카드',1,1),('DELETED','CARD_STATUS','사용자가 삭제',5,1),('DOMESTIC','CARD_NETWORK','국내전용',1,1),('EXPIRED','CARD_STATUS','만료',3,1),('FAILED','CARD_ISSUANCE_STATUS','처리 실패',3,1),('MASTER','CARD_NETWORK','Mastercard',3,1),('PENDING','PAYMENT_STATUS','대기',1,1),('PROCESSED','CARD_ISSUANCE_STATUS','처리 완료',2,1),('QR','PAYMENT_METHOD','QR코드',2,1),('RECEIVED','CARD_ISSUANCE_STATUS','수신됨',1,1),('SUSPENDED','CARD_STATUS','정지',2,1),('UNLINKED','CARD_STATUS','연동 해제',4,1),('USER','USER_ROLE','일반 사용자',1,1),('VISA','CARD_NETWORK','VISA',2,1);
/*!40000 ALTER TABLE `common_codes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `encryption_keys`
--

DROP TABLE IF EXISTS `encryption_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `encryption_keys` (
  `key_id` bigint NOT NULL AUTO_INCREMENT,
  `key_alias` varchar(50) NOT NULL,
  `key_value` varchar(255) NOT NULL COMMENT 'Base64 인코딩된 대칭키',
  `algorithm` varchar(30) NOT NULL DEFAULT 'AES256',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`key_id`),
  UNIQUE KEY `UQ_encryption_keys_key_alias` (`key_alias`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `encryption_keys`
--

LOCK TABLES `encryption_keys` WRITE;
/*!40000 ALTER TABLE `encryption_keys` DISABLE KEYS */;
INSERT INTO `encryption_keys` VALUES (1,'CI','ZHsppXZssP/iNxL0DhGMYRpfzs9ksDslBH+j1E8UeVU=','AES256',1,'2026-07-28 17:45:12');
/*!40000 ALTER TABLE `encryption_keys` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchant_brands`
--

DROP TABLE IF EXISTS `merchant_brands`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_brands` (
  `brand_id` bigint NOT NULL AUTO_INCREMENT,
  `brand_code` varchar(30) NOT NULL,
  `brand_name` varchar(100) NOT NULL,
  `brand_logo` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`brand_id`),
  UNIQUE KEY `UQ_merchant_brands_brand_code` (`brand_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchant_brands`
--

LOCK TABLES `merchant_brands` WRITE;
/*!40000 ALTER TABLE `merchant_brands` DISABLE KEYS */;
INSERT INTO `merchant_brands` VALUES (1,'STARBUCKS','스타벅스','https://cdn.benepay.com/brands/starbucks.png'),(2,'GS25','GS25','https://cdn.benepay.com/brands/gs25.png'),(3,'EMART24','이마트24','https://cdn.benepay.com/brands/emart24.png');
/*!40000 ALTER TABLE `merchant_brands` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchant_categories`
--

DROP TABLE IF EXISTS `merchant_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_categories` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(30) NOT NULL,
  `category_name` varchar(50) NOT NULL,
  `category_icon` varchar(100) NOT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `UQ_merchant_categories_category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchant_categories`
--

LOCK TABLES `merchant_categories` WRITE;
/*!40000 ALTER TABLE `merchant_categories` DISABLE KEYS */;
INSERT INTO `merchant_categories` VALUES (1,'CAFE','카페','https://cdn.benepay.com/icons/cafe.svg'),(2,'CVS','편의점','https://cdn.benepay.com/icons/cvs.svg'),(3,'MART','마트','https://cdn.benepay.com/icons/mart.svg'),(4,'GAS','주유소','https://cdn.benepay.com/icons/gas.svg');
/*!40000 ALTER TABLE `merchant_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchants`
--

DROP TABLE IF EXISTS `merchants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchants` (
  `merchant_id` bigint NOT NULL AUTO_INCREMENT,
  `brand_id` bigint DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `merchant_code` varchar(50) NOT NULL,
  `merchant_name` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`merchant_id`),
  KEY `FK_merchant_brands_TO_merchants` (`brand_id`),
  KEY `FK_merchant_categories_TO_merchants` (`category_id`),
  CONSTRAINT `FK_merchant_brands_TO_merchants` FOREIGN KEY (`brand_id`) REFERENCES `merchant_brands` (`brand_id`),
  CONSTRAINT `FK_merchant_categories_TO_merchants` FOREIGN KEY (`category_id`) REFERENCES `merchant_categories` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchants`
--

LOCK TABLES `merchants` WRITE;
/*!40000 ALTER TABLE `merchants` DISABLE KEYS */;
INSERT INTO `merchants` VALUES (1,1,1,'MC-0000001','스타벅스 강남점','서울특별시 강남구 테헤란로 123',37.4980000,127.0276000,'02-1234-5678'),(2,2,2,'MC-0000002','GS25 역삼역점','서울특별시 강남구 역삼동 456',37.5006000,127.0364000,'02-2345-6789'),(3,NULL,3,'MC-0000003','동네마트 역삼점','서울특별시 강남구 역삼동 789',37.5010000,127.0370000,NULL),(4,NULL,4,'MC-0000004','SK주유소 삼성점','서울특별시 강남구 삼성동 12',37.5100000,127.0500000,'02-3456-7890');
/*!40000 ALTER TABLE `merchants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `user_card_id` bigint NOT NULL,
  `payment_time` datetime NOT NULL,
  `original_amount` decimal(10,0) NOT NULL,
  `discount_amount` decimal(10,0) NOT NULL DEFAULT '0',
  `final_amount` decimal(10,0) NOT NULL,
  `payment_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'APPROVED\r\nCANCELED',
  `payment_method` varchar(20) NOT NULL COMMENT 'BARCODE / QR',
  PRIMARY KEY (`payment_id`),
  KEY `FK_merchants_TO_payments` (`merchant_id`),
  KEY `FK_user_cards_TO_payments` (`user_card_id`),
  KEY `FK_common_codes_TO_payments_status` (`payment_status`),
  KEY `FK_common_codes_TO_payments_method` (`payment_method`),
  CONSTRAINT `FK_common_codes_TO_payments_method` FOREIGN KEY (`payment_method`) REFERENCES `common_codes` (`code`),
  CONSTRAINT `FK_common_codes_TO_payments_status` FOREIGN KEY (`payment_status`) REFERENCES `common_codes` (`code`),
  CONSTRAINT `FK_merchants_TO_payments` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`merchant_id`),
  CONSTRAINT `FK_user_cards_TO_payments` FOREIGN KEY (`user_card_id`) REFERENCES `user_cards` (`user_card_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (1,1,1,'2026-07-01 08:12:00',6000,600,5400,'APPROVED','QR'),(2,2,2,'2026-07-02 19:30:00',12000,600,11400,'APPROVED','BARCODE'),(3,4,3,'2026-07-03 07:50:00',50000,4000,46000,'CANCELED','QR'),(4,1,1,'2026-07-10 15:05:00',4500,450,4050,'PENDING','QR');
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_cards`
--

DROP TABLE IF EXISTS `user_cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_cards` (
  `user_card_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `card_id` bigint NOT NULL,
  `token_id` varchar(36) NOT NULL,
  `payment_token` varchar(19) NOT NULL,
  `token_expiry_date` date NOT NULL,
  `pan_hash` char(64) NOT NULL,
  `pan_last4` char(4) NOT NULL,
  `par` char(29) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `recommendation_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_card_id`),
  UNIQUE KEY `UQ_user_cards_token_id` (`token_id`),
  UNIQUE KEY `UQ_user_cards_payment_token` (`payment_token`),
  KEY `FK_users_TO_user_cards` (`user_id`),
  KEY `FK_cards_TO_user_cards` (`card_id`),
  KEY `FK_common_codes_TO_user_cards_status` (`status`),
  KEY `IDX_user_cards_pan_hash` (`pan_hash`),
  CONSTRAINT `FK_cards_TO_user_cards` FOREIGN KEY (`card_id`) REFERENCES `cards` (`card_id`),
  CONSTRAINT `FK_common_codes_TO_user_cards_status` FOREIGN KEY (`status`) REFERENCES `common_codes` (`code`),
  CONSTRAINT `FK_users_TO_user_cards` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_cards`
--

LOCK TABLES `user_cards` WRITE;
/*!40000 ALTER TABLE `user_cards` DISABLE KEYS */;
INSERT INTO `user_cards` VALUES (1,1,1,'tok_5f9a1b2c3d4e5f6a7b8c9d0e1f2a3b4c','9876543210981234','2028-12-31','a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234','1234','H1234567890123456789012345678','ACTIVE',1,1,'2026-03-01 10:00:00',NULL,NULL),(2,1,2,'tok_7c2a9f1b3e5d6a7b8c9d0e1f2a3b4c5d','9876543210985678','2027-11-30','b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456','5678','H2345678901234567890123456789','ACTIVE',0,1,'2026-03-05 11:20:00',NULL,NULL),(3,2,3,'tok_3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a','9876543210989012','2029-06-30','c3d4e5f6789012345678901234567890abcdef1234567890abcdef12345678','9012','H3456789012345678901234567890','SUSPENDED',1,0,'2026-01-20 08:45:00','2026-06-01 09:00:00',NULL);
/*!40000 ALTER TABLE `user_cards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `login_id` varchar(30) NOT NULL,
  `login_password_hash` varchar(255) NOT NULL,
  `pin_hash` varchar(100) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `birth_date` char(8) DEFAULT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER',
  `di` varchar(100) NOT NULL,
  `ci_encrypted` varchar(200) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `fcm_token` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UQ_users_login_id` (`login_id`),
  UNIQUE KEY `UQ_users_di` (`di`),
  KEY `FK_common_codes_TO_users_role` (`role`),
  CONSTRAINT `FK_common_codes_TO_users_role` FOREIGN KEY (`role`) REFERENCES `common_codes` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'hong123','$2a$10$mockHashValueForHongGildong0000000000000000000000000','$2a$10$mockPinHash1','홍길동','01012345678','19900101','USER','di_mock_0000000000000000000000000000000000000000000000000000000000000000000000000000000000','ci_encrypted_mock_AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHIIIIJJJJKKKKLLLLMMMMNNNN','2026-01-10 09:00:00',0,'fcm_token_mock_device_a1b2c3'),(2,'kim456','$2a$10$mockHashValueForKimYuna00000000000000000000000000000',NULL,'김유나','01098765432','19950505','USER','di_mock_1111111111111111111111111111111111111111111111111111111111111111111111111111111111','ci_encrypted_mock_ZZZZYYYYXXXXWWWWVVVVUUUUTTTTSSSSRRRRQQQQPPPPOOOONNNN','2026-02-15 14:30:00',0,'fcm_token_mock_device_x9y8z7');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

