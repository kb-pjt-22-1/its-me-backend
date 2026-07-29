-- 개발용 자동 로그인(POST /api/auth/dev-login) 대상 계정 dev1~dev10.
--
-- 이 파일은 docker-compose.yml이 마운트하는 docker/mysql/init 밖에 있어 컨테이너 기동 시
-- 자동으로 실행되지 않는다. dev-login.enabled(application.properties)와 마찬가지로, 이 계정도
-- 개발자가 명시적으로 적용할 때만 DB에 들어가야 한다는 원칙을 파일 배치로도 지킨 것이다.
--
-- 적용:
--   mysql -h127.0.0.1 -uroot -p<password> benepay < docker/mysql/dev-seed/dev-accounts.sql
-- 도커 컨테이너 안 MySQL에 적용하려면:
--   docker compose exec -T mysql mysql -uroot -p<password> benepay < docker/mysql/dev-seed/dev-accounts.sql
--
-- login_password_hash에 BCrypt 형식이 아닌 값을 넣어 두면 BCryptPasswordEncoder.matches()가
-- 항상 false를 돌려주므로, 이 계정들은 일반 /api/auth/login으로는 들어올 수 없고
-- dev-login 엔드포인트로만 닿는다. slot 수를 늘리려면 여기와
-- application.properties의 dev-login.account-count를 같이 맞춘다.
INSERT INTO users (login_id, login_password_hash, pin_hash, name, phone_number,
                   birth_date, role, di, ci_encrypted, created_at, is_deleted, fcm_token)
VALUES
 ('dev1',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자1',  NULL, '19900101', 'USER', 'dev_di_slot_1',  'dev_ci_slot_1',  NOW(), FALSE, NULL),
 ('dev2',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자2',  NULL, '19900102', 'USER', 'dev_di_slot_2',  'dev_ci_slot_2',  NOW(), FALSE, NULL),
 ('dev3',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자3',  NULL, '19900103', 'USER', 'dev_di_slot_3',  'dev_ci_slot_3',  NOW(), FALSE, NULL),
 ('dev4',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자4',  NULL, '19900104', 'USER', 'dev_di_slot_4',  'dev_ci_slot_4',  NOW(), FALSE, NULL),
 ('dev5',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자5',  NULL, '19900105', 'USER', 'dev_di_slot_5',  'dev_ci_slot_5',  NOW(), FALSE, NULL),
 ('dev6',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자6',  NULL, '19900106', 'USER', 'dev_di_slot_6',  'dev_ci_slot_6',  NOW(), FALSE, NULL),
 ('dev7',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자7',  NULL, '19900107', 'USER', 'dev_di_slot_7',  'dev_ci_slot_7',  NOW(), FALSE, NULL),
 ('dev8',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자8',  NULL, '19900108', 'USER', 'dev_di_slot_8',  'dev_ci_slot_8',  NOW(), FALSE, NULL),
 ('dev9',  '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자9',  NULL, '19900109', 'USER', 'dev_di_slot_9',  'dev_ci_slot_9',  NOW(), FALSE, NULL),
 ('dev10', '!DEV-ACCOUNT-NO-PASSWORD-LOGIN!', NULL, '개발자10', NULL, '19900110', 'USER', 'dev_di_slot_10', 'dev_ci_slot_10', NOW(), FALSE, NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);
