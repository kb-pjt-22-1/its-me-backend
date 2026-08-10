package site.benepay.domain.recommendation.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 모드 1(즉시 할인)·모드 2(실적 채우기) 계산 엔진. CsvProcessing/benefits.py +
 * category_search.py를 포팅한 것 - 모드 3(우선순위 비교)은 여기 없다(다음 단계).
 *
 * <p>모드 1은 혜택마다 건당 최소 결제액이 다르므로 각 혜택을 자기 하한가격에서 평가하고,
 * 금액이 달라도 <b>할인율</b>로 비교한다(README "4. 모드 1"). 모드 2는 카테고리 하나로
 * 스코프를 좁혀 포팅했다 - Python 원본의 지갑 전체 allocate()/segmentShare 콜드스타트
 * 추정 대신, 이미 카테고리가 고정된 호출 맥락(카테고리 검색)에 맞춰 그 카테고리를 커버하는
 * 혜택 중 하나만 고르고, 월 지출 추정치는 recommendation-params.json의 통상결제액
 * (typicalPaymentAmount) 1건을 그대로 쓴다 - 실제 월 총 지출 데이터가 없어서다.</p>
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

	public static Long effectiveMonthlyCap(BenefitNode b, double fuelPricePerLiter) {
		if (b.monthlyDiscountLimit() != null) {
			return b.monthlyDiscountLimit();
		}
		if (b.monthlyEligibleLimit() != null) {
			return Math.round(b.monthlyEligibleLimit() * effectiveRate(b, false, fuelPricePerLiter));
		}
		if (b.discountAmount() > 0 && b.monthlyCountLimit() != null) {
			return b.discountAmount() * b.monthlyCountLimit();
		}
		return null;
	}

	public static long evaluationAmount(BenefitNode b, long typicalAmount) {
		return Math.max(b.minimumPaymentAmount(), typicalAmount);
	}

	public static double nominalPaymentDiscount(BenefitNode b, long amount, boolean weekend, double fuelPricePerLiter) {
		if (b.minimumPaymentAmount() > 0 && amount < b.minimumPaymentAmount()) {
			return 0.0;
		}
		if (b.discountAmount() > 0) {
			return b.discountAmount();
		}
		return amount * effectiveRate(b, weekend, fuelPricePerLiter);
	}

	public static double paymentDiscount(BenefitNode b, long amount, Double remainingCap, boolean weekend, double fuelPricePerLiter) {
		if (b.minimumPaymentAmount() > 0 && amount < b.minimumPaymentAmount()) {
			return 0.0;
		}
		double eligible = amount;
		if (b.maximumEligiblePerTransaction() != null) {
			eligible = Math.min(eligible, b.maximumEligiblePerTransaction());
		}
		double discount = b.discountAmount() > 0 ? b.discountAmount() : eligible * effectiveRate(b, weekend, fuelPricePerLiter);
		if (b.maximumDiscountPerTransaction() != null) {
			discount = Math.min(discount, b.maximumDiscountPerTransaction());
		}
		if (remainingCap != null) {
			discount = Math.min(discount, Math.max(0.0, remainingCap));
		}
		return discount;
	}

	private static Long remainingCap(BenefitNode b, BenefitUsage usage, double fuelPricePerLiter) {
		Long cap = effectiveMonthlyCap(b, fuelPricePerLiter);
		return cap == null ? null : cap - usage.usedAmount();
	}

	private record RemainingUses(int remaining, String label) {
	}

	private static RemainingUses remainingUses(BenefitNode b, BenefitUsage usage) {
		RemainingUses tightest = null;
		if (b.annualCountLimit() != null) {
			tightest = new RemainingUses(b.annualCountLimit() - usage.usedCountYear(), "연 " + b.annualCountLimit() + "회");
		}
		if (b.monthlyCountLimit() != null) {
			RemainingUses monthly = new RemainingUses(b.monthlyCountLimit() - usage.usedCountMonth(), "월 " + b.monthlyCountLimit() + "회");
			if (tightest == null || monthly.remaining() < tightest.remaining()) {
				tightest = monthly;
			}
		}
		return tightest;
	}

	/**
	 * 구간 통합한도 잔액. 통합한도 소진액은 별도로 저장하지 않고, 이 구간에서
	 * 통합한도에 포함되는(integratedLimitExcluded가 아닌) 혜택들의 이번 달
	 * 소진액을 합산해서 구한다.
	 */
	private static Long remainingTotalCap(PerformanceTier tier, Map<String, BenefitUsage> usageByServiceName) {
		Long cap = tier.combinedCap();
		if (cap == null) {
			return null;
		}
		long usedTotal = tier.realBenefits().stream()
			.filter(b -> !b.integratedLimitExcluded())
			.mapToLong(b -> usageByServiceName.getOrDefault(b.serviceName(), BenefitUsage.NONE).usedAmount())
			.sum();
		return cap - usedTotal;
	}

	private record BestCandidate(
		BenefitNode benefit, long amount, double discount, double rate, Long left, RemainingUses uses, double nominalRate
	) {
	}

	private record Blocked(BenefitStatus status, String note) {
	}

	/**
	 * 지금 이 결제에서 받는 할인. tiers는 이 카드의 전체 구간(오름차순), prevMonthSpend는
	 * 전월 실적 - 이 값으로 활성 구간이 정해진다. categoryCode는 매칭용(merchant_categories
	 * 기준), categoryName은 typicalPaymentAmount/통과율 조회용(params.json 키).
	 */
	public static Mode1Result evaluateNow(
		List<PerformanceTier> tiers,
		long prevMonthSpend,
		String categoryCode,
		String categoryName,
		long typicalAmount,
		Map<String, BenefitUsage> usageByServiceName,
		RecommendationParams params
	) {
		PerformanceTier active = activeTier(tiers, prevMonthSpend);
		List<BenefitNode> covering = active.benefitsForCategory(categoryCode);

		boolean hasAnywhere = tiers.stream().anyMatch(t -> !t.benefitsForCategory(categoryCode).isEmpty());
		if (!hasAnywhere) {
			return Mode1Result.blank(BenefitStatus.NO_BENEFIT, typicalAmount, "이 카테고리 혜택 자체가 없음");
		}
		if (covering.isEmpty()) {
			long need = tiers.stream()
				.filter(t -> !t.benefitsForCategory(categoryCode).isEmpty())
				.mapToLong(PerformanceTier::minimumSpending)
				.min()
				.orElse(0L);
			return Mode1Result.blank(BenefitStatus.PERFORMANCE_INSUFFICIENT, typicalAmount,
				String.format("전월 실적 %,d원 < %,d원 구간", prevMonthSpend, need));
		}

		double fuelPricePerLiter = params.constants().fuelPricePerLiter();
		Long totalLeft = remainingTotalCap(active, usageByServiceName);

		BestCandidate best = null;
		List<Blocked> blocked = new ArrayList<>();

		for (BenefitNode benefit : covering) {
			BenefitUsage usage = usageByServiceName.getOrDefault(benefit.serviceName(), BenefitUsage.NONE);

			RemainingUses uses = remainingUses(benefit, usage);
			if (uses != null && uses.remaining() <= 0) {
				blocked.add(new Blocked(BenefitStatus.COUNT_EXHAUSTED,
					benefit.serviceName() + " " + uses.label() + " 모두 사용"));
				continue;
			}

			Long left = remainingCap(benefit, usage, fuelPricePerLiter);
			if (left != null && left <= 0) {
				blocked.add(new Blocked(BenefitStatus.LIMIT_EXHAUSTED, benefit.serviceName() + " 한도 소진"));
				continue;
			}

			long amount = evaluationAmount(benefit, typicalAmount);
			double nominal = nominalPaymentDiscount(benefit, amount, false, fuelPricePerLiter);
			double discount = paymentDiscount(benefit, amount, left == null ? null : left.doubleValue(), false, fuelPricePerLiter);
			if (totalLeft != null) {
				discount = Math.min(discount, Math.max(0.0, totalLeft));
			}
			if (discount <= 0) {
				continue;
			}

			double rate = discount / amount;
			if (best == null || rate > best.rate()) {
				best = new BestCandidate(benefit, amount, discount, rate, left, uses, amount == 0 ? 0.0 : nominal / amount);
			}
		}

		if (best == null) {
			Blocked chosen = blocked.isEmpty() ? new Blocked(BenefitStatus.LIMIT_EXHAUSTED, "적용 가능한 혜택 없음") : blocked.get(0);
			return Mode1Result.blank(chosen.status(), typicalAmount, chosen.note());
		}

		return buildResult(best, typicalAmount, categoryName, params);
	}

	private static Mode1Result buildResult(BestCandidate best, long typicalAmount, String categoryName, RecommendationParams params) {
		BenefitNode benefit = best.benefit();
		StringBuilder note = new StringBuilder(benefit.serviceName())
			.append(" · 잔여한도 ")
			.append(best.left() == null ? "무제한" : String.format("%,d원", best.left()));

		boolean capped = best.nominalRate() - best.rate() > 1e-9;
		if (capped) {
			note.insert(0, String.format("명목 %.1f%% → 한도로 %.1f%% · ", best.nominalRate() * 100, best.rate() * 100));
		}
		if (best.uses() != null) {
			note.append(String.format(" · %s 중 %d회 남음", best.uses().label(), best.uses().remaining()));
		}
		if (!benefit.merchantNote().isEmpty()) {
			note.append(" · ").append(benefit.merchantNote());
		}

		boolean conditional = best.amount() > typicalAmount;
		if (conditional) {
			double share = params.ticketHistogram().passRate(categoryName, benefit.minimumPaymentAmount());
			note.append(String.format(" · 최소 %,d원 필요 (통상 %,d원, 이 업종 결제의 %.0f%%)",
				benefit.minimumPaymentAmount(), typicalAmount, share * 100));
		}

		BenefitStatus status = conditional ? BenefitStatus.CONDITIONAL_DISCOUNT : BenefitStatus.IMMEDIATE_DISCOUNT;
		return new Mode1Result(status, best.rate(), best.nominalRate(), capped,
			Math.round(best.discount()), best.amount(), benefit, note.toString());
	}

	// ==================================================================== 모드 2

	/**
	 * 이번 달이 지금 끝난다면 다음 달에 적용될 구간. 모드 2의 비교 기준선이다.
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

	/** 카드에 월별로 찍힌 일평균 실적의 평균. 이력이 없으면 0. */
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
	// evaluate_build와 별개로 직접 테스트하므로, 여기서도 같은 격리 테스트가 가능하도록 연다.
	record FillProbability(double pFill, double pFlow, double pHist, double expectedMore) {
	}

	/**
	 * 이번 달 이 카테고리 구간을 채울 확률. 두 증거(평소 페이스로 남은 기간에 gap을 넘을
	 * 확률 P_흐름, 이 카드로 그 구간을 실제 넘겨온 비율 P_이력)를 로그오즈에서 합친다.
	 * gap이 이미 0 이하면(구간을 이미 넘었으면) 전부 확실(1.0)로 본다.
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
	 */
	private static long benefitDiscountEstimate(
		BenefitNode benefit, String categoryName, long ticket, RecommendationParams params
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
			Double limit = benefit.monthlyCountLimit() == null ? null : benefit.monthlyCountLimit().doubleValue();
			if (benefit.annualCountLimit() != null) {
				double perMonth = benefit.annualCountLimit() / 12.0;
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
			discount = Math.min(discount, benefit.monthlyDiscountLimit());
		}
		return Math.round(discount);
	}

	/**
	 * 이 구간이 이 카테고리에서 주는 월 할인액 추정치. 이 카테고리를 커버하는 혜택이 여럿이면
	 * (Python의 allocate()를 카테고리 하나로 단순화해) 이 지출로 가장 큰 할인을 주는 혜택
	 * 하나만 고른다 - 같은 지출이 여러 혜택에 중복 계상되지 않는다.
	 */
	private static long tierDiscountForCategory(
		PerformanceTier tier, String categoryCode, String categoryName, long ticket, RecommendationParams params
	) {
		List<BenefitNode> covering = tier.benefitsForCategory(categoryCode);
		if (covering.isEmpty()) {
			return 0L;
		}
		BenefitNode best = null;
		long bestDiscount = 0L;
		for (BenefitNode benefit : covering) {
			long discount = benefitDiscountEstimate(benefit, categoryName, ticket, params);
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

	/**
	 * 이 카드로 이 카테고리의 다음 구간을 여는 데 얼마나 기여하는지. tiers는 이 카드의 전체
	 * 구간, currentMonthSpend는 이번 달 누적 실적(모드 1의 전월 실적과 다르다),
	 * monthlySpendEstimate는 이 카테고리의 월 지출 추정치(통상결제액 1건), spendHistory는
	 * 연월(yyyyMM) -> 그 달 총 실적 Map(오래된 -> 최신 순서 상관없음). today는 남은 일수
	 * 계산 기준일(호출부에서 LocalDate.now()를 넘긴다).
	 */
	public static Mode2Result evaluateBuild(
		List<PerformanceTier> tiers,
		long currentMonthSpend,
		String categoryCode,
		String categoryName,
		long monthlySpendEstimate,
		Map<String, Long> spendHistory,
		RecommendationParams params,
		LocalDate today
	) {
		boolean hasAnywhere = tiers.stream().anyMatch(t -> !t.benefitsForCategory(categoryCode).isEmpty());
		if (!hasAnywhere) {
			return Mode2Result.blank(BuildStatus.NO_BENEFIT, "이 카테고리 혜택 자체가 없음");
		}

		PerformanceTier next = nextTier(tiers, currentMonthSpend, categoryCode);
		if (next == null) {
			return Mode2Result.blank(BuildStatus.TOP_TIER_SECURED, "이미 최고 구간이거나 더 열릴 구간이 없음");
		}

		RecommendationParams.Constants constants = params.constants();
		long gap = next.minimumSpending() - currentMonthSpend;
		int months = spendHistory.size();
		int hits = hits(spendHistory, next.minimumSpending());

		FillProbability prob = fillProbability(
			dailyRate(spendHistory), cv(spendHistory, constants), remainingFactor(categoryName, params, today),
			gap, hits, months, constants
		);

		PerformanceTier baseline = baselineTier(tiers, currentMonthSpend);
		long nextDiscount = tierDiscountForCategory(next, categoryCode, categoryName, monthlySpendEstimate, params);
		long baselineDiscount =
			tierDiscountForCategory(baseline, categoryCode, categoryName, monthlySpendEstimate, params);
		long gain = nextDiscount - baselineDiscount;

		double score = prob.pFill() * Math.max(0, gain);
		BuildStatus status = prob.pFill() >= constants.buildReachThreshold()
			? BuildStatus.TIER_UPGRADABLE
			: BuildStatus.HARD_TO_REACH;

		String histLabel = months > 0 ? String.format("이력 %d/%d개월 충족", hits, months) : "이력 없음";
		String note = String.format(
			"%,d원까지 %,d원 부족 · 평소 페이스로 +%,.0f원 · 충족확률 %.0f%% · 열리면 월 %+,d원 · %s",
			next.minimumSpending(), gap, prob.expectedMore(), prob.pFill() * 100, gain, histLabel
		);

		return new Mode2Result(status, prob.pFill(), prob.pFlow(), prob.pHist(),
			gap, gain, Math.round(score), hits, months, note);
	}
}
