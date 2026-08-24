package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PerformanceTierTest {

	@Test
	void combinedCapIsNullWhenMonthlyLimitTypeExcludesIt() {
		PerformanceTier sumOfIndividual =
			new PerformanceTier(null, "1구간", 0, null, 500L, "SUM_OF_INDIVIDUAL_LIMITS", List.of());
		PerformanceTier unlimited = new PerformanceTier(null, "1구간", 0, null, 500L, "UNLIMITED", List.of());

		assertThat(sumOfIndividual.combinedCap()).isNull();
		assertThat(unlimited.combinedCap()).isNull();
	}

	@Test
	void combinedCapReturnsTheConfiguredCapForOtherLimitTypes() {
		PerformanceTier integrated = new PerformanceTier(null, "1구간", 0, null, 500L, "INTEGRATED_LIMIT", List.of());
		PerformanceTier noType = new PerformanceTier(null, "1구간", 0, null, 500L, null, List.of());

		assertThat(integrated.combinedCap()).isEqualTo(500L);
		assertThat(noType.combinedCap()).isEqualTo(500L);
	}

	// ---- benefitsForCategory ----

	private static BenefitNode benefit(String benefitType, List<String> categoryCodes, List<String> merchantNames) {
		return new BenefitNode("서비스", benefitType, categoryCodes, "STATEMENT_DISCOUNT", 10, 0L, 0L, 0L, 0,
			null, null, null, null, null, null, merchantNames, false, null);
	}

	@Test
	void benefitsForCategoryWithoutMerchantNameIncludesMerchantLimitedBenefits() {
		// 1-arg 버전(merchantName 없음)은 매장 한정 혜택도 그대로 포함한다 - 특정 매장을
		// 판정하는 게 아니라 업종 단위로 훑는 자리(예: 카테고리별 혜택 현황)에서 쓰기 때문이다.
		BenefitNode outbackOnly = benefit("MERCHANT_BRAND", List.of("5812"), List.of("아웃백"));
		PerformanceTier tier = new PerformanceTier(null, "1구간", 0, null, null, null, List.of(outbackOnly));

		assertThat(tier.benefitsForCategory("5812")).containsExactly(outbackOnly);
	}

	@Test
	void benefitsForCategoryWithMerchantNameExcludesNonMatchingMerchantLimitedBenefits() {
		BenefitNode outbackOnly = benefit("MERCHANT_BRAND", List.of("5812"), List.of("아웃백"));
		PerformanceTier tier = new PerformanceTier(null, "1구간", 0, null, null, null, List.of(outbackOnly));

		assertThat(tier.benefitsForCategory("5812", "아웃백")).containsExactly(outbackOnly);
		assertThat(tier.benefitsForCategory("5812", "맥도날드")).isEmpty();
	}

	@Test
	void benefitsForCategoryWithMerchantNameStillIncludesUnrestrictedBenefits() {
		BenefitNode categoryWide = benefit("MERCHANT_CATEGORY", List.of("5812"), List.of());
		PerformanceTier tier = new PerformanceTier(null, "1구간", 0, null, null, null, List.of(categoryWide));

		assertThat(tier.benefitsForCategory("5812", "아무 식당")).containsExactly(categoryWide);
	}
}
