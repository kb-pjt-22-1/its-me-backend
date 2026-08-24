package site.benepay.domain.recommendation.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 사용자가 보유한 카드 전체의 card_benefit_monthly_usage 집계 결과 read model(카드+혜택
 * 단위). 매장 추천 엔진(BenefitEngine)이 monthlyDiscountLimit/monthlyCountLimit/
 * annualCountLimit을 "이미 소진한 만큼 뺀 잔여 한도"로 계산하는 데 쓴다.
 *
 * <p>월별 조회에서는 usedAmount/usedCount 둘 다 채워지고, 연간 횟수 조회에서는 usedCount만
 * 의미가 있다(BenefitUsageVO와 동일한 규약).</p>
 */
@Getter
@Setter
public class RecommendationBenefitUsageVO {

	private Long userCardId;
	private String benefitServiceName;
	private Long usedAmount;
	private Integer usedCount;
}
