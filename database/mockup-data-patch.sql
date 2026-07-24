USE benepay;

-- 목업 화면용 데모 사용자/카드 데이터
UPDATE users SET name='김포인트', login_id='point', email='point@example.com' WHERE user_id=1;

UPDATE cards SET card_name='Deep Dream Platinum', card_company_name='신한카드',
    description='카페와 음식점 혜택을 제공하는 대표 카드', min_benefit_amount=300000 WHERE card_id=1;
UPDATE cards SET card_name='Shinhan The More', card_company_name='신한카드',
    description='전 가맹점 포인트 적립 카드', min_benefit_amount=0 WHERE card_id=2;
UPDATE cards SET card_name='매일 캐시백 체크', card_company_name='BenePay', card_type='CHECK',
    description='실적 조건 없는 기본 캐시백 카드', min_benefit_amount=0 WHERE card_id=3;

UPDATE user_cards SET card_token='TOKEN-DEEP-1234', card_last4='1234', is_primary=TRUE WHERE user_card_id=1;
UPDATE user_cards SET card_token='TOKEN-MORE-5678', card_last4='5678', is_primary=FALSE WHERE user_card_id=2;
UPDATE user_cards SET card_token='TOKEN-CASH-9012', card_last4='9012', is_primary=FALSE WHERE user_card_id=3;

UPDATE card_benefits SET benefit_type='청구', benefit_name='카페 10% 할인',
    description='카페 결제 시 10% 할인',
    benefits_info=JSON_OBJECT('category','CAFE','discount_rate',10,'monthly_limit',15000) WHERE benefit_id=1;
UPDATE card_benefits SET benefit_type='환급', benefit_name='음식점 5% 캐시백',
    description='음식점 결제 시 5% 캐시백',
    benefits_info=JSON_OBJECT('category','FOOD','cashback_rate',5,'monthly_limit',20000) WHERE benefit_id=2;
UPDATE card_benefits SET benefit_type='환급', benefit_name='기본 적립 3%',
    description='전 가맹점 결제액 3% 포인트 적립',
    benefits_info=JSON_OBJECT('category','ALL','point_rate',3,'monthly_limit',30000) WHERE benefit_id=3;
UPDATE card_benefits SET benefit_type='환급', benefit_name='기본 캐시백 0.3%',
    description='실적 조건 없이 0.3% 캐시백',
    benefits_info=JSON_OBJECT('category','ALL','cashback_rate',0.3,'monthly_limit',10000) WHERE benefit_id=4;

INSERT INTO merchant_categories(category_id,category_code,category_name) VALUES
(6,'MEDICAL','병원/약국'),(7,'FUEL','주유소')
ON DUPLICATE KEY UPDATE category_name=VALUES(category_name);

UPDATE merchants SET category_id=1, merchant_code='GIMPO-STARBUCKS', merchant_name='스타벅스 김포점', brand_name='스타벅스',
    address='경기도 김포시 김포대로 100', latitude=37.6209000, longitude=126.7198000 WHERE merchant_id=1;
UPDATE merchants SET category_id=2, merchant_code='EMART-MALL', merchant_name='이마트몰', brand_name='이마트',
    address='경기도 김포시 풍무로 20', latitude=37.6179000, longitude=126.7167000 WHERE merchant_id=2;
UPDATE merchants SET category_id=1, merchant_code='TODAY-COFFEE', merchant_name='오늘의 커피 로스터스', brand_name='오늘의 커피',
    address='경기도 김포시 김포한강1로 80', latitude=37.6218000, longitude=126.7241000 WHERE merchant_id=3;
UPDATE merchants SET category_id=1, merchant_code='SOSOHAN-TABLE', merchant_name='소소한 식탁', brand_name='소소한 식탁',
    address='경기도 김포시 김포한강2로 150', latitude=37.6245000, longitude=126.7286000 WHERE merchant_id=4;
UPDATE merchants SET category_id=2, merchant_code='CU-GIMPO', merchant_name='CU 김포한강점', brand_name='CU',
    address='경기도 김포시 김포한강3로 40', latitude=37.6191000, longitude=126.7313000 WHERE merchant_id=5;
UPDATE merchants SET category_id=3, merchant_code='CGV-GIMPO', merchant_name='CGV 김포한강', brand_name='CGV',
    address='경기도 김포시 걸포로 6', latitude=37.6272000, longitude=126.7212000 WHERE merchant_id=6;
UPDATE merchants SET category_id=6, merchant_code='GOOD-HOSPITAL', merchant_name='김포 좋은병원', brand_name='좋은병원',
    address='경기도 김포시 돌문로 30', latitude=37.6158000, longitude=126.7238000 WHERE merchant_id=7;
UPDATE merchants SET category_id=7, merchant_code='HAPPY-FUEL', merchant_name='행복 주유소', brand_name='행복주유소',
    address='경기도 김포시 양촌로 77', latitude=37.6259000, longitude=126.7161000 WHERE merchant_id=8;

UPDATE card_monthly_status SET total_spending_amount=255000, is_benefit_eligible=TRUE, updated_at=NOW()
 WHERE user_card_id=1 AND target_year_month='202607';
UPDATE card_monthly_status SET total_spending_amount=180000, is_benefit_eligible=TRUE, updated_at=NOW()
 WHERE user_card_id=2 AND target_year_month='202607';
UPDATE card_monthly_status SET total_spending_amount=82000, is_benefit_eligible=TRUE, updated_at=NOW()
 WHERE user_card_id=3 AND target_year_month='202607';

INSERT INTO bookmarked_stores(user_id,merchant_id,created_at,is_deleted) VALUES
(1,4,'2026-07-22 09:00:00',FALSE),(1,3,'2026-07-22 09:01:00',FALSE)
ON DUPLICATE KEY UPDATE created_at=VALUES(created_at), is_deleted=FALSE;

DELETE FROM payments WHERE user_card_id IN (1,2,3) AND payment_time >= '2026-07-01 00:00:00';
INSERT INTO payments(user_card_id,merchant_id,payment_time,original_amount,discount_amount,reward_amount,final_amount,payment_status,benefit_description) VALUES
(1,1,'2026-07-20 14:30:00',6000,600,0,5400,'APPROVED','카페 10% 할인'),
(2,2,'2026-07-19 18:15:00',42800,0,2140,42800,'APPROVED','기본 포인트 적립'),
(1,3,'2026-07-15 10:10:00',12000,1200,0,10800,'APPROVED','카페 10% 할인');

-- 애플리케이션 데모 모드 실행 시 user_id=1의 PIN을 BCrypt(123456) 해시로 자동 갱신한다.
