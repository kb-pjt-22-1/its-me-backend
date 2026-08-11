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
		long prevMonthSpend = cardSpendHistory.isEmpty()
			? 0L
			: cardSpendHistory.get(java.util.Collections.max(cardSpendHistory.keySet()));
		return BenefitEngine.evaluatePriority(
			tiers, prevMonthSpend, currentMonthSpend, CAFE, "카페", TICKET,
			cardSpendHistory, walletSpendHistory, TEST_PARAMS, today, beta
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
}
