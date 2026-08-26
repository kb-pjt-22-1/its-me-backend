package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link BetaImpactSequentialPaymentsTest}와 같은 방식(결제 순서 재생 + 매 결제마다
 * evaluatePriority로 카드 선택 + selectPaymentBenefit으로 실제 확정 할인 계산)을, 지어낸
 * 카드가 아니라 실제 목데이터(its-me-infra/mysql/init/02_seed.sql)의 카드·혜택 JSON을 그대로
 * 파싱해서 재현한다. 구간 문턱(minimumSpending)은 목데이터 원본 값을 그대로 쓰고 손대지
 * 않는다 - card_1.json/card_2.json은 실제 시드 SQL에서 그대로 추출한 값이다.
 *
 * <p>카드 두 장:
 * <ul>
 *   <li><b>샘 쏘영 체크카드</b>(card_id=1) - 0구간(0원)은 혜택 없음, 1구간(5만원)부터 편의점
 *       (GS25·CU)·패스트푸드 등 5% - 문턱이 낮고 그 위로 더 오를 구간이 아예 없다(사실상
 *       "즉시형"에 가장 가까운 실제 카드).</li>
 *   <li><b>노리 체크카드</b>(card_id=2) - 0구간(0원)/1구간(20만원)부터 카페(스타벅스)·외식·
 *       영화 등 20%(2~4구간은 30만/50만/100만원, 이번 시나리오에선 사실상 도달권 밖) - 문턱이
 *       높지만 한번 넘으면 혜택이 훨씬 크다("실적유도형"에 가장 가까운 실제 카드).</li>
 * </ul>
 */
class BetaImpactRealCardsTest {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private record Payment(LocalDate date, String categoryCode, String categoryName, String merchantName,
		long amount) {
	}

	private static final class CardState {
		final String cardName;
		final List<PerformanceTier> tiers;
		long prevMonthSpend;
		long currentMonthSpend;
		final Map<String, Long> completedMonthHistory = new HashMap<>();
		// 서비스별 이번 달 소진량(card_benefit_monthly_usage에 해당) - 월 한도(monthlyDiscountLimit
		// 등)가 실제로는 그 달 여러 결제에 걸쳐 누적 소진된다는 걸 반영한다. 이걸 안 하면(매 결제를
		// usage=Map.of()로 독립 평가하면) 월 한도가 있는 혜택이 결제 건수만큼 매번 다시 꽉 찬
		// 것처럼 계산돼 실제보다 할인액이 부풀려진다.
		Map<String, BenefitUsage> monthlyUsage = new HashMap<>();

		CardState(String cardName, List<PerformanceTier> tiers, long seedPrevMonthSpend) {
			this.cardName = cardName;
			this.tiers = tiers;
			this.prevMonthSpend = seedPrevMonthSpend;
		}

		void recordUsage(BenefitApplication applied) {
			if (applied.serviceName() == null) {
				return;
			}
			BenefitUsage current = monthlyUsage.getOrDefault(applied.serviceName(), BenefitUsage.NONE);
			monthlyUsage.put(applied.serviceName(), new BenefitUsage(
				current.usedAmount() + applied.discountAmount(), current.usedMonthlyCount() + 1,
				current.usedAnnualCount()));
		}
	}

	private static List<PerformanceTier> loadRealCardTiers(String resourceName) {
		try (InputStream in = BetaImpactRealCardsTest.class.getResourceAsStream(resourceName)) {
			if (in == null) {
				throw new IllegalStateException("테스트 리소스를 찾을 수 없습니다: " + resourceName);
			}
			String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return BenefitJsonParser.parse(json, OBJECT_MAPPER);
		} catch (IOException e) {
			throw new IllegalStateException("카드 JSON 리소스 로딩 실패: " + resourceName, e);
		}
	}

	private static RecommendationParams.Constants constants() {
		return new RecommendationParams.Constants(1700.0, 0.35, 2.0, 0.25, 0.15, 0.05, 0.95, 0.2);
	}

	private static final RecommendationParams PARAMS = new RecommendationParams(
		Map.of(), new RecommendationParams.TicketHistogram(new double[0], Map.of()), Map.of(), constants());

	// 실제 목데이터의 카테고리 코드 그대로(BenefitJsonParser로 파싱해서 확인한 값).
	private static final String CAFE = "5813"; // 노리 체크카드 "커피 할인"(스타벅스 한정)
	private static final String CONVENIENCE = "5499"; // 샘 쏘영 "편스토랑한끼"(GS25/CU), 노리 "편의점 할인"(GS25)
	private static final String FAST_FOOD = "5814"; // 샘 쏘영 "콜라버거레이션"
	private static final String DINING = "5812"; // 노리 체크카드 "외식 할인"(아웃백/VIPS 한정, 최소결제 3만원)

	private static List<Payment> heavySpenderSequence() {
		List<Payment> payments = new ArrayList<>();
		// ---- 1개월차 (2026-08) - 노리가 커버하는 카페/편의점/외식만 다 몰아줘도 한 달에
		// 24만원(노리의 1구간 문턱 20만원을 실제로 넘김) - 문턱 값 자체는 그대로, 씀씀이 규모만 키움.
		payments.add(new Payment(LocalDate.of(2026, 8, 3), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 7), CONVENIENCE, "편의점", "GS25 역삼점", 30_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 10), DINING, "외식", "아웃백 강남점", 60_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 14), FAST_FOOD, "패스트푸드", "맥도날드 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 17), CONVENIENCE, "편의점", "CU 도곡점", 30_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 21), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 24), DINING, "외식", "VIPS 서초점", 60_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 28), CONVENIENCE, "편의점", "GS25 역삼점", 30_000));
		// ---- 2개월차 (2026-09) ----
		payments.add(new Payment(LocalDate.of(2026, 9, 3), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 7), CONVENIENCE, "편의점", "GS25 역삼점", 30_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 10), DINING, "외식", "아웃백 강남점", 60_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 14), FAST_FOOD, "패스트푸드", "맥도날드 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 17), CONVENIENCE, "편의점", "CU 도곡점", 30_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 21), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 24), DINING, "외식", "VIPS 서초점", 60_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 28), CONVENIENCE, "편의점", "GS25 역삼점", 30_000));
		return payments;
	}

	private static List<Payment> paymentSequence() {
		List<Payment> payments = new ArrayList<>();
		// ---- 1개월차 (2026-08) ----
		payments.add(new Payment(LocalDate.of(2026, 8, 2), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 5), CONVENIENCE, "편의점", "GS25 역삼점", 25_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 9), FAST_FOOD, "패스트푸드", "맥도날드 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 12), CONVENIENCE, "편의점", "CU 도곡점", 20_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 16), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 19), FAST_FOOD, "패스트푸드", "버거킹 서초점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 23), CONVENIENCE, "편의점", "GS25 역삼점", 25_000));
		payments.add(new Payment(LocalDate.of(2026, 8, 27), CAFE, "카페", "스타벅스 강남점", 15_000));
		// ---- 2개월차 (2026-09) ----
		payments.add(new Payment(LocalDate.of(2026, 9, 2), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 5), CONVENIENCE, "편의점", "GS25 역삼점", 25_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 9), FAST_FOOD, "패스트푸드", "맥도날드 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 12), CONVENIENCE, "편의점", "CU 도곡점", 20_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 16), CAFE, "카페", "스타벅스 강남점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 19), FAST_FOOD, "패스트푸드", "버거킹 서초점", 15_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 23), CONVENIENCE, "편의점", "GS25 역삼점", 25_000));
		payments.add(new Payment(LocalDate.of(2026, 9, 27), CAFE, "카페", "스타벅스 강남점", 15_000));
		return payments;
	}

	private static SimulationResult simulate(List<Payment> payments, double beta) {
		// 기존 유저 가정: 샘쏘영은 이미 자기 문턱(5만원)을 넘겨 써왔고, 노리는 아직 문턱(20만원)
		// 근처도 못 간 상태 - 실제 서비스에서도 흔한, "이미 구간을 확보한 카드"와 "거의 안 쓴
		// 카드"가 같이 지갑에 있는 시작점이다. 문턱 값 자체(50,000/200,000/...)는 목데이터 그대로다.
		CardState samSoyoung = new CardState("샘 쏘영 체크카드", loadRealCardTiers("/cards/card_1.json"), 60_000);
		CardState nori = new CardState("노리 체크카드", loadRealCardTiers("/cards/card_2.json"), 30_000);
		List<CardState> cards = List.of(samSoyoung, nori);

		samSoyoung.completedMonthHistory.put("202606", 55_000L);
		samSoyoung.completedMonthHistory.put("202607", 60_000L);
		nori.completedMonthHistory.put("202606", 25_000L);
		nori.completedMonthHistory.put("202607", 30_000L);

		Map<String, Long> walletHistory = new HashMap<>();
		walletHistory.put("202606", 80_000L); // 55,000 + 25,000
		walletHistory.put("202607", 90_000L); // 60,000 + 30,000

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
					card.monthlyUsage = new HashMap<>(); // 월 한도는 달마다 새로 찬다.
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
					payment.date(), beta, card.monthlyUsage
				);
				if (winner == null || result.total() > winnerResult.total()) {
					winner = card;
					winnerResult = result;
				}
			}

			BenefitApplication applied = BenefitEngine.selectPaymentBenefit(
				winner.tiers, winner.prevMonthSpend, payment.categoryCode(), null, payment.amount(),
				winner.monthlyUsage);
			totalDiscount += applied.discountAmount();
			winner.currentMonthSpend += payment.amount();
			winner.recordUsage(applied);

			log.add(String.format("%s %-5s %-14s %7d원 -> %-10s (할인 %,5d원, 전월실적 %,7d원)",
				payment.date(), payment.categoryName(), payment.merchantName(), payment.amount(),
				winner.cardName, applied.discountAmount(), winner.prevMonthSpend));
		}

		return new SimulationResult(totalDiscount, log);
	}

	private record SimulationResult(long totalDiscount, List<String> log) {
	}

	@Test
	void comparesTotalDiscountAcrossRealCardsAndRealCategoryBenefits() {
		List<Payment> payments = paymentSequence();

		SimulationResult beta0 = simulate(payments, 0.0);
		SimulationResult beta1 = simulate(payments, 1.0);

		System.out.println("==== beta=0 (이번 달 확정 이득만) - 샘 쏘영 vs 노리(실제 카드) ====");
		beta0.log().forEach(System.out::println);
		System.out.println("beta=0 2개월 합계 할인액: " + beta0.totalDiscount() + "원");

		System.out.println("==== beta=1 (다음 달 기대 이득까지) - 샘 쏘영 vs 노리(실제 카드) ====");
		beta1.log().forEach(System.out::println);
		System.out.println("beta=1 2개월 합계 할인액: " + beta1.totalDiscount() + "원");

		System.out.println("차이(beta=1 - beta=0): " + (beta1.totalDiscount() - beta0.totalDiscount()) + "원");

		assertThat(beta0.totalDiscount()).isPositive();
		assertThat(beta1.totalDiscount()).isPositive();
		// 실제 목데이터 기준 발견: 이 정도의(월 14.5만원) 씀씀이로는 노리 체크카드의 1구간
		// 문턱(20만원, 원본 값 그대로)을 2개월 내내 넘기지 못한다 - beta=1이 카페 결제를
		// 노리로 돌려도(future>blank(0)이라 항상 이김) 실제로 구간이 안 열리니 그 결제들은
		// 여전히 0% 그대로다. 그 결과 beta=0과 beta=1의 2개월 실수령 할인액이 완전히
		// 같아진다 - "미래를 내다본 추천"이 항상 이득을 주는 게 아니라, 문턱이 실제로
		// 닿는 씀씀이일 때만 의미가 있다는 걸 실제 카드 데이터로 보여준다.
		assertThat(beta1.totalDiscount()).isEqualTo(beta0.totalDiscount());
	}

	@Test
	void betaOneOutperformsWhenRealSpendingActuallyReachesTheRealThreshold() {
		List<Payment> payments = heavySpenderSequence();

		SimulationResult beta0 = simulate(payments, 0.0);
		SimulationResult beta1 = simulate(payments, 1.0);

		System.out.println("==== [씀씀이 확대] beta=0 - 샘 쏘영 vs 노리(실제 카드) ====");
		beta0.log().forEach(System.out::println);
		System.out.println("beta=0 2개월 합계 할인액: " + beta0.totalDiscount() + "원");

		System.out.println("==== [씀씀이 확대] beta=1 - 샘 쏘영 vs 노리(실제 카드) ====");
		beta1.log().forEach(System.out::println);
		System.out.println("beta=1 2개월 합계 할인액: " + beta1.totalDiscount() + "원");

		System.out.println("차이(beta=1 - beta=0): " + (beta1.totalDiscount() - beta0.totalDiscount()) + "원");

		// 문턱(20만원)은 그대로인데 카페/편의점/외식을 다 몰아주면 한 달에 24만원이라 실제로
		// 넘을 수 있는 씀씀이다 - 이번엔 beta=1이 실제로 더 많은 할인을 받아와야 한다.
		assertThat(beta1.totalDiscount()).isGreaterThan(beta0.totalDiscount());
	}
}
