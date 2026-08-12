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
	 * 이 구간 안에서 주어진 업종 코드를 커버하는 혜택들.
	 */
	public List<BenefitNode> benefitsForCategory(String categoryCode) {
		return realBenefits().stream()
			.filter(b -> b.coversCategory(categoryCode))
			.collect(Collectors.toList());
	}
}
