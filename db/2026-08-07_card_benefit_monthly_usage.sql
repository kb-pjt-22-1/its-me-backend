-- 카드 추천(모드 1) 한도소진/횟수소진 판정을 위한 혜택 소진액 추적 스키마.
--
-- 이 저장소(its-me-backend)에는 마이그레이션 도구가 없다 - 실제 적용은 kb-pjt-22-1/its-me-infra
-- 레포에서 조율해야 한다. 이 파일은 그쪽에 전달할 DDL이다.
--
-- 배경: payments 테이블에 결제 1건이 어느 혜택을 적용받았는지 기록하는 컬럼이 없어서,
-- 혜택별 월/연 한도 소진액을 귀속 집계할 방법이 없다(CsvProcessing/INTEGRATION.md의
-- "혜택별 한도 소진액" 절 참고). card_monthly_status와 같은 "미리 집계해두는" 패턴을 따라
-- 집계 테이블로 간다 - 요청마다 payments를 스캔하지 않아도 된다.
--
-- 키는 performanceTiers[].benefitNodeId(구간 id)가 아니라 개별 혜택의 serviceName이다.
-- benefits_info JSON에서 구간 하나에 여러 혜택이 들어있고, 한도/횟수 제한은 혜택
-- 하나하나에 걸리기 때문이다(CsvProcessing/category_search.py의 CardState.used_discount가
-- 이미 service_name 단위로 추적함 - 참조 구현과 키를 맞췄다).
--
-- 쓰기 경로는 이번 단계 범위 밖이다: 결제 1건을 만들 때 "어느 혜택을 적용했는지" 정하는
-- 로직(결제 처리) 자체가 아직 이 코드베이스에 없다(PaymentMapper에 조회 메서드만 있고
-- insert/create가 없음). 그 기능이 생기면 결제 완료 시점에 이 테이블에 upsert하면 된다.
-- 지금은 RecommendationMapper.findYearlyBenefitUsage가 읽기만 하고, 행이 없으면(항상 없음)
-- 사용액 0으로 계산한다.

ALTER TABLE payments ADD COLUMN benefit_service_name VARCHAR(100) NULL
    COMMENT '이 결제에 적용된 혜택의 serviceName. 감사/재계산용 - 집계 테이블이 깨지면 여기서 다시 만들 수 있다.';

CREATE TABLE card_benefit_monthly_usage (
    card_benefit_monthly_usage_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_card_id                  BIGINT       NOT NULL,
    benefit_service_name          VARCHAR(100) NOT NULL COMMENT 'benefits_info JSON의 benefit.serviceName',
    target_year                   INT          NOT NULL COMMENT 'annualCountLimit 집계용 - target_year_month에서 매번 파싱하지 않도록 별도 컬럼으로 둠',
    target_year_month             CHAR(6)      NOT NULL,
    used_amount                   DECIMAL(12,0) NOT NULL DEFAULT 0,
    used_count                    INT          NOT NULL DEFAULT 0,
    updated_at                    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (card_benefit_monthly_usage_id),
    CONSTRAINT UQ_card_benefit_monthly_usage
        UNIQUE (user_card_id, benefit_service_name, target_year_month),
    CONSTRAINT FK_user_cards_TO_card_benefit_monthly_usage
        FOREIGN KEY (user_card_id) REFERENCES user_cards (user_card_id)
);

CREATE INDEX IDX_card_benefit_monthly_usage_year
    ON card_benefit_monthly_usage (user_card_id, target_year);
