package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class BenefitJsonParserTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void parseReturnsEmptyListForNullOrBlankInput() {
		assertThat(BenefitJsonParser.parse(null, objectMapper)).isEmpty();
		assertThat(BenefitJsonParser.parse("", objectMapper)).isEmpty();
		assertThat(BenefitJsonParser.parse("   ", objectMapper)).isEmpty();
	}

	@Test
	void parseReturnsEmptyListWhenPerformanceTiersIsMissingOrNotAnArray() {
		assertThat(BenefitJsonParser.parse("{}", objectMapper)).isEmpty();
		assertThat(BenefitJsonParser.parse("{\"performanceTiers\":{}}", objectMapper)).isEmpty();
	}

	@Test
	void parseReturnsEmptyListForMalformedJson() {
		assertThat(BenefitJsonParser.parse("이건 json이 아니다 {", objectMapper)).isEmpty();
	}

	@Test
	void parseTreatsExplicitNullFieldsTheSameAsMissingFields() {
		String json = "{\"performanceTiers\":[{"
			+ "\"benefitNodeId\":null,\"tierName\":null,\"minimumSpending\":0,"
			+ "\"maximumSpending\":null,\"maximumCombinedMonthlyBenefit\":null,\"monthlyLimitType\":null,"
			+ "\"benefits\":[{\"serviceName\":\"카페\",\"benefitType\":\"MERCHANT_CATEGORY\","
			+ "\"categoryCodes\":[\"5311\"],\"discountMethod\":\"STATEMENT_DISCOUNT\",\"discountRate\":10,"
			+ "\"minimumPaymentAmount\":0,\"merchantScope\":null,"
			+ "\"maximumDiscountAmountPerTransaction\":null,\"monthlyCountLimit\":null}]"
			+ "}]}";

		List<PerformanceTier> tiers = BenefitJsonParser.parse(json, objectMapper);

		assertThat(tiers).hasSize(1);
		PerformanceTier tier = tiers.get(0);
		assertThat(tier.benefitNodeId()).isNull();
		assertThat(tier.tierName()).isNull();
		assertThat(tier.maximumSpending()).isNull();
		assertThat(tier.maximumCombinedMonthlyBenefit()).isNull();
		assertThat(tier.monthlyLimitType()).isNull();

		BenefitNode benefit = tier.benefits().get(0);
		assertThat(benefit.merchantScope()).isNull();
		assertThat(benefit.maximumDiscountPerTransaction()).isNull();
		assertThat(benefit.monthlyCountLimit()).isNull();
	}

	@Test
	void parseFallsBackToLegacyFieldNamesWhenPrimaryFieldsAreAbsent() {
		String json = "{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
			+ "{\"serviceName\":\"주유\",\"benefitType\":\"MERCHANT_CATEGORY\",\"categoryCodes\":[\"5541\"],"
			+ "\"discountMethod\":\"PER_LITER_STATEMENT_DISCOUNT\",\"discountAmountPerLiter\":45,"
			+ "\"maximumDiscountablePaymentAmount\":20000,\"maximumDiscountAmountPerMonth\":30000,"
			+ "\"maximumDiscountablePaymentAmountPerMonth\":100000}"
			+ "]}]}";

		BenefitNode benefit = BenefitJsonParser.parse(json, objectMapper).get(0).benefits().get(0);

		// weekday/weekendDiscountPerLiter 둘 다 없으니 discountAmountPerLiter(구 필드명)로 대체된다.
		assertThat(benefit.weekdayDiscountPerLiter()).isEqualTo(45L);
		assertThat(benefit.weekendDiscountPerLiter()).isEqualTo(45L);
		assertThat(benefit.maximumEligiblePerTransaction()).isEqualTo(20_000L);
		assertThat(benefit.monthlyDiscountLimit()).isEqualTo(30_000L);
		assertThat(benefit.monthlyEligibleLimit()).isEqualTo(100_000L);
	}

	@Test
	void parsePrefersThePrimaryFieldNameWhenBothAreProvided() {
		String json = "{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
			+ "{\"serviceName\":\"주유\",\"benefitType\":\"MERCHANT_CATEGORY\",\"categoryCodes\":[\"5541\"],"
			+ "\"discountMethod\":\"PER_LITER_STATEMENT_DISCOUNT\","
			+ "\"weekdayDiscountPerLiter\":50,\"weekendDiscountPerLiter\":60,\"discountAmountPerLiter\":999}"
			+ "]}]}";

		BenefitNode benefit = BenefitJsonParser.parse(json, objectMapper).get(0).benefits().get(0);

		assertThat(benefit.weekdayDiscountPerLiter()).isEqualTo(50L);
		assertThat(benefit.weekendDiscountPerLiter()).isEqualTo(60L);
	}

	@Test
	void parseFallsBackToRewardRateForPointAccumulationBenefitsWhenDiscountRateIsAbsent() {
		// 가온 올포인트 체크카드처럼 적립형(POINT_ACCUMULATION) 혜택은 원본 JSON에 discountRate가
		// 아니라 rewardRate로 저장돼 있다. 이걸 대체 필드로 읽지 않으면 discountRate가 0.0으로
		// 취급돼 BenefitEngine이 적립형 카드의 혜택 가치를 0으로 계산해 추천에서 항상 배제된다.
		String json = "{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
			+ "{\"serviceName\":\"전 가맹점 기본 적립\",\"benefitType\":\"ALL_MERCHANTS\",\"categoryCodes\":[],"
			+ "\"discountMethod\":\"POINT_ACCUMULATION\",\"rewardRate\":0.2}"
			+ "]}]}";

		BenefitNode benefit = BenefitJsonParser.parse(json, objectMapper).get(0).benefits().get(0);

		assertThat(benefit.discountRate()).isEqualTo(0.2);
	}

	@Test
	void parsePrefersDiscountRateOverRewardRateWhenBothArePresent() {
		String json = "{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
			+ "{\"serviceName\":\"카페\",\"benefitType\":\"MERCHANT_CATEGORY\",\"categoryCodes\":[\"5311\"],"
			+ "\"discountMethod\":\"STATEMENT_DISCOUNT\",\"discountRate\":10,\"rewardRate\":999}"
			+ "]}]}";

		BenefitNode benefit = BenefitJsonParser.parse(json, objectMapper).get(0).benefits().get(0);

		assertThat(benefit.discountRate()).isEqualTo(10.0);
	}
}
