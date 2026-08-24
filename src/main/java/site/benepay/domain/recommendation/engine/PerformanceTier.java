package site.benepay.domain.recommendation.engine;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * cards.benefits_info JSON의 performanceTiers[] 원소 하나.
 * CsvProcessing/benefits.py의 Tier dataclass를 그대로 옮긴 것.
 */
public record PerformanceTier(
	String benefitNodeId,
	String tierName,
	long minimumSpending,
	Long maximumSpending,
	Long maximumCombinedMonthlyBenefit,
	String monthlyLimitType,
	List<BenefitNode> benefits
) {

	private static final Set<String> NO_COMBINED_CAP = Set.of("SUM_OF_INDIVIDUAL_LIMITS", "UNLIMITED");

	/**
	 * 실제로 적용할 구간 통합한도. monthlyLimitType이 SUM_OF_INDIVIDUAL_LIMITS/UNLIMITED면
	 * 통합한도 자체를 안 쓴다(각 혜택 개별 한도만 적용).
	 */
	public Long combinedCap() {
		if (monthlyLimitType != null && NO_COMBINED_CAP.contains(monthlyLimitType)) {
			return null;
		}
		return maximumCombinedMonthlyBenefit;
	}

	/**
	 * 해외 가맹점 혜택(merchantScope: OVERSEAS)을 제외한, 추천 대상 혜택만.
	 */
	public List<BenefitNode> realBenefits() {
		return benefits.stream()
			.filter(b -> !b.isOverseas())
			.collect(Collectors.toList());
	}

	/**
	 * 이 구간 안에서 주어진 업종 코드를 커버하는 혜택들. 특정 매장을 판정하는 게 아니라
	 * 업종 단위로만 훑는 자리(예: 카테고리별 혜택 현황)에서 쓴다 - MERCHANT_BRAND처럼 매장이
	 * 한정된 혜택도 그대로 포함된다.
	 */
	public List<BenefitNode> benefitsForCategory(String categoryCode) {
		return benefitsForCategory(categoryCode, null);
	}

	/**
	 * 이 구간 안에서 주어진 업종 코드를 커버하면서 실제로 이 매장에 적용되는 혜택들.
	 * MERCHANT_BRAND처럼 특정 매장에만 한정된 혜택(예: "아웃백 10% 할인")은 categoryCodes
	 * 매칭만으로 걸러지지 않으므로, 특정 매장을 놓고 판정하는 자리(추천 목록, 결제)는 반드시
	 * 이 오버로드를 써야 한다. merchantName이 null이면 매장 제한 혜택도 통과한다(지갑 전체
	 * 기준 계산처럼 특정 매장이 없는 경우).
	 */
	public List<BenefitNode> benefitsForCategory(String categoryCode, String merchantName) {
		return realBenefits().stream()
			.filter(b -> b.coversCategory(categoryCode) && b.matchesMerchant(merchantName))
			.collect(Collectors.toList());
	}
}
