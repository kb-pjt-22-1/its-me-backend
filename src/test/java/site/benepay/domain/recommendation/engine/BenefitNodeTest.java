package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BenefitNodeTest {

	private static BenefitNode node(
		String benefitType, List<String> categoryCodes, List<String> merchantNames, String merchantScope
	) {
		return new BenefitNode(
			"서비스", benefitType, categoryCodes, "STATEMENT_DISCOUNT", 10, 0L, 0L, 0L, 0,
			null, null, null, null, null, null, merchantNames, false, merchantScope
		);
	}

	@Test
	void isOverseasReflectsMerchantScope() {
		assertThat(node("MERCHANT_CATEGORY", List.of("5311"), List.of(), "OVERSEAS").isOverseas()).isTrue();
		assertThat(node("MERCHANT_CATEGORY", List.of("5311"), List.of(), "DOMESTIC").isOverseas()).isFalse();
		assertThat(node("MERCHANT_CATEGORY", List.of("5311"), List.of(), null).isOverseas()).isFalse();
	}

	@Test
	void allMerchantsWithNoCategoryCodesCoversEveryCategory() {
		BenefitNode allMerchants = node("ALL_MERCHANTS", List.of(), List.of(), null);

		assertThat(allMerchants.coversCategory("5311")).isTrue();
		assertThat(allMerchants.coversCategory("아무코드")).isTrue();
	}

	@Test
	void categoryBenefitOnlyCoversListedCodes() {
		BenefitNode cafeOnly = node("MERCHANT_CATEGORY", List.of("5311"), List.of(), null);

		assertThat(cafeOnly.coversCategory("5311")).isTrue();
		assertThat(cafeOnly.coversCategory("9999")).isFalse();
	}

	@Test
	void allMerchantsWithExplicitCodesStillFiltersByCode() {
		// benefitType이 ALL_MERCHANTS라도 categoryCodes가 비어있지 않으면 그 코드로만 매칭한다.
		BenefitNode limited = node("ALL_MERCHANTS", List.of("5311"), List.of(), null);

		assertThat(limited.coversCategory("5311")).isTrue();
		assertThat(limited.coversCategory("9999")).isFalse();
	}

	@Test
	void merchantNoteIsBlankWhenNotMerchantLimited() {
		BenefitNode unrestricted = node("MERCHANT_CATEGORY", List.of("5311"), List.of(), null);

		assertThat(unrestricted.isMerchantLimited()).isFalse();
		assertThat(unrestricted.merchantNote()).isEmpty();
	}

	@Test
	void merchantNoteJoinsMerchantNamesWhenLimited() {
		BenefitNode limited = node("MERCHANT_CATEGORY", List.of("5311"), List.of("스타벅스", "이디야"), null);

		assertThat(limited.isMerchantLimited()).isTrue();
		assertThat(limited.merchantNote()).isEqualTo("스타벅스·이디야 한정");
	}

	// ---- matchesMerchant ----

	@Test
	void matchesMerchantAlwaysTrueWhenNotMerchantLimited() {
		BenefitNode unrestricted = node("MERCHANT_CATEGORY", List.of("5812"), List.of(), null);

		assertThat(unrestricted.matchesMerchant("아무 음식점")).isTrue();
		assertThat(unrestricted.matchesMerchant(null)).isTrue();
	}

	@Test
	void matchesMerchantTrueWhenMerchantNameIsNullRegardlessOfRestriction() {
		// 매장이 특정되지 않은 호출(지갑 전체 기준 계산 등)이면 매장 한정 혜택도 걸러내지 않는다.
		BenefitNode outbackOnly = node("MERCHANT_BRAND", List.of("5812"), List.of("아웃백"), null);

		assertThat(outbackOnly.matchesMerchant(null)).isTrue();
	}

	@Test
	void matchesMerchantFalseForDifferentRestaurantInSameCategory() {
		// 실제 버그 재현: "직장인 보너스 체크카드"의 아웃백 10% 할인이 같은 5812(음식점)
		// 카테고리의 다른 매장까지 적용되면 안 된다.
		BenefitNode outbackOnly = node("MERCHANT_BRAND", List.of("5812"), List.of("아웃백"), null);

		assertThat(outbackOnly.matchesMerchant("아웃백")).isTrue();
		assertThat(outbackOnly.matchesMerchant("맥도날드")).isFalse();
		assertThat(outbackOnly.matchesMerchant("김밥천국")).isFalse();
	}

	@Test
	void matchesMerchantIgnoresSpacingAndCase() {
		BenefitNode outbackOnly = node("MERCHANT_BRAND", List.of("5812"), List.of("아웃백"), null);

		assertThat(outbackOnly.matchesMerchant("아웃백 강남점")).isTrue();
		assertThat(outbackOnly.matchesMerchant("아 웃 백")).isTrue();
	}

	@Test
	void matchesMerchantFalseWhenMerchantNameIsBlank() {
		BenefitNode outbackOnly = node("MERCHANT_BRAND", List.of("5812"), List.of("아웃백"), null);

		assertThat(outbackOnly.matchesMerchant("")).isFalse();
		assertThat(outbackOnly.matchesMerchant("   ")).isFalse();
	}
}
