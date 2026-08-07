package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * CsvProcessing/test_boundary.py의 65개 경계값 테스트 중 모드 1(즉시 할인) 관련 항목만
 * 이식했다. 모드 2(실적 채우기)·모드 3(우선순위 비교) 전용 테스트는 그 기능을 포팅할 때
 * 같이 옮긴다.
 */
class BenefitEngineTest {

	private static final String CAFE = "5311";
	private static final String MOVIE = "7832";
	private static final String LEISURE = "7994";

	private static final RecommendationParams TEST_PARAMS = new RecommendationParams(
		Map.of(),
		new RecommendationParams.TicketHistogram(new double[0], Map.of()),
		new RecommendationParams.Constants(1700.0)
	);

	// ---------------------------------------------------------------- 헬퍼

	private static Mode1Result evaluateNow(List<PerformanceTier> tiers, long prevMonthSpend, long typicalAmount) {
		return evaluateNow(tiers, CAFE, prevMonthSpend, typicalAmount, Map.of());
	}

	private static Mode1Result evaluateNow(
		List<PerformanceTier> tiers, long prevMonthSpend, long typicalAmount, Map<String, BenefitUsage> usage
	) {
		return evaluateNow(tiers, CAFE, prevMonthSpend, typicalAmount, usage);
	}

	private static Mode1Result evaluateNow(
		List<PerformanceTier> tiers, String categoryCode, long prevMonthSpend, long typicalAmount,
		Map<String, BenefitUsage> usage
	) {
		return BenefitEngine.evaluateNow(tiers, prevMonthSpend, categoryCode, "카테고리", typicalAmount, usage, TEST_PARAMS);
	}

	private static Map<String, BenefitUsage> usageOf(String serviceName, long usedAmount) {
		return Map.of(serviceName, new BenefitUsage(usedAmount, 0, 0));
	}

	/** 정률 혜택 하나짜리 카드. serviceName은 항상 "카페"(한도 소진 추적 키). */
	private static BenefitNode rateBenefit(String categoryCode, double rate, Long cap, long minPayment) {
		return new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(categoryCode), "STATEMENT_DISCOUNT",
			rate, 0L, 0L, 0L, minPayment, null, null, cap, null, null, null, List.of(), false, null);
	}

	private static List<PerformanceTier> oneTierCard(long threshold, double rate, Long cap, long minPayment, Long totalLimit) {
		List<PerformanceTier> tiers = new ArrayList<>();
		if (threshold > 0) {
			tiers.add(new PerformanceTier(null, "0구간", 0, null, null, null, List.of()));
		}
		tiers.add(new PerformanceTier(null, "1구간", threshold, null, totalLimit, null,
			List.of(rateBenefit(CAFE, rate, cap, minPayment))));
		return tiers;
	}

	private static List<PerformanceTier> countedCard(Integer annual, Integer monthly) {
		BenefitNode b = new BenefitNode("정비 할인", "MERCHANT_BRAND", List.of(LEISURE), "CASHBACK_DISCOUNT",
			0, 20_000L, 0L, 0L, 0, null, null, null, null, monthly, annual, List.of(), false, null);
		return List.of(new PerformanceTier(null, null, 0, null, null, null, List.of(b)));
	}

	// ---------------------------------------------------------------- 구간 경계

	@Test
	void duplicateThresholdPicksTierWithBenefits() {
		List<PerformanceTier> tiers = List.of(
			new PerformanceTier(null, "빈 구간", 0, null, 0L, null, List.of()),
			new PerformanceTier(null, "혜택 구간", 0, null, null, null,
				List.of(rateBenefit(CAFE, 10, 5_000L, 0)))
		);
		PerformanceTier active = BenefitEngine.activeTier(tiers, 500_000);
		assertThat(active.tierName()).isEqualTo("혜택 구간");
	}

	@Test
	void tierBoundaryOpensExactlyAtThreshold() {
		List<PerformanceTier> tiers = oneTierCard(300_000, 10, null, 0, null);

		assertThat(evaluateNow(tiers, 300_000, 10_000).status()).isEqualTo(BenefitStatus.IMMEDIATE_DISCOUNT);
		assertThat(evaluateNow(tiers, 299_999, 10_000).status()).isEqualTo(BenefitStatus.PERFORMANCE_INSUFFICIENT);
	}

	// ---------------------------------------------------------------- 한도 경계

	@Test
	void capBoundaryClampsDiscountToRemainingLimit() {
		List<PerformanceTier> tiers = oneTierCard(0, 10, 1_000L, 0, null);

		assertThat(evaluateNow(tiers, 500_000, 10_000, usageOf("카페", 0)).discount()).isEqualTo(1_000);
		assertThat(evaluateNow(tiers, 500_000, 10_000, usageOf("카페", 500)).discount()).isEqualTo(500);

		Mode1Result empty = evaluateNow(tiers, 500_000, 10_000, usageOf("카페", 1_000));
		assertThat(empty.status()).isEqualTo(BenefitStatus.LIMIT_EXHAUSTED);
		assertThat(empty.discount()).isZero();

		Mode1Result over = evaluateNow(tiers, 500_000, 10_000, usageOf("카페", 1_500));
		assertThat(over.status()).isEqualTo(BenefitStatus.LIMIT_EXHAUSTED);
		assertThat(over.discount()).isZero();
	}

	@Test
	void totalLimitZeroBlocksAllDiscount() {
		List<PerformanceTier> tiers = oneTierCard(0, 10, 5_000L, 0, 0L);
		assertThat(evaluateNow(tiers, 500_000, 10_000).discount()).isZero();
	}

	// ---------------------------------------------------------------- 건당 최소결제

	@Test
	void minPaymentBoundary() {
		List<PerformanceTier> tiers = oneTierCard(0, 10, 10_000L, 10_000, null);

		assertThat(evaluateNow(tiers, 500_000, 10_000).status()).isEqualTo(BenefitStatus.IMMEDIATE_DISCOUNT);

		Mode1Result below = evaluateNow(tiers, 500_000, 9_999);
		assertThat(below.status()).isEqualTo(BenefitStatus.CONDITIONAL_DISCOUNT);
		assertThat(below.amount()).isEqualTo(10_000);
		assertThat(below.rate()).isCloseTo(0.10, within(1e-9));

		Mode1Result above = evaluateNow(tiers, 500_000, 50_000);
		assertThat(above.amount()).isEqualTo(50_000);
		assertThat(above.status()).isEqualTo(BenefitStatus.IMMEDIATE_DISCOUNT);
	}

	@Test
	void fixedRefundRateIsHighestAtMinimumPayment() {
		BenefitNode movie = new BenefitNode("영화 할인", "MERCHANT_BRAND", List.of(MOVIE), "CASHBACK_DISCOUNT",
			0, 4_000L, 0L, 0L, 15_000, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, null, 0, null, null, null, List.of(movie)));

		Mode1Result low = evaluateNow(tiers, MOVIE, 500_000, 5_000, Map.of());
		assertThat(low.rate()).isCloseTo(4_000.0 / 15_000, within(1e-4));

		Mode1Result high = evaluateNow(tiers, MOVIE, 500_000, 60_000, Map.of());
		assertThat(high.rate()).isCloseTo(4_000.0 / 60_000, within(1e-4));
	}

	@Test
	void monthlyCapLowersEffectiveRateAndFlagsCapped() {
		List<PerformanceTier> tiers = oneTierCard(0, 10, 10_000L, 0, null);

		Mode1Result capped = evaluateNow(tiers, 500_000, 10_000, usageOf("카페", 9_600));
		assertThat(capped.discount()).isEqualTo(400);
		assertThat(capped.rate()).isCloseTo(0.04, within(1e-9));
		assertThat(capped.nominalRate()).isCloseTo(0.10, within(1e-9));
		assertThat(capped.capped()).isTrue();

		assertThat(evaluateNow(tiers, 500_000, 10_000).capped()).isFalse();
	}

	@Test
	void limitExhaustionFlipsRanking() {
		List<PerformanceTier> highRateLowCap = oneTierCard(0, 50, 10_000L, 0, null);
		List<PerformanceTier> lowRateHighCap = oneTierCard(0, 10, 50_000L, 0, null);

		Mode1Result high = evaluateNow(highRateLowCap, 500_000, 10_000, usageOf("카페", 9_600));
		Mode1Result low = evaluateNow(lowRateHighCap, 500_000, 10_000);

		assertThat(low.rate()).isGreaterThan(high.rate());
		assertThat(high.nominalRate()).isGreaterThan(low.nominalRate());
	}

	@Test
	void perTransactionCapFlipsRanking() {
		BenefitNode bigRateSmallCap = new BenefitNode("영화 할인", "MERCHANT_BRAND", List.of(MOVIE), "CASHBACK_DISCOUNT",
			35, 0L, 0L, 0L, 0, 20_000L, 7_000L, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> bigTiers = List.of(new PerformanceTier(null, null, 0, null, null, null, List.of(bigRateSmallCap)));

		BenefitNode flatRate = new BenefitNode("영화관", "MERCHANT_CATEGORY", List.of(MOVIE), "STATEMENT_DISCOUNT",
			15, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> flatTiers = List.of(new PerformanceTier(null, null, 0, null, null, null, List.of(flatRate)));

		Mode1Result big = evaluateNow(bigTiers, MOVIE, 500_000, 63_490, Map.of());
		Mode1Result flat = evaluateNow(flatTiers, MOVIE, 500_000, 63_490, Map.of());

		assertThat(flat.rate()).isGreaterThan(big.rate());
		assertThat(big.rate()).isCloseTo(7_000.0 / 63_490, within(1e-4));
	}

	// ---------------------------------------------------------------- 횟수 한도

	@Test
	void annualCountLimitBlocksAfterExhausted() {
		List<PerformanceTier> tiers = countedCard(1, null);

		Mode1Result fresh = evaluateNow(tiers, LEISURE, 500_000, 50_000, Map.of());
		assertThat(fresh.status()).isEqualTo(BenefitStatus.IMMEDIATE_DISCOUNT);
		assertThat(fresh.note()).contains("1회 남음");

		Mode1Result spent = evaluateNow(tiers, LEISURE, 500_000, 50_000, Map.of("정비 할인", new BenefitUsage(0, 0, 1)));
		assertThat(spent.status()).isEqualTo(BenefitStatus.COUNT_EXHAUSTED);
		assertThat(spent.discount()).isZero();
	}

	@Test
	void monthlyCountLimitBlocksAfterExhausted() {
		List<PerformanceTier> tiers = countedCard(null, 2);

		Mode1Result oneUsed = evaluateNow(tiers, LEISURE, 500_000, 50_000, Map.of("정비 할인", new BenefitUsage(0, 1, 0)));
		assertThat(oneUsed.status()).isEqualTo(BenefitStatus.IMMEDIATE_DISCOUNT);

		Mode1Result twoUsed = evaluateNow(tiers, LEISURE, 500_000, 50_000, Map.of("정비 할인", new BenefitUsage(0, 2, 0)));
		assertThat(twoUsed.status()).isEqualTo(BenefitStatus.COUNT_EXHAUSTED);
	}

	@Test
	void tighterCountLimitWins() {
		List<PerformanceTier> tiers = countedCard(2, 5);
		Mode1Result r = evaluateNow(tiers, LEISURE, 500_000, 50_000, Map.of("정비 할인", new BenefitUsage(0, 0, 2)));
		assertThat(r.status()).isEqualTo(BenefitStatus.COUNT_EXHAUSTED);
	}

	// ---------------------------------------------------------------- 해외 가맹점

	@Test
	void overseasBenefitExcludedFromRealBenefits() {
		BenefitNode overseas = new BenefitNode("해외 가맹점 할인", "ALL_MERCHANTS", List.of(), "STATEMENT_DISCOUNT",
			2.0, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, "OVERSEAS");
		BenefitNode domestic = new BenefitNode("국내 가맹점 할인", "ALL_MERCHANTS", List.of(), "STATEMENT_DISCOUNT",
			1.0, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, "DOMESTIC");
		PerformanceTier tier = new PerformanceTier(null, "기본", 0, null, null, null, List.of(domestic, overseas));

		assertThat(tier.realBenefits()).extracting(BenefitNode::serviceName).containsExactly("국내 가맹점 할인");

		Mode1Result r = evaluateNow(List.of(tier), CAFE, 500_000, 10_000, Map.of());
		assertThat(r.rate()).isCloseTo(0.01, within(1e-9));
	}

	// ---------------------------------------------------------------- 할인율 vs 할인액

	@Test
	void higherRateWinsEvenWithSmallerAbsoluteDiscount() {
		List<PerformanceTier> lowRateBig = oneTierCard(0, 2, 100_000L, 0, null);
		List<PerformanceTier> highRateSmall = oneTierCard(0, 20, 100_000L, 3_000, null);

		Mode1Result low = evaluateNow(lowRateBig, 500_000, 10_000);
		Mode1Result high = evaluateNow(highRateSmall, 500_000, 10_000);

		assertThat(high.rate()).isGreaterThan(low.rate());
		assertThat(high.discount()).isGreaterThan(low.discount());
	}
}
