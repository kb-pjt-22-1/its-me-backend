package site.benepay.domain.benefit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * card_benefit_monthly_usage 집계 결과 read model. 카드 한 장이 혜택 하나(serviceName)에
 * 대해 이번 달(또는 지정한 기간) 소진한 할인액/횟수를 담는다.
 *
 * <p>월별 조회(findMonthlyUsageByUserCardId 등)에서는 usedAmount/usedCount 둘 다 채워지고,
 * 연간 횟수 조회(findAnnualCountByUserCardId)에서는 usedCount만 의미가 있다(annualCountLimit
 * 비교용 - 연 단위로는 할인액 한도가 없어 usedAmount는 안 씀).</p>
 */
@Getter
@Setter
public class BenefitUsageVO {

	private Long userCardId;
	private String benefitServiceName;
	private Long usedAmount;
	private Integer usedCount;
}
