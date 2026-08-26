package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * beta=0(이번 달 확정 이득만 비교)과 beta=1(다음 달 기대 이득까지 반영)이 같은 매장·같은
 * 결제액·같은 보유 카드 상태에서 서로 다른 카드를 추천할 때, 그 선택 차이가 실제 두 달치
 * 실수령 할인액에 얼마나 영향을 주는지 검증한다.
 *
 * <p>evaluatePriority()는 "now"(ticket 기준 추정치)로 랭킹을 매기지만, 실제로 통장에 찍히는
 * 할인액은 그 카드의 그 달 실제 지출 총액 × activeTier가 고른 구간의 혜택률이다 - 그래서 이
 * 테스트는 랭킹(추천)과 실현손익(realized)을 분리해서 계산한다: evaluatePriority로 "어느
 * 카드가 뽑히는가"만 판단하고, 실제 2개월 손익은 activeTier + 카드별 실제 월 지출로 직접
 * 계산한다.</p>
 */
class BetaImpactAnalysisTest {

	private static final String CATEGORY_CODE = "5311";
	private static final String CATEGORY_NAME = "카페";
	// 매달 이 매장에서 실제로 결제하는 금액 - "같은 매장, 같은 금액"을 그대로 반영한다.
	// evaluatePriority의 typicalAmount(랭킹용 추정 기준)로도, 실현손익 계산의 "그 달 실제
	// 지출 총액"으로도 동일하게 쓴다.
	private static final long MONTHLY_SPEND = 120_000L;
	// 이번 달 초반이라 남은 기간이 넉넉하다(다음 구간 도달 확률 P_흐름이 유의미하게 계산되려면
	// 필요) - 월말이면 어떤 카드든 미래 가치가 0에 수렴한다.
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

	private static RecommendationParams.Constants constants() {
		// CsvProcessing/recommendation_params.json 실제 값과 동일(BenefitEngineTest와 동일 상수).
		return new RecommendationParams.Constants(1700.0, 0.35, 2.0, 0.25, 0.15, 0.05, 0.95, 0.2);
	}

	private static RecommendationParams params() {
		return new RecommendationParams(
			Map.of(), new RecommendationParams.TicketHistogram(new double[0], Map.of()), Map.of(), constants());
	}

	private static BenefitNode rateBenefit(double rate) {
		return new BenefitNode("혜택", "MERCHANT_CATEGORY", List.of(CATEGORY_CODE), "STATEMENT_DISCOUNT",
			rate, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
	}

	/**
	 * 즉시혜택카드: 지금 당장은 후하지만(8%), 다음 구간(10%)은 300만원이 필요해 사실상
	 * 이번 달 안에 도달 불가능하다 - "미래 가치가 없는" 카드의 대조군.
	 */
	private static List<PerformanceTier> immediateBenefitCard() {
		return List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null, List.of(rateBenefit(8))),
			new PerformanceTier(null, "1구간", 3_000_000, null, null, null, List.of(rateBenefit(10)))
		);
	}

	/**
	 * 실적유도카드: 지금은 박하지만(1%), 다음 구간(20%)은 10만원이라 이번 결제(12만원)를
	 * 여기 몰아주면 이번 달 실적만으로 바로 넘는다 - "당장은 손해, 다음 달부터 큰 이득"인 카드.
	 */
	private static List<PerformanceTier> performanceInducingCard() {
		return List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null, List.of(rateBenefit(1))),
			new PerformanceTier(null, "1구간", 100_000, null, null, null, List.of(rateBenefit(20)))
		);
	}

	// 지갑 전체로 월 15만원 안팎을 이 카테고리에 쓰는 습관 - performanceInducingCard의 10만원
	// 문턱을 이번 결제 이후 남은 기간 동안 채울 확률(P_흐름)이 높게 잡히도록 뒷받침한다.
	private static final Map<String, Long> WALLET_HISTORY = Map.of("202506", 150_000L, "202507", 160_000L);
	// performanceInducingCard 자체도 과거 두 달 다 10만원 문턱을 넘겨왔다(P_이력 근거).
	private static final Map<String, Long> INDUCING_CARD_HISTORY = Map.of("202506", 110_000L, "202507", 130_000L);

	private static Mode3Result evaluate(
		List<PerformanceTier> tiers, long prevMonthSpend, Map<String, Long> cardHistory, double beta
	) {
		// currentMonthSpend=0: 이번 달 시작 시점, 아직 이번 결제를 어느 카드에 넣을지 정하기
		// 전이라는 의사결정 시점을 나타낸다(gap은 nextTier 문턱 전체를 기준으로 계산됨).
		return BenefitEngine.evaluatePriority(tiers, prevMonthSpend, 0L, CATEGORY_CODE, null, CATEGORY_NAME,
			MONTHLY_SPEND, cardHistory, WALLET_HISTORY, params(), TODAY, beta, Map.of());
	}

	/** activeTier가 고른 구간의 이 카테고리 혜택률(%) - 카드에 카테고리 혜택이 정확히 하나뿐이라는 전제. */
	private static double activeRate(List<PerformanceTier> tiers, long prevMonthSpend) {
		PerformanceTier active = BenefitEngine.activeTier(tiers, prevMonthSpend);
		return active.benefitsForCategory(CATEGORY_CODE).get(0).discountRate();
	}

	/**
	 * 이 카드에 두 달 연속 MONTHLY_SPEND를 몰아줬을 때 실제로 받는 할인액 총합. 1개월차는
	 * prevMonthSpend(들어올 때의 실적)로 activeTier가 정해지고, 2개월차는 1개월차의 실제 지출
	 * (MONTHLY_SPEND)이 전월 실적이 되어 activeTier를 다시 정한다.
	 */
	private static long realizedTwoMonthDiscount(List<PerformanceTier> tiers, long incomingPrevMonthSpend) {
		long month1 = Math.round(MONTHLY_SPEND * activeRate(tiers, incomingPrevMonthSpend) / 100.0);
		long month2 = Math.round(MONTHLY_SPEND * activeRate(tiers, MONTHLY_SPEND) / 100.0);
		return month1 + month2;
	}

	@Test
	void betaZeroAndBetaOnePickDifferentCardsForTheIdenticalSpendingPattern() {
		List<PerformanceTier> immediate = immediateBenefitCard();
		List<PerformanceTier> inducing = performanceInducingCard();

		Mode3Result immediateBeta0 = evaluate(immediate, 0L, Map.of(), 0.0);
		Mode3Result inducingBeta0 = evaluate(inducing, 0L, INDUCING_CARD_HISTORY, 0.0);
		Mode3Result immediateBeta1 = evaluate(immediate, 0L, Map.of(), 1.0);
		Mode3Result inducingBeta1 = evaluate(inducing, 0L, INDUCING_CARD_HISTORY, 1.0);

		System.out.printf(
			"[beta=0] 즉시혜택카드 now=%d future=%.0f total=%.0f | 실적유도카드 now=%d future=%.0f total=%.0f%n",
			immediateBeta0.now(), immediateBeta0.future(), immediateBeta0.total(),
			inducingBeta0.now(), inducingBeta0.future(), inducingBeta0.total());
		System.out.printf(
			"[beta=1] 즉시혜택카드 now=%d future=%.0f total=%.0f | 실적유도카드 now=%d future=%.0f total=%.0f "
				+ "(P_흐름=%.3f P_이력=%.3f P_fill=%.3f)%n",
			immediateBeta1.now(), immediateBeta1.future(), immediateBeta1.total(),
			inducingBeta1.now(), inducingBeta1.future(), inducingBeta1.total(),
			inducingBeta1.pFlow(), inducingBeta1.pHist(), inducingBeta1.pRoute());

		// beta=0(이번 달 확정 이득만 비교)일 때는 즉시혜택카드가 이긴다 - 8% > 1%.
		assertThat(immediateBeta0.total()).isGreaterThan(inducingBeta0.total());
		// beta=1(다음 달 기대 이득까지 반영)일 때는 실적유도카드가 역전한다.
		assertThat(inducingBeta1.total()).isGreaterThan(immediateBeta1.total());

		// ---- 실제 2개월 실수령 할인액(activeTier + 실제 지출 기준, ticket 추정치 아님) ----
		long realizedIfFollowBeta0 = realizedTwoMonthDiscount(immediate, 0L); // beta=0이 고른 즉시혜택카드
		long realizedIfFollowBeta1 = realizedTwoMonthDiscount(inducing, 0L); // beta=1이 고른 실적유도카드
		long difference = realizedIfFollowBeta1 - realizedIfFollowBeta0;

		System.out.printf(
			"실현손익 - beta=0 추천(즉시혜택카드) 2개월 할인액=%d원, beta=1 추천(실적유도카드) 2개월 할인액=%d원, 차이=%+d원%n",
			realizedIfFollowBeta0, realizedIfFollowBeta1, difference);

		// beta=1의 "미래를 내다본" 추천이 실제로 2개월 누적 기준 더 많은 할인을 받아온다.
		assertThat(realizedIfFollowBeta1).isGreaterThan(realizedIfFollowBeta0);
		assertThat(difference).isEqualTo(6_000L);
	}

	// ==================================================================== 통계적 유의성

	/**
	 * 위 시나리오의 뼈대(즉시혜택 카드 vs 실적유도 카드 구조)를 유지한 채 문턱/혜택률/지갑
	 * 규모를 무작위로 흔들어 60개 시나리오를 만들고, beta=0/beta=1 추천이 실제로 갈리는
	 * 경우만 모아 "beta=1 추천의 2개월 실수령액이 beta=0 추천보다 많다"는 가설을 대응표본
	 * t-검정(H0: 평균 차이=0)으로 검증한다. 표본이 충분히 크면(n≥30) t-분포가 정규분포에
	 * 근사하므로, 정규근사 임계값(|t|>1.96, 양측 5%)으로 판단한다.
	 */
	@Test
	void betaOneRecommendationsSignificantlyOutperformBetaZeroAcrossRandomizedScenarios() {
		Random random = new Random(42); // 재현 가능하도록 고정 시드
		List<Long> differences = new ArrayList<>();

		for (int i = 0; i < 60; i++) {
			// 즉시혜택카드: 지금 6~12%, 다음 구간은 항상 사실상 도달 불가(300만원 고정).
			double immediateNowRate = 6 + random.nextDouble() * 6;
			// 실적유도카드: 지금 0.5~2%, 다음 구간 15~25%, 문턱 80,000~120,000원.
			double inducingNowRate = 0.5 + random.nextDouble() * 1.5;
			double inducingNextRate = 15 + random.nextDouble() * 10;
			long inducingThreshold = 80_000 + random.nextInt(40_001);
			long walletMonthly = 130_000 + random.nextInt(70_001);

			List<PerformanceTier> immediate = List.of(
				new PerformanceTier(null, "0구간", 0, null, null, null, List.of(rateBenefit(immediateNowRate))),
				new PerformanceTier(null, "1구간", 3_000_000, null, null, null, List.of(rateBenefit(12)))
			);
			List<PerformanceTier> inducing = List.of(
				new PerformanceTier(null, "0구간", 0, null, null, null, List.of(rateBenefit(inducingNowRate))),
				new PerformanceTier(null, "1구간", inducingThreshold, null, null, null,
					List.of(rateBenefit(inducingNextRate)))
			);
			Map<String, Long> wallet = Map.of("202506", walletMonthly, "202507", walletMonthly);
			Map<String, Long> inducingHistory = Map.of("202506", inducingThreshold + 10_000,
				"202507", inducingThreshold + 20_000);

			Mode3Result immediateBeta0 = evaluateWith(immediate, wallet, Map.of(), 0.0);
			Mode3Result inducingBeta0 = evaluateWith(inducing, wallet, inducingHistory, 0.0);
			Mode3Result immediateBeta1 = evaluateWith(immediate, wallet, Map.of(), 1.0);
			Mode3Result inducingBeta1 = evaluateWith(inducing, wallet, inducingHistory, 1.0);

			boolean beta0PicksImmediate = immediateBeta0.total() >= inducingBeta0.total();
			boolean beta1PicksInducing = inducingBeta1.total() > immediateBeta1.total();
			// 정확히 원하는 시나리오(즉시혜택카드는 beta=0이, 실적유도카드는 beta=1이 고른 경우)만 표본에 넣는다.
			if (!beta0PicksImmediate || !beta1PicksInducing) {
				continue;
			}

			long realizedBeta0 = realizedTwoMonthDiscount(immediate, 0L, walletMonthly);
			long realizedBeta1 = realizedTwoMonthDiscount(inducing, 0L, walletMonthly);
			differences.add(realizedBeta1 - realizedBeta0);
		}

		assertThat(differences).as("beta=0/beta=1 추천이 실제로 갈리는 시나리오가 충분히 나와야 검정이 성립한다")
			.hasSizeGreaterThanOrEqualTo(20);

		double mean = differences.stream().mapToLong(Long::longValue).average().orElseThrow();
		double variance = differences.stream()
			.mapToDouble(d -> Math.pow(d - mean, 2))
			.sum() / (differences.size() - 1);
		double standardError = Math.sqrt(variance / differences.size());
		double tStatistic = mean / standardError;

		System.out.printf(
			"표본 수=%d, 평균 차이=%.1f원, 표준오차=%.1f, t=%.2f (|t|>1.96이면 양측 5%% 유의)%n",
			differences.size(), mean, standardError, tStatistic);

		// 평균 차이가 0보다 유의하게 크다(beta=1 추천이 더 많이 번다) - 정규근사 양측 5% 임계값.
		assertThat(tStatistic).isGreaterThan(1.96);
		assertThat(mean).isPositive();
	}

	private static Mode3Result evaluateWith(
		List<PerformanceTier> tiers, Map<String, Long> wallet, Map<String, Long> cardHistory, double beta
	) {
		return BenefitEngine.evaluatePriority(tiers, 0L, 0L, CATEGORY_CODE, null, CATEGORY_NAME, MONTHLY_SPEND,
			cardHistory, wallet, params(), TODAY, beta, Map.of());
	}

	private static long realizedTwoMonthDiscount(List<PerformanceTier> tiers, long incomingPrevMonthSpend,
		long monthlySpend) {
		long month1 = Math.round(monthlySpend * activeRate(tiers, incomingPrevMonthSpend) / 100.0);
		long month2 = Math.round(monthlySpend * activeRate(tiers, monthlySpend) / 100.0);
		return month1 + month2;
	}
}
