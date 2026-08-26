package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 카페/편의점/마트/주유 등 여러 카테고리를 오가는 "정해진 결제 순서"를 2개월치 그대로
 * 재생하면서, 매 결제마다 beta=0/beta=1이 각각 어느 카드를 추천했을지와 그 결제의 실제
 * 할인액을 누적한다 - {@link BetaImpactAnalysisTest}가 카테고리 하나·거래 하나로 증명한
 * 것을, 실제 지갑처럼 여러 카테고리·여러 거래가 섞인 시퀀스로 확장한 버전이다.
 *
 * <p>카드의 구간(activeTier)은 카테고리와 무관하게 "그 카드의 그 달 총 사용액"으로
 * 정해진다(README "3. 구간 선택") - categoryCode는 그 구간 안에서 어느 혜택 노드가
 * 적용되는지만 결정한다. 그래서 이 테스트는 카드별로 prevMonthSpend/currentMonthSpend
 * (총액, 카테고리 무관)를 추적하고, evaluatePriority는 추천 판단에만, selectPaymentBenefit은
 * 그 결제 한 건의 실제 확정 할인 계산에만 쓴다(PaymentTokenServiceImpl과 동일한 역할 분리).</p>
 */
class BetaImpactSequentialPaymentsTest {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

	private record Payment(LocalDate date, String categoryCode, String categoryName, String merchantName,
		long amount) {
	}

	/** 카드 한 장의 진행 중인 상태 - 시뮬레이션 동안 policy(beta)별로 독립적으로 진행된다. */
	private static final class CardState {
		final String cardName;
		final List<PerformanceTier> tiers;
		long prevMonthSpend;
		long currentMonthSpend;
		final Map<String, Long> completedMonthHistory = new HashMap<>();

		CardState(String cardName, List<PerformanceTier> tiers) {
			this.cardName = cardName;
			this.tiers = tiers;
		}
	}

	private static RecommendationParams.Constants constants() {
		return new RecommendationParams.Constants(1700.0, 0.35, 2.0, 0.25, 0.15, 0.05, 0.95, 0.2);
	}

	private static final RecommendationParams PARAMS = new RecommendationParams(
		Map.of(), new RecommendationParams.TicketHistogram(new double[0], Map.of()), Map.of(), constants());

	private static BenefitNode rateBenefit(String categoryCode, double rate) {
		return new BenefitNode("혜택", "MERCHANT_CATEGORY", List.of(categoryCode), "STATEMENT_DISCOUNT",
			rate, 0L, 0L, 0L, 0, null, null, null, null, null, null, List.of(), false, null);
	}

	private static final String CAFE = "5311";
	private static final String CONVENIENCE = "5412";
	private static final String GAS = "5541";
	private static final String MART = "5211";
	private static final Map<String, String> CATEGORY_NAMES =
		Map.of(CAFE, "카페", CONVENIENCE, "편의점", GAS, "주유", MART, "마트");

	/** 즉시형: 지금 당장 후하지만(카테고리별 7/5/3/2%), 다음 구간(300만원)은 사실상 불가능. */
	private static List<PerformanceTier> immediateCard() {
		return List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null,
				List.of(rateBenefit(CAFE, 7), rateBenefit(CONVENIENCE, 5), rateBenefit(GAS, 3),
					rateBenefit(MART, 2))),
			new PerformanceTier(null, "1구간", 3_000_000, null, null, null,
				List.of(rateBenefit(CAFE, 9), rateBenefit(CONVENIENCE, 6), rateBenefit(GAS, 4),
					rateBenefit(MART, 3)))
		);
	}

	/** 실적유도형: 지금은 박하지만(전 카테고리 1%), 다음 구간(10만원)은 한 달 지출을 몰아주면 도달. */
	private static List<PerformanceTier> inducingCard() {
		return List.of(
			new PerformanceTier(null, "0구간", 0, null, null, null,
				List.of(rateBenefit(CAFE, 1), rateBenefit(CONVENIENCE, 1), rateBenefit(GAS, 1),
					rateBenefit(MART, 1))),
			new PerformanceTier(null, "1구간", 100_000, null, null, null,
				List.of(rateBenefit(CAFE, 15), rateBenefit(CONVENIENCE, 12), rateBenefit(GAS, 10),
					rateBenefit(MART, 8)))
		);
	}

	/** 2개월치, 4개 카테고리를 오가는 고정 결제 순서. */
	private static List<Payment> paymentSequence() {
		List<Payment> payments = new ArrayList<>();
		// ---- 1개월차 (2026-08) ----
		payments.add(new Payment(LocalDate.of(2026, 8, 2), CAFE, "카페", "스타벅스 강남점", 12_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 6), CONVENIENCE, "편의점", "GS25 역삼점", 8_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 10), MART, "마트", "이마트 강남점", 45_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 15), GAS, "주유", "SK주유소 서초점", 60_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 20), CAFE, "카페", "스타벅스 강남점", 12_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 25), CONVENIENCE, "편의점", "CU 도곡점", 10_000));
		// ---- 2개월차 (2026-09) ----
		payments.add(new Payment(LocalDate.of(2026, 9, 3), MART, "마트", "이마트 강남점", 45_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 8), GAS, "주유", "SK주유소 서초점", 60_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 15), CAFE, "카페", "스타벅스 강남점", 12_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 22), CONVENIENCE, "편의점", "GS25 역삼점", 8_000));
		return payments;
	}

	/**
	 * 결제 순서 하나를 beta 하나로 끝까지 재생한다. 매 결제마다 evaluatePriority로 두 카드를
	 * 비교해 이긴 카드에 실제로 결제를 태우고(selectPaymentBenefit으로 실제 확정 할인 계산),
	 * 그 카드의 이번 달 누적 사용액을 늘린다. 월이 바뀌면 모든 카드의 이번 달 누적을 전월
	 * 실적으로 넘기고 이력에 기록한다.
	 */
	private static SimulationResult simulate(List<Payment> payments, double beta) {
		CardState immediate = new CardState("즉시형카드", immediateCard());
		CardState inducing = new CardState("실적유도카드", inducingCard());
		List<CardState> cards = List.of(immediate, inducing);

		Map<String, Long> walletHistory = new HashMap<>();
		// 시뮬레이션 시작 전(6~7월)에도 지갑 전체로 월 15만원 안팎을 써온 이력이 있다고 가정한다 -
		// 이게 없으면 8월 첫 결제 시점엔 "이번 달에 지출이 얼마나 더 들어올지"를 판단할 근거
		// 자체가 없어(dailyRate=0) P_흐름이 하한(pFlowMin)에 눌린 채 시작해버려, beta=1이 8월
		// 안에는 절대 실적유도카드로 방향을 못 트는 인위적인 "콜드스타트" 문제가 생긴다 -
		// 실제 서비스라면 기존 유저는 항상 이런 이력을 갖고 있으므로 반영하는 게 맞다.
		walletHistory.put("202606", 150_000L);
		walletHistory.put("202607", 160_000L);
		YearMonth currentYearMonth = null;
		long totalDiscount = 0;
		List<String> log = new ArrayList<>();

		for (Payment payment : payments) {
			YearMonth paymentYearMonth = YearMonth.from(payment.date());
			if (currentYearMonth != null && !paymentYearMonth.equals(currentYearMonth)) {
				String closedKey = currentYearMonth.format(YEAR_MONTH_FORMATTER);
				long walletTotalForClosedMonth = 0;
				for (CardState card : cards) {
					card.completedMonthHistory.put(closedKey, card.currentMonthSpend);
					card.prevMonthSpend = card.currentMonthSpend;
					walletTotalForClosedMonth += card.currentMonthSpend;
					card.currentMonthSpend = 0;
				}
				walletHistory.put(closedKey, walletTotalForClosedMonth);
			}
			currentYearMonth = paymentYearMonth;

			CardState winner = null;
			Mode3Result winnerResult = null;
			for (CardState card : cards) {
				Mode3Result result = BenefitEngine.evaluatePriority(
					card.tiers, card.prevMonthSpend, card.currentMonthSpend, payment.categoryCode(), null,
					payment.categoryName(), payment.amount(), card.completedMonthHistory, walletHistory, PARAMS,
					payment.date(), beta, Map.of()
				);
				if (winner == null || result.total() > winnerResult.total()) {
					winner = card;
					winnerResult = result;
				}
			}

			BenefitApplication applied = BenefitEngine.selectPaymentBenefit(
				winner.tiers, winner.prevMonthSpend, payment.categoryCode(), null, payment.amount(), Map.of());
			totalDiscount += applied.discountAmount();
			winner.currentMonthSpend += payment.amount();

			log.add(String.format("%s %-4s %-12s %7d원 -> %-6s (할인 %,5d원, 전월실적 %,7d원)",
				payment.date(), payment.categoryName(), payment.merchantName(), payment.amount(),
				winner.cardName, applied.discountAmount(), winner.prevMonthSpend));
		}

		return new SimulationResult(totalDiscount, log);
	}

	private record SimulationResult(long totalDiscount, List<String> log) {
	}

	@Test
	void comparesTotalDiscountAcrossAFixedMultiCategoryPaymentSequence() {
		List<Payment> payments = paymentSequence();

		SimulationResult beta0 = simulate(payments, 0.0);
		SimulationResult beta1 = simulate(payments, 1.0);

		System.out.println("==== beta=0 (이번 달 확정 이득만) ====");
		beta0.log().forEach(System.out::println);
		System.out.println("beta=0 2개월 합계 할인액: " + beta0.totalDiscount() + "원");

		System.out.println("==== beta=1 (다음 달 기대 이득까지) ====");
		beta1.log().forEach(System.out::println);
		System.out.println("beta=1 2개월 합계 할인액: " + beta1.totalDiscount() + "원");

		System.out.println("차이(beta=1 - beta=0): " + (beta1.totalDiscount() - beta0.totalDiscount()) + "원");

		// beta=0은 매 결제 즉시형카드의 "now"가 항상 더 높아서(전 카테고리에서 즉시형 > 실적유도형
		// 0구간 혜택률) 전부 즉시형카드로만 몰린다 - 실적유도카드는 끝내 문턱을 못 넘는다.
		assertThat(beta0.totalDiscount()).isPositive();

		// beta=1은 최소 한 번 이상 실적유도카드를 선택해 문턱(10만원)을 넘기고, 그 결과 2개월
		// 합계 실수령 할인액이 beta=0보다 많다.
		assertThat(beta1.totalDiscount()).isGreaterThan(beta0.totalDiscount());
	}
}
