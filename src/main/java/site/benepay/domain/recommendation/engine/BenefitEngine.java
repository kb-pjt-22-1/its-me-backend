package site.benepay.domain.recommendation.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 모드 3(우선순위 비교) 계산 엔진. CsvProcessing/benefits.py + category_search.py의
 * evaluate_priority()/compare_priority()를 포팅한 것 - 모드 1(즉시 할인)·모드 2(실적
 * 채우기)는 삭제됐다(모드 3 하나로 대체).
 *
 * <p>"이미 혜택을 받고 있는 카드 두 장 중 어디에 이번 달 남은 지출을 몰아줄까"에 답한다.
 * 이번 달 확정 이득(now)과 다음 달 확률적 기대 이득(future)을 같은 화폐 단위(원/월)로
 * beta 가중 합산해 total을 낸다 - 카드 랭킹은 이 total 내림차순이다. 모드 2와 달리
 * P_흐름(다음 구간 도달 확률)의 기준이 "이 카드의 평소 습관 지출"이 아니라 "지갑 전체의
 * 남은 지출 여력"이다 - 의도적으로 몰아준다는 전제이기 때문이다.</p>
 */
public final class BenefitEngine {

	private static final Set<String> PER_LITER_METHODS =
		Set.of("PER_LITER_STATEMENT_DISCOUNT", "PER_LITER_CASHBACK");

	private BenefitEngine() {
	}

	/**
	 * 전월 실적으로 확정된 구간을 고른다. 기준액이 같은 구간이 여럿이면 혜택이
	 * 있는 쪽을 고른다(README "3. 구간 선택" - 0구간과 겹치는 경우가 있음).
	 */
	public static PerformanceTier activeTier(List<PerformanceTier> tiers, long prevMonthSpend) {
		List<PerformanceTier> eligible = tiers.stream()
			.filter(t -> t.minimumSpending() <= prevMonthSpend)
			.toList();
		if (eligible.isEmpty()) {
			return tiers.get(0);
		}
		return eligible.stream()
			.max(Comparator.comparingLong(PerformanceTier::minimumSpending)
				.thenComparing(t -> !t.realBenefits().isEmpty()))
			.orElseThrow();
	}

	public static double effectiveRate(BenefitNode b, boolean weekend, double fuelPricePerLiter) {
		if (PER_LITER_METHODS.contains(b.discountMethod())) {
			long perLiter = weekend ? b.weekendDiscountPerLiter() : b.weekdayDiscountPerLiter();
			return perLiter / fuelPricePerLiter;
		}
		return b.discountRate() / 100.0;
	}

	// ==================================================================== 다음 구간 / 기준선

	/**
	 * 이번 달이 지금 끝난다면 다음 달에 적용될 구간. 모드 3의 gain 기준선이다.
	 * 전월 실적으로 정해지는 activeTier와 헷갈리면 안 된다 - 로직은 같고 기준 금액만 다르다.
	 */
	public static PerformanceTier baselineTier(List<PerformanceTier> tiers, long currentMonthSpend) {
		return activeTier(tiers, currentMonthSpend);
	}

	/**
	 * 이번 달 누적 실적 기준으로 아직 안 열렸고, 이 카테고리에 혜택이 있는 다음 구간.
	 * Python 원본은 카테고리 구분 없이 "혜택이 있는 다음 구간"을 고르지만, 이 포팅은
	 * 카테고리 하나로 스코프를 좁혔으므로 이 카테고리를 커버하는지까지 확인한다.
	 */
	public static PerformanceTier nextTier(List<PerformanceTier> tiers, long currentMonthSpend, String categoryCode) {
		return tiers.stream()
			.filter(t -> t.minimumSpending() > currentMonthSpend && !t.benefitsForCategory(categoryCode).isEmpty())
			.min(Comparator.comparingLong(PerformanceTier::minimumSpending))
			.orElse(null);
	}

	// ==================================================================== 확률

	private static double erf(double x) {
		// Abramowitz-Stegun 7.1.26 근사. 최대오차 1.5e-7 - 확률 추정 용도로는 충분하다.
		double sign = x < 0 ? -1.0 : 1.0;
		double ax = Math.abs(x);
		double t = 1.0 / (1.0 + 0.3275911 * ax);
		double poly = ((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592;
		return sign * (1.0 - poly * t * Math.exp(-ax * ax));
	}

	private static double normalCdf(double z) {
		return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
	}

	private static double logit(double p) {
		double eps = 1e-6;
		double clamped = Math.min(1 - eps, Math.max(eps, p));
		return Math.log(clamped / (1 - clamped));
	}

	private static double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}

	private static double clamp(double x, double lo, double hi) {
		return Math.max(lo, Math.min(hi, x));
	}

	private static int daysInYearMonth(String yyyymm) {
		int year = Integer.parseInt(yyyymm.substring(0, 4));
		int month = Integer.parseInt(yyyymm.substring(4, 6));
		return YearMonth.of(year, month).lengthOfMonth();
	}

	/** 월별로 찍힌 일평균 실적의 평균. 이력이 없으면 0. 카드 이력·지갑 이력 양쪽에 다 쓴다. */
	private static double dailyRate(Map<String, Long> spendHistory) {
		if (spendHistory.isEmpty()) {
			return 0.0;
		}
		double sum = 0.0;
		for (Map.Entry<String, Long> e : spendHistory.entrySet()) {
			sum += e.getValue() / (double) daysInYearMonth(e.getKey());
		}
		return sum / spendHistory.size();
	}

	/** 일평균 실적의 변동계수(CV). 이력 2개월 미만이면 defaultCv로 대체한다. */
	private static double cv(Map<String, Long> spendHistory, RecommendationParams.Constants constants) {
		if (spendHistory.size() < 2) {
			return constants.defaultCv();
		}
		List<Double> rates = spendHistory.entrySet().stream()
			.map(e -> e.getValue() / (double) daysInYearMonth(e.getKey()))
			.toList();
		double mean = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		if (mean == 0.0) {
			return constants.defaultCv();
		}
		double variance = rates.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0);
		return Math.max(constants.minCv(), Math.sqrt(variance) / mean);
	}

	/** 이력 기간 중 그 실적 구간을 넘긴 개월 수. */
	private static int hits(Map<String, Long> spendHistory, long threshold) {
		return (int) spendHistory.values().stream().filter(v -> v >= threshold).count();
	}

	/**
	 * 남은 일수를 요일 가중으로 환산한 값(오늘 다음 날부터 이번 달 말일까지). today를 인자로
	 * 받는 이유는 내부에서 LocalDate.now()를 직접 부르면 "말일이라 남은 일수 0" 같은 경계값을
	 * 테스트에서 재현할 수 없기 때문이다 - 호출부(서비스 계층)에서 LocalDate.now()를 넘긴다.
	 * weekdayIndex에 이 카테고리가 없으면 가중 없이 남은 일수 그대로 쓴다.
	 */
	private static double remainingFactor(String categoryName, RecommendationParams params, LocalDate today) {
		int remainingDays = today.lengthOfMonth() - today.getDayOfMonth();
		double[] weights = params.weekdayIndex() == null ? null : params.weekdayIndex().get(categoryName);
		if (weights == null) {
			return remainingDays;
		}
		double total = 0.0;
		for (int i = 1; i <= remainingDays; i++) {
			DayOfWeek dow = today.plusDays(i).getDayOfWeek();
			total += weights[dow.getValue() - 1];
		}
		return total;
	}

	// package-private(테스트 전용): CsvProcessing의 test_boundary.py가 fill_probability를
	// evaluate_priority와 별개로 직접 테스트하므로, 여기서도 같은 격리 테스트가 가능하도록 연다.
	record FillProbability(double pFill, double pFlow, double pHist, double expectedMore) {
	}

	/**
	 * 이번 달 이 카테고리 구간을 채울 확률. 두 증거(남은 기간에 gap을 넘을 확률 P_흐름,
	 * 이 구간을 실제 넘겨온 비율 P_이력)를 로그오즈에서 합친다. gap이 이미 0 이하면
	 * (구간을 이미 넘었으면) 전부 확실(1.0)로 본다.
	 *
	 * <p>dailyRate/cv는 P_흐름의 기준 - 모드 2라면 이 카드의 평소 습관 지출, 모드 3이라면
	 * "의도적으로 몰아준다"는 전제라 지갑 전체의 남은 지출 여력을 넘긴다. hits/months는
	 * 항상 이 카드 자체의 이력 기준이다(모드 2/3 공통 - P_이력은 습관이 아니라 실제로
	 * 그 카드로 구간을 넘겨온 이력이라 카드 습관 지출로 바뀌지 않는다).</p>
	 */
	static FillProbability fillProbability(
		double dailyRate, double cv, double remainingFactor, long gap, int hits, int months,
		RecommendationParams.Constants constants
	) {
		if (gap <= 0) {
			return new FillProbability(1.0, 1.0, 1.0, 0.0);
		}
		double expectedMore = dailyRate * remainingFactor;
		double sigma = Math.max(expectedMore * cv, 1.0);
		double pFlow = clamp(normalCdf((expectedMore - gap) / sigma), constants.pFlowMin(), constants.pFlowMax());
		double pHist = (hits + constants.priorStrength() * constants.historyPrior())
			/ (months + constants.priorStrength());
		double pFill = sigmoid(logit(pHist) + logit(pFlow));
		return new FillProbability(pFill, pFlow, pHist, expectedMore);
	}

	private static double qualifyingRatio(BenefitNode benefit, String categoryName, RecommendationParams params) {
		if (benefit.minimumPaymentAmount() <= 0) {
			return 1.0;
		}
		return params.ticketHistogram().passRate(categoryName, benefit.minimumPaymentAmount());
	}

	/**
	 * 혜택 하나가 이 카테고리의 통상결제액(ticket) 1건을 근거로 한 달에 주는 할인 추정치.
	 * benefits.py의 benefit_discount()를 단일 카테고리·단일 혜택 배정으로 단순화한 것.
	 *
	 * <p>usage는 이 혜택을 이번 달(월 한도)·올해(연 한도) 이미 얼마나 썼는지다(card_benefit_
	 * monthly_usage 집계) - 한도를 "매번 새로 꽉 찬 상태"로 가정하지 않고 남은 만큼만 캡으로
	 * 쓴다. monthlyEligibleLimit(월 이용금액 한도)은 소진액을 추적하지 않으므로(BenefitUsage
	 * 참고) usage와 무관하게 기존 그대로 이번 ticket 기준으로만 적용한다.</p>
	 */
	private static long benefitDiscountEstimate(
		BenefitNode benefit, String categoryName, long ticket, RecommendationParams params, BenefitUsage usage
	) {
		double qualifying = qualifyingRatio(benefit, categoryName, params);
		double usable = ticket * qualifying;
		double count = usable / ticket;

		if (benefit.monthlyEligibleLimit() != null && usable > benefit.monthlyEligibleLimit()) {
			double scale = usable == 0 ? 0 : benefit.monthlyEligibleLimit() / usable;
			usable = benefit.monthlyEligibleLimit();
			count *= scale;
		}

		double discount;
		if (benefit.discountAmount() > 0) {
			Double limit = benefit.monthlyCountLimit() == null
				? null : Math.max(0.0, (double) benefit.monthlyCountLimit() - usage.usedMonthlyCount());
			if (benefit.annualCountLimit() != null) {
				double remainingAnnual = Math.max(0, benefit.annualCountLimit() - usage.usedAnnualCount());
				double perMonth = remainingAnnual / 12.0;
				limit = limit == null ? perMonth : Math.min(limit, perMonth);
			}
			double uses = limit == null ? count : Math.min(count, limit);
			discount = benefit.discountAmount() * uses;
		} else {
			discount = usable * effectiveRate(benefit, false, params.constants().fuelPricePerLiter());
			if (benefit.maximumDiscountPerTransaction() != null && count > 0) {
				discount = Math.min(discount, benefit.maximumDiscountPerTransaction() * count);
			}
		}

		if (benefit.monthlyDiscountLimit() != null) {
			double remaining = Math.max(0, benefit.monthlyDiscountLimit() - usage.usedAmount());
			discount = Math.min(discount, remaining);
		}
		return Math.round(discount);
	}

	private static final Set<String> PAYMENT_TIME_EXCLUDED_METHODS =
		Set.of("POINT_ACCUMULATION", "PER_LITER_STATEMENT_DISCOUNT", "PER_LITER_CASHBACK");

	/**
	 * 결제 1건에 실제로 적용할 혜택을 고른다. benefitDiscountEstimate가 "통상결제액 1건이
	 * 반복된다면"이라는 통계적 추정인 것과 달리, 이건 이번 결제 금액 하나에 대한 확정 계산이다.
	 *
	 * <p>POINT_ACCUMULATION(적립형)은 결제금액을 깎지 않고 포인트로 쌓이는 것이라 이 결제의
	 * discountAmount로 취급하면 데이터가 왜곡돼 제외한다. PER_LITER_*는 리터 단위 계산인데
	 * 결제 API에 리터 정보가 없어 계산할 수 없어 제외한다. 카테고리를 커버하는 나머지 혜택 중
	 * 최소결제금액을 충족하고 월/연 횟수 한도가 아직 안 찬 것들 가운데 할인액이 가장 큰 하나만
	 * 고른다 - 실제 결제는 한 번에 한 혜택만 적용되는 게 자연스럽다.</p>
	 */
	public static BenefitApplication selectPaymentBenefit(
		List<PerformanceTier> tiers,
		long prevMonthSpend,
		String categoryCode,
		long paymentAmount,
		Map<String, BenefitUsage> usageByServiceName
	) {
		PerformanceTier active = activeTier(tiers, prevMonthSpend);

		BenefitApplication best = BenefitApplication.NONE;
		for (BenefitNode benefit : active.benefitsForCategory(categoryCode)) {
			if (PAYMENT_TIME_EXCLUDED_METHODS.contains(benefit.discountMethod())) {
				continue;
			}
			if (benefit.minimumPaymentAmount() > paymentAmount) {
				continue;
			}
			BenefitUsage usage = usageByServiceName.getOrDefault(benefit.serviceName(), BenefitUsage.NONE);
			long discount = singleTransactionDiscount(benefit, paymentAmount, usage);
			if (discount > best.discountAmount()) {
				best = new BenefitApplication(benefit.serviceName(), discount);
			}
		}
		return best;
	}

	private static long singleTransactionDiscount(BenefitNode benefit, long paymentAmount, BenefitUsage usage) {
		if (benefit.monthlyCountLimit() != null && usage.usedMonthlyCount() >= benefit.monthlyCountLimit()) {
			return 0L;
		}
		if (benefit.annualCountLimit() != null && usage.usedAnnualCount() >= benefit.annualCountLimit()) {
			return 0L;
		}

		long discount = benefit.discountAmount() > 0
			? benefit.discountAmount()
			: Math.round(paymentAmount * (benefit.discountRate() / 100.0));

		if (benefit.maximumDiscountPerTransaction() != null) {
			discount = Math.min(discount, benefit.maximumDiscountPerTransaction());
		}
		if (benefit.monthlyDiscountLimit() != null) {
			long remaining = Math.max(0, benefit.monthlyDiscountLimit() - usage.usedAmount());
			discount = Math.min(discount, remaining);
		}
		return Math.max(discount, 0L);
	}

	/**
	 * 이 구간이 이 카테고리에서 주는 월 할인액 추정치. 이 카테고리를 커버하는 혜택이 여럿이면
	 * (Python의 allocate()를 카테고리 하나로 단순화해) 이 지출로 가장 큰 할인을 주는 혜택
	 * 하나만 고른다 - 같은 지출이 여러 혜택에 중복 계상되지 않는다. 모드 3의 now(활성 구간)와
	 * gain(다음/기준 구간 차이) 양쪽에서 재사용한다.
	 */
	private static long tierDiscountForCategory(
		PerformanceTier tier, String categoryCode, String categoryName, long ticket, RecommendationParams params,
		Map<String, BenefitUsage> usageByServiceName
	) {
		List<BenefitNode> covering = tier.benefitsForCategory(categoryCode);
		if (covering.isEmpty()) {
			return 0L;
		}
		BenefitNode best = null;
		long bestDiscount = 0L;
		for (BenefitNode benefit : covering) {
			BenefitUsage usage = usageByServiceName.getOrDefault(benefit.serviceName(), BenefitUsage.NONE);
			long discount = benefitDiscountEstimate(benefit, categoryName, ticket, params, usage);
			if (best == null || discount > bestDiscount) {
				best = benefit;
				bestDiscount = discount;
			}
		}
		if (!best.integratedLimitExcluded() && tier.combinedCap() != null) {
			bestDiscount = Math.min(bestDiscount, tier.combinedCap());
		}
		return bestDiscount;
	}

	// ==================================================================== 모드 3

	/**
	 * 이번 달 남은 지출을 이 카드에 몰아줬을 때의 기대 총 가치(원/월).
	 *
	 * <ul>
	 *   <li>now - 이번 달 확정 구간(activeTier)에서 이 카테고리 지출 전체에 대한 예상 월
	 *       할인 총액(tierDiscountForCategory 재사용 - 모드 2의 gain과 같은 재료).</li>
	 *   <li>future - 다음 구간까지 gap을 채울 확률(walletSpendHistory 기준 P_흐름 × 이
	 *       카드 자체 이력 기준 P_이력) × 구간이 열렸을 때 늘어나는 월 할인(gain).</li>
	 *   <li>total - now + beta × future. beta=0이면 future를 아예 안 보므로 "이번 달
	 *       확정 이득만으로 줄 세우기"(모드 1과 동일한 순위)로 되돌아간다.</li>
	 * </ul>
	 *
	 * @param tiers 이 카드의 전체 구간(오름차순)
	 * @param prevMonthSpend 전월 실적 - activeTier(now)를 정한다
	 * @param currentMonthSpend 이번 달 누적 실적 - baselineTier/nextTier(gap, gain)를 정한다
	 * @param categoryCode 매칭용(merchant_categories 기준)
	 * @param categoryName typicalPaymentAmount/통과율 조회용(params.json 키)
	 * @param typicalAmount 이 카테고리의 통상 결제액(ticket)
	 * @param cardSpendHistory 이 카드의 과거 완료된 달 실적(yyyyMM -> 금액) - P_이력(hits/months)에 쓴다
	 * @param walletSpendHistory 보유 카드 전체를 합산한 과거 완료된 달 실적 - P_흐름(daily_rate/cv)에 쓴다.
	 *        "의도적으로 몰아준다"는 전제라 이 카드의 평소 습관이 아니라 지갑 전체 여력이 기준이다
	 * @param today 남은 일수 계산 기준일(호출부에서 LocalDate.now()를 넘긴다)
	 * @param beta 확정 이득과 확률적 기대 이득을 몇 대 몇으로 합산할지 정하는 가정값
	 * @param usageByServiceName 이 카드가 혜택별로 이번 달/올해 이미 소진한 사용량(card_benefit_
	 *        monthly_usage) - now/next/baseline 세 시나리오 모두 "지금 이 순간까지의 소진량"은
	 *        같으므로 동일한 맵을 그대로 재사용한다
	 */
	public static Mode3Result evaluatePriority(
		List<PerformanceTier> tiers,
		long prevMonthSpend,
		long currentMonthSpend,
		String categoryCode,
		String categoryName,
		long typicalAmount,
		Map<String, Long> cardSpendHistory,
		Map<String, Long> walletSpendHistory,
		RecommendationParams params,
		LocalDate today,
		double beta,
		Map<String, BenefitUsage> usageByServiceName
	) {
		boolean hasAnywhere = tiers.stream().anyMatch(t -> !t.benefitsForCategory(categoryCode).isEmpty());
		if (!hasAnywhere) {
			return Mode3Result.blank("이 카테고리 혜택 자체가 없음");
		}

		PerformanceTier active = activeTier(tiers, prevMonthSpend);
		long now = tierDiscountForCategory(active, categoryCode, categoryName, typicalAmount, params,
			usageByServiceName);

		PerformanceTier next = nextTier(tiers, currentMonthSpend, categoryCode);
		if (next == null) {
			return new Mode3Result(now, 0.0, 1.0, 1.0, 1.0, 0L, 0L, now,
				"이미 최고 구간 확보 · 이번 달 확정 이득만 존재");
		}

		RecommendationParams.Constants constants = params.constants();
		long gap = next.minimumSpending() - currentMonthSpend;
		double remainingFactor = remainingFactor(categoryName, params, today);
		int hits = hits(cardSpendHistory, next.minimumSpending());
		int months = cardSpendHistory.size();

		FillProbability prob = fillProbability(
			dailyRate(walletSpendHistory), cv(walletSpendHistory, constants), remainingFactor, gap, hits, months, constants
		);

		PerformanceTier baseline = baselineTier(tiers, currentMonthSpend);
		long nextDiscount = tierDiscountForCategory(next, categoryCode, categoryName, typicalAmount, params,
			usageByServiceName);
		long baselineDiscount =
			tierDiscountForCategory(baseline, categoryCode, categoryName, typicalAmount, params, usageByServiceName);
		long gain = nextDiscount - baselineDiscount;

		double future = prob.pFill() * Math.max(0, gain);
		double total = now + beta * future;

		String note = String.format(
			"이번 달 확정 %,d원 + 다음 달 기대 %,.0f원(성사확률 %.0f%% × 이득 %+,d원) = 총 %,.0f원",
			now, future, prob.pFill() * 100, gain, total
		);

		return new Mode3Result(now, future, prob.pFill(), prob.pFlow(), prob.pHist(), gap, gain, total, note);
	}
}
