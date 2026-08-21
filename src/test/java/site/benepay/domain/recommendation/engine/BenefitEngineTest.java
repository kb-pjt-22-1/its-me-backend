package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * CsvProcessing/test_boundary.py의 65개 경계값 테스트 중 모드 3(우선순위 비교) 관련 항목을
 * 이식했다 - 모드 1(즉시 할인)·모드 2(실적 채우기)는 삭제됐다(모드 3 하나로 대체).
 */
class BenefitEngineTest {

	private static final String CAFE = "5311";
	private static final long TICKET = 10_000L;

	private static final RecommendationParams TEST_PARAMS = new RecommendationParams(
		Map.of(),
		new RecommendationParams.TicketHistogram(new double[0], Map.of()),
		Map.of(),
		testConstants()
	);

	// CsvProcessing/recommendation_params.json의 실제 값과 동일 - 테스트도 같은 상수로 검증한다.
	private static RecommendationParams.Constants testConstants() {
		return new RecommendationParams.Constants(1700.0, 0.35, 2.0, 0.25, 0.15, 0.05, 0.95, 0.2);
	}

	// ---------------------------------------------------------------- 헬퍼

	private static Mode3Result evaluatePriority(
		List<PerformanceTier> tiers, long currentMonthSpend, Map<String, Long> cardSpendHistory,
		Map<String, Long> walletSpendHistory, LocalDate today, double beta
	) {
		return evaluatePriority(tiers, currentMonthSpend, cardSpendHistory, walletSpendHistory, today, beta, Map.of());
	}

	private static Mode3Result evaluatePriority(
		List<PerformanceTier> tiers, long currentMonthSpend, Map<String, Long> cardSpendHistory,
		Map<String, Long> walletSpendHistory, LocalDate today, double beta,
		Map<String, BenefitUsage> usageByServiceName
	) {
		long prevMonthSpend = cardSpendHistory.isEmpty()
			? 0L
			: cardSpendHistory.get(java.util.Collections.max(cardSpendHistory.keySet()));
		return BenefitEngine.evaluatePriority(
			tiers, prevMonthSpend, currentMonthSpend, CAFE, null, "카페", TICKET,
			cardSpendHistory, walletSpendHistory, TEST_PARAMS, today, beta, usageByServiceName
		);
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

	private static List<PerformanceTier> twoTierCard(long secondThreshold, double rate1, double rate2) {
		return List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null, List.of(rateBenefit(CAFE, rate1, null, 0))),
			new PerformanceTier(null, "1구간", secondThreshold, null, null, null, List.of(rateBenefit(CAFE, rate2, null, 0)))
		);
	}

	// ---------------------------------------------------------------- 모드 3

	/**
	 * '혜택 높지만 실적 어려운 카드'(highHard) vs '혜택 낮지만 실적 쉬운 카드'(lowEasy).
	 * 둘 다 0원 구간이 곧 지금 받는 혜택이라 activeTier는 항상 이 구간이다. highHard는
	 * 다음 구간이 500만원 위(사실상 도달 불가), lowEasy는 5천원 위(거의 확실).
	 */
	private static List<PerformanceTier> highHardCard() {
		return twoTierCard(5_000_000, 10, 12);
	}

	private static List<PerformanceTier> lowEasyCard() {
		return twoTierCard(5_000, 2, 15);
	}

	// 두 gap 모두 못 넘는 값 -> P_이력 조건이 같아진다.
	private static final Map<String, Long> SHARED_CARD_HISTORY = Map.of("202512", 3_000L);
	private static final Map<String, Long> SHARED_WALLET_HISTORY = Map.of("202511", 600_000L, "202512", 600_000L);
	private static final LocalDate TODAY = LocalDate.of(2025, 12, 1);

	@Test
	void priorityNowFavorsHighRate() {
		Mode3Result a = evaluatePriority(highHardCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 1.0);
		Mode3Result b = evaluatePriority(lowEasyCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 1.0);

		assertThat(a.now()).isGreaterThan(b.now() * 3);
	}

	@Test
	void priorityRouteFavorsEasyGap() {
		Mode3Result a = evaluatePriority(highHardCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 1.0);
		Mode3Result b = evaluatePriority(lowEasyCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 1.0);

		assertThat(b.pRoute()).isGreaterThan(a.pRoute() * 5);
	}

	@Test
	void priorityTotalFlipsTheRanking() {
		// 당장의 할인만 보면 A(highHard)가 압도적인데도, 다음 달 기대치를 합치면 B(lowEasy)가 앞선다.
		Mode3Result a = evaluatePriority(highHardCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 1.0);
		Mode3Result b = evaluatePriority(lowEasyCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 1.0);

		assertThat(a.now()).isGreaterThan(b.now());
		assertThat(b.total()).isGreaterThan(a.total());
	}

	@Test
	void priorityBetaZeroReducesToNow() {
		// beta=0이면 미래 성분을 아예 안 보므로 now만으로 줄 세운 것과 같아야 한다.
		Mode3Result a = evaluatePriority(highHardCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 0.0);
		Mode3Result b = evaluatePriority(lowEasyCard(), 0, SHARED_CARD_HISTORY, SHARED_WALLET_HISTORY, TODAY, 0.0);

		assertThat(a.total()).isCloseTo(a.now(), within(1e-6));
		assertThat(b.total()).isCloseTo(b.now(), within(1e-6));
		assertThat(a.total()).isGreaterThan(b.total());
	}

	@Test
	void priorityMaxTierHasNoFuture() {
		// 이미 최고 구간이면 future=0, total=now.
		List<PerformanceTier> tiers = oneTierCard(0, 10, null, 0, null);

		Mode3Result r = evaluatePriority(
			tiers, 0, Map.of("202512", 500_000L), Map.of("202511", 500_000L, "202512", 500_000L), TODAY, 1.0
		);

		assertThat(r.future()).isZero();
		assertThat(r.total()).isCloseTo(r.now(), within(1e-6));
	}

	@Test
	void evaluatePriorityExcludesMerchantLimitedBenefitAtADifferentMerchant() {
		// recommendMerchants("오늘의 추천")가 재현하던 버그: 아웃백 한정 혜택이 다른 음식점을
		// 평가할 때도 now/gain에 반영되면 안 된다.
		String restaurant = "5812";
		BenefitNode outbackOnly = new BenefitNode("외식 할인", "MERCHANT_BRAND", List.of(restaurant),
			"CASHBACK_DISCOUNT", 10, 0L, 0L, 0L, 0, null, null, null, null, null, null,
			List.of("아웃백"), false, null);
		List<PerformanceTier> tiers =
			List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(outbackOnly)));

		Mode3Result atOutback = BenefitEngine.evaluatePriority(
			tiers, 0, 0, restaurant, "아웃백", "음식점", TICKET, Map.of(), Map.of(), TEST_PARAMS, TODAY, 1.0, Map.of()
		);
		Mode3Result atOtherRestaurant = BenefitEngine.evaluatePriority(
			tiers, 0, 0, restaurant, "맥도날드", "음식점", TICKET, Map.of(), Map.of(), TEST_PARAMS, TODAY, 1.0, Map.of()
		);

		assertThat(atOutback.now()).isGreaterThan(0L);
		assertThat(atOtherRestaurant.now()).isZero();
		assertThat(atOtherRestaurant.total()).isZero();
		assertThat(atOtherRestaurant.note()).contains("혜택 자체가 없음");
	}

	@Test
	void noBenefitAnywhereReturnsBlankResult() {
		List<PerformanceTier> tiers = List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null, List.of())
		);

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(r.now()).isZero();
		assertThat(r.total()).isZero();
		assertThat(r.note()).contains("혜택 자체가 없음");
	}

	@Test
	void gapZeroMeansFullCertainty() {
		BenefitEngine.FillProbability r =
			BenefitEngine.fillProbability(0, testConstants().defaultCv(), 10.0, 0, 12, 12, testConstants());
		assertThat(r.pFill()).isCloseTo(1.0, within(1e-9));
	}

	@Test
	void historyBoundary() {
		RecommendationParams.Constants c = testConstants();

		BenefitEngine.FillProbability none = BenefitEngine.fillProbability(0, c.defaultCv(), 10.0, 300_000, 0, 0, c);
		assertThat(none.pHist()).isCloseTo(c.historyPrior(), within(1e-9));

		BenefitEngine.FillProbability never = BenefitEngine.fillProbability(0, c.defaultCv(), 10.0, 300_000, 0, 12, c);
		assertThat(never.pHist()).isLessThan(c.historyPrior());

		BenefitEngine.FillProbability always = BenefitEngine.fillProbability(0, c.defaultCv(), 10.0, 300_000, 12, 12, c);
		assertThat(always.pHist()).isGreaterThan(c.historyPrior());

		BenefitEngine.FillProbability shortHistory =
			BenefitEngine.fillProbability(0, c.defaultCv(), 10.0, 300_000, 1, 1, c);
		assertThat(shortHistory.pHist()).isLessThan(always.pHist());
	}

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
	void overseasBenefitExcludedFromRealBenefits() {
		BenefitNode overseas = new BenefitNode("해외 가맹점 할인", "ALL_MERCHANTS", List.of(), "STATEMENT_DISCOUNT",
			2.0, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, "OVERSEAS");
		BenefitNode domestic = new BenefitNode("국내 가맹점 할인", "ALL_MERCHANTS", List.of(), "STATEMENT_DISCOUNT",
			1.0, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, "DOMESTIC");
		PerformanceTier tier = new PerformanceTier(null, "기본", 0, null, null, null, List.of(domestic, overseas));

		assertThat(tier.realBenefits()).extracting(BenefitNode::serviceName).containsExactly("국내 가맹점 할인");
	}

	// ---------------------------------------------------------------- activeTier 방어적 폴백

	@Test
	void activeTierFallsBackToFirstTierWhenNoTierQualifies() {
		// 첫 구간의 기준액이 0이 아닌 비정상적인 구간 목록이라도 방어적으로 첫 구간을 돌려준다.
		List<PerformanceTier> tiers = List.of(
			new PerformanceTier(null, "1구간", 100_000, null, null, null, List.of(rateBenefit(CAFE, 10, null, 0)))
		);

		PerformanceTier active = BenefitEngine.activeTier(tiers, 50_000);

		assertThat(active.tierName()).isEqualTo("1구간");
	}

	// ---------------------------------------------------------------- 리터당 할인

	@Test
	void effectiveRateUsesPerLiterDiscountForFuelBenefits() {
		BenefitNode fuelBenefit = new BenefitNode("주유 할인", "MERCHANT_CATEGORY", List.of("주유소코드"),
			"PER_LITER_STATEMENT_DISCOUNT", 0, 0L, 50L, 60L, 0, null, null, null, null, null, null,
			List.of(), false, null);

		assertThat(BenefitEngine.effectiveRate(fuelBenefit, false, 1700.0)).isCloseTo(50.0 / 1700.0, within(1e-9));
		assertThat(BenefitEngine.effectiveRate(fuelBenefit, true, 1700.0)).isCloseTo(60.0 / 1700.0, within(1e-9));
	}

	// ---------------------------------------------------------------- 지갑 여력(P_흐름)

	@Test
	void walletWithNoHistoryYieldsTheFloorFlowProbability() {
		List<PerformanceTier> tiers = twoTierCard(300_000, 5, 10);

		Mode3Result r = evaluatePriority(tiers, 0, Map.of("202512", 1_000L), Map.of(), TODAY, 1.0);

		assertThat(r.pFlow()).isCloseTo(testConstants().pFlowMin(), within(1e-9));
	}

	@Test
	void walletWithZeroMeanSpendUsesDefaultCv() {
		List<PerformanceTier> tiers = twoTierCard(300_000, 5, 10);
		Map<String, Long> zeroWallet = Map.of("202511", 0L, "202512", 0L);

		Mode3Result r = evaluatePriority(tiers, 0, Map.of("202512", 1_000L), zeroWallet, TODAY, 1.0);

		// 지출 이력은 2개월 있지만 평균이 0이라 cv는 여전히 defaultCv로 대체된다 - 흐름확률은 최저치.
		assertThat(r.pFlow()).isCloseTo(testConstants().pFlowMin(), within(1e-9));
	}

	@Test
	void remainingFactorAppliesWeekdayWeightsWhenConfigured() {
		RecommendationParams paramsWithWeights = new RecommendationParams(
			Map.of(),
			new RecommendationParams.TicketHistogram(new double[0], Map.of()),
			Map.of("카페", new double[] {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}),
			testConstants()
		);
		List<PerformanceTier> tiers = twoTierCard(300_000, 5, 10);
		Map<String, Long> cardHistory = Map.of("202512", 1_000L);
		Map<String, Long> walletHistory = Map.of("202511", 600_000L, "202512", 600_000L);

		Mode3Result withWeights = BenefitEngine.evaluatePriority(
			tiers, 1_000L, 0, CAFE, null, "카페", TICKET, cardHistory, walletHistory, paramsWithWeights, TODAY, 1.0,
			Map.of()
		);
		Mode3Result withoutWeights = evaluatePriority(tiers, 0, cardHistory, walletHistory, TODAY, 1.0);

		// 가중치가 전부 1.0이면 요일 가중 합이 남은 일수 그대로와 같아야 한다 - 가중 루프가 실행됐는지
		// 이 동등성으로 검증한다(가중이 실제로 다르게 적용됐다면 이 assert가 깨진다).
		assertThat(withWeights.pFlow()).isCloseTo(withoutWeights.pFlow(), within(1e-6));
	}

	// ---------------------------------------------------------------- 건당 최소결제 통과율

	@Test
	void qualifyingRatioAppliesPassRateWhenMinimumPaymentIsPositive() {
		RecommendationParams paramsWithHistogram = new RecommendationParams(
			Map.of(),
			new RecommendationParams.TicketHistogram(
				new double[] {1_000, 5_000, 10_000},
				Map.of("카페", new long[] {10L, 10L, 10L})
			),
			Map.of(),
			testConstants()
		);
		// 최소 결제 5,000원 - 히스토그램상 5,000원 이상 결제 비율은 20/30(66.7%).
		List<PerformanceTier> tiers = List.of(
			new PerformanceTier(null, "1구간", 0, null, null, null, List.of(rateBenefit(CAFE, 20, null, 5_000)))
		);

		Mode3Result r = BenefitEngine.evaluatePriority(
			tiers, 0, 0, CAFE, null, "카페", TICKET, Map.of(), Map.of(), paramsWithHistogram, TODAY, 1.0, Map.of()
		);

		// usable = 10,000 * (20/30) ≈ 6,667원, 할인 = 6,667 * 20% ≈ 1,333원.
		assertThat(r.now()).isBetween(1_300L, 1_400L);
	}

	// ---------------------------------------------------------------- 혜택 할인 추정치 한도

	@Test
	void benefitDiscountEstimateAppliesMonthlyEligibleLimitClamp() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			10, 0L, 0L, 0L, 0, null, null, null, 5_000L, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		// usable이 monthlyEligibleLimit(5,000원)으로 잘려 할인액도 그 기준으로 계산된다: 5,000 * 10%.
		assertThat(r.now()).isEqualTo(500L);
	}

	@Test
	void benefitDiscountEstimateFlatFeeUsesTighterOfMonthlyAndAnnualLimits() {
		// 정액 2,000원, 월 3회·연 24회(=월 환산 2회) - 더 빡빡한 연 환산 한도(2회)가 적용된다.
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "CASHBACK_DISCOUNT",
			0, 2_000L, 0L, 0L, 0, null, null, null, null, 3, 24, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		// count=1(usable/ticket), perMonth=24/12=2, limit=min(3,2)=2, uses=min(1,2)=1 -> 2,000원.
		assertThat(r.now()).isEqualTo(2_000L);
	}

	@Test
	void benefitDiscountEstimateFlatFeeWithNoCountLimitUsesFullContribution() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "CASHBACK_DISCOUNT",
			0, 2_000L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		// 횟수 한도가 전혀 없으면 uses=count(1) 그대로 - 2,000원.
		assertThat(r.now()).isEqualTo(2_000L);
	}

	@Test
	void benefitDiscountEstimateAppliesPerTransactionAndMonthlyDiscountCaps() {
		// 정률 50%면 5,000원이 나오지만 건당 상한 1,000원에 걸리고, 월 한도 1,500원은 이미 그 아래라 영향 없다.
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			50, 0L, 0L, 0L, 0, null, 1_000L, 1_500L, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(r.now()).isEqualTo(1_000L);
	}

	// ---------------------------------------------------------------- 구간별 할인 추정(tierDiscountForCategory)

	@Test
	void tierDiscountForCategoryReturnsZeroWhenActiveTierHasNoBenefitForCategory() {
		List<PerformanceTier> tiers = List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null, List.of()),
			new PerformanceTier(null, "1구간", 300_000, null, null, null, List.of(rateBenefit(CAFE, 10, null, 0)))
		);

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		// 활성 구간(0구간)엔 이 카테고리 혜택이 없어 now=0, 다음 구간(1구간)까지는 이득이 있다.
		assertThat(r.now()).isZero();
		assertThat(r.gap()).isEqualTo(300_000L);
		assertThat(r.gain()).isGreaterThan(0L);
	}

	@Test
	void tierDiscountForCategoryPicksTheHigherDiscountBenefitAmongMultiple() {
		// 같은 구간에 카페 혜택이 여럿이면 더 큰 할인을 주는 하나만 골라야 한다(중복 계상 방지).
		List<PerformanceTier> tiers = List.of(
			new PerformanceTier(null, "1구간", 0, null, null, null,
				List.of(rateBenefit(CAFE, 5, null, 0), rateBenefit(CAFE, 15, null, 0), rateBenefit(CAFE, 10, null, 0)))
		);

		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);

		// 10,000원 * 15% = 1,500원 (5%나 10%였다면 그보다 작았을 것).
		assertThat(r.now()).isEqualTo(1_500L);
	}

	@Test
	void tierDiscountForCategoryClampsToTheCombinedCapUnlessIntegratedLimitExcluded() {
		PerformanceTier cappedTier = new PerformanceTier(null, "1구간", 0, null, 500L, null,
			List.of(rateBenefit(CAFE, 50, null, 0)));
		Mode3Result capped = evaluatePriority(List.of(cappedTier), 0, Map.of(), Map.of(), TODAY, 1.0);
		assertThat(capped.now()).isEqualTo(500L);

		BenefitNode excludedBenefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			50, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), true, null);
		PerformanceTier excludedTier = new PerformanceTier(null, "1구간", 0, null, 500L, null, List.of(excludedBenefit));
		Mode3Result excluded = evaluatePriority(List.of(excludedTier), 0, Map.of(), Map.of(), TODAY, 1.0);

		// 10,000 * 50% = 5,000원 - integratedLimitExcluded=true라 구간 통합한도(500원)에 안 잘린다.
		assertThat(excluded.now()).isEqualTo(5_000L);
	}

	// ---------------------------------------------------------------- 한도 소진액 반영(usage)

	@Test
	void benefitDiscountEstimateSubtractsAlreadyUsedAmountFromMonthlyDiscountLimit() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			50, 0L, 0L, 0L, 0, null, null, 3_000L, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		// 정률 50%면 5,000원인데 monthlyDiscountLimit(3,000원)에 이미 2,000원을 써서 잔여 1,000원만 남았다.
		Map<String, BenefitUsage> usage = Map.of("카페", new BenefitUsage(2_000L, 0, 0));
		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0, usage);

		assertThat(r.now()).isEqualTo(1_000L);
	}

	@Test
	void benefitDiscountEstimateTreatsFullyConsumedMonthlyDiscountLimitAsZero() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			50, 0L, 0L, 0L, 0, null, null, 3_000L, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Map<String, BenefitUsage> usage = Map.of("카페", new BenefitUsage(3_000L, 0, 0));
		Mode3Result r = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0, usage);

		assertThat(r.now()).isZero();
	}

	@Test
	void benefitDiscountEstimateSubtractsAlreadyUsedCountFromMonthlyAndAnnualLimits() {
		// benefitDiscountEstimateFlatFeeUsesTighterOfMonthlyAndAnnualLimits와 같은 카드지만
		// 이번 달 이미 1회, 올해 이미 20회를 썼다고 가정한다.
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "CASHBACK_DISCOUNT",
			0, 2_000L, 0L, 0L, 0, null, null, null, null, 3, 24, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		// 사용량이 전혀 없으면 기존 테스트대로 2,000원(annualCountLimit/12=2가 더 빡빡함).
		Mode3Result withoutUsage = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0);
		assertThat(withoutUsage.now()).isEqualTo(2_000L);

		// 올해 이미 20회를 썼으면 남은 연간 한도는 4회 -> perMonth = 4/12 ≈ 0.33 -> uses=min(1,0.33)=0.33
		// -> discount = 2,000 * 0.33 ≈ 667원(반올림).
		Map<String, BenefitUsage> usage = Map.of("카페", new BenefitUsage(0L, 0, 20));
		Mode3Result withUsage = evaluatePriority(tiers, 0, Map.of(), Map.of(), TODAY, 1.0, usage);
		assertThat(withUsage.now()).isLessThan(withoutUsage.now());
	}

	// ---------------------------------------------------------------- selectPaymentBenefit(결제 시점 확정 계산)

	@Test
	void selectPaymentBenefitPicksTheHigherDiscountAmongCoveringBenefits() {
		BenefitNode lowRate = rateBenefit(CAFE, 5, null, 0);
		BenefitNode highRate = new BenefitNode("고율카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			15, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers =
			List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(lowRate, highRate)));

		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, Map.of());

		assertThat(application.serviceName()).isEqualTo("고율카페");
		assertThat(application.discountAmount()).isEqualTo(1_500L);
	}

	@Test
	void selectPaymentBenefitExcludesPointAccumulation() {
		BenefitNode point = new BenefitNode("적립카페", "MERCHANT_CATEGORY", List.of(CAFE), "POINT_ACCUMULATION",
			50, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(point)));

		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, Map.of());

		assertThat(application).isEqualTo(BenefitApplication.NONE);
	}

	@Test
	void selectPaymentBenefitExcludesPerLiterMethods() {
		BenefitNode perLiter = new BenefitNode("주유할인", "MERCHANT_CATEGORY", List.of(CAFE),
			"PER_LITER_STATEMENT_DISCOUNT", 0, 0L, 50L, 60L, 0, null, null, null, null, null, null,
			List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(perLiter)));

		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, Map.of());

		assertThat(application).isEqualTo(BenefitApplication.NONE);
	}

	@Test
	void selectPaymentBenefitExcludesBenefitsBelowMinimumPaymentAmount() {
		BenefitNode benefit = rateBenefit(CAFE, 50, null, 20_000L);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, Map.of());

		assertThat(application).isEqualTo(BenefitApplication.NONE);
	}

	@Test
	void selectPaymentBenefitReturnsZeroWhenMonthlyCountLimitAlreadyExhausted() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "CASHBACK_DISCOUNT",
			0, 1_000L, 0L, 0L, 0, null, null, null, null, 2, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Map<String, BenefitUsage> usage = Map.of("카페", new BenefitUsage(0L, 2, 0));
		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, usage);

		assertThat(application).isEqualTo(BenefitApplication.NONE);
	}

	@Test
	void selectPaymentBenefitCapsDiscountToRemainingMonthlyDiscountLimit() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			50, 0L, 0L, 0L, 0, null, null, 3_000L, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		// 정률 50%면 5,000원인데 월 한도 3,000원 중 이미 2,500원을 써서 잔여 500원만 남았다.
		Map<String, BenefitUsage> usage = Map.of("카페", new BenefitUsage(2_500L, 0, 0));
		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, usage);

		assertThat(application.serviceName()).isEqualTo("카페");
		assertThat(application.discountAmount()).isEqualTo(500L);
	}

	@Test
	void selectPaymentBenefitReturnsNoneWhenCategoryHasNoCoveringBenefit() {
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of()));

		BenefitApplication application = BenefitEngine.selectPaymentBenefit(tiers, 0, CAFE, null, 10_000L, Map.of());

		assertThat(application).isEqualTo(BenefitApplication.NONE);
	}

	@Test
	void selectPaymentBenefitExcludesMerchantLimitedBenefitAtADifferentMerchant() {
		// 실제 버그 재현: "직장인 보너스 체크카드"의 아웃백 10% 할인이 실제 결제 시점에
		// 아웃백이 아닌 다른 음식점(같은 5812 업종)에도 잘못 적용되던 문제.
		String restaurant = "5812";
		BenefitNode outbackOnly = new BenefitNode("외식 할인", "MERCHANT_BRAND", List.of(restaurant),
			"CASHBACK_DISCOUNT", 10, 0L, 0L, 0L, 0, null, null, null, null, null, null,
			List.of("아웃백"), false, null);
		List<PerformanceTier> tiers =
			List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(outbackOnly)));

		BenefitApplication atOutback =
			BenefitEngine.selectPaymentBenefit(tiers, 0, restaurant, "아웃백", 30_000L, Map.of());
		BenefitApplication atOtherRestaurant =
			BenefitEngine.selectPaymentBenefit(tiers, 0, restaurant, "맥도날드", 30_000L, Map.of());

		assertThat(atOutback.serviceName()).isEqualTo("외식 할인");
		assertThat(atOutback.discountAmount()).isEqualTo(3_000L);
		assertThat(atOtherRestaurant).isEqualTo(BenefitApplication.NONE);
	}

	// ---------------------------------------------------------------- shortDescription

	@Test
	void shortDescriptionForRateBasedBenefitWithoutCap() {
		List<PerformanceTier> tiers = oneTierCard(0, 10, null, 0, null);

		Mode3Result result = evaluatePriority(tiers, 0L, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(result.shortDescription()).isEqualTo("카페 10% 할인");
	}

	@Test
	void shortDescriptionIncludesCapWhenMaximumDiscountPerTransactionIsSet() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "STATEMENT_DISCOUNT",
			10, 0L, 0L, 0L, 0, null, 1_000L, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result result = evaluatePriority(tiers, 0L, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(result.shortDescription()).isEqualTo("카페 10% 할인 · 최대 1,000원");
	}

	@Test
	void shortDescriptionForFlatDiscountAmountBenefit() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "CASHBACK",
			0, 3_000L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result result = evaluatePriority(tiers, 0L, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(result.shortDescription()).isEqualTo("3,000원 할인");
	}

	@Test
	void shortDescriptionUsesPointLabelForPointAccumulation() {
		BenefitNode benefit = new BenefitNode("카페", "MERCHANT_CATEGORY", List.of(CAFE), "POINT_ACCUMULATION",
			5, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
		List<PerformanceTier> tiers = List.of(new PerformanceTier(null, "1구간", 0, null, null, null, List.of(benefit)));

		Mode3Result result = evaluatePriority(tiers, 0L, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(result.shortDescription()).isEqualTo("카페 5% 적립");
	}

	@Test
	void shortDescriptionIsNullWhenCategoryHasNoBenefitAtAll() {
		List<PerformanceTier> tiers = oneTierCard(0, 10, null, 0, null); // CAFE만 커버

		Mode3Result result = BenefitEngine.evaluatePriority(
			tiers, 0L, 0L, "9999", null, "다른카테고리", TICKET, Map.of(), Map.of(), TEST_PARAMS, TODAY, 1.0, Map.of()
		);

		assertThat(result.shortDescription()).isNull();
	}

	@Test
	void shortDescriptionFallsBackToNextTierBenefitWhenActiveTierDoesNotCoverCategory() {
		// 0구간은 카페 혜택이 없고, 1구간(다음 구간)만 카페를 커버한다 - 실제로 홈 화면에서
		// now=0인데도 카드의 일반적인 혜택 문구는 계속 보여야 하는 상황을 재현한다.
		PerformanceTier tier0 = new PerformanceTier(null, "0구간", 0, null, null, null, List.of());
		PerformanceTier tier1 = new PerformanceTier(null, "1구간", 500_000, null, null, null,
			List.of(rateBenefit(CAFE, 10, null, 0)));
		List<PerformanceTier> tiers = List.of(tier0, tier1);

		Mode3Result result = evaluatePriority(tiers, 0L, Map.of(), Map.of(), TODAY, 1.0);

		assertThat(result.now()).isZero();
		assertThat(result.shortDescription()).isEqualTo("카페 10% 할인");
	}
}
