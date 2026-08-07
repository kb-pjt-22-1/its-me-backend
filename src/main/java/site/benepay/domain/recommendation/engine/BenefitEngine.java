package site.benepay.domain.recommendation.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 모드 1(즉시 할인) 계산 엔진. CsvProcessing/benefits.py + category_search.py의
 * evaluate_now() 경로를 그대로 포팅한 것 - 모드 2/3(실적 채우기, 우선순위 비교)은
 * 여기 없다(다음 단계).
 *
 * <p>혜택마다 건당 최소 결제액이 다르므로 각 혜택을 자기 하한가격에서 평가하고,
 * 금액이 달라도 <b>할인율</b>로 비교한다(README "4. 모드 1").</p>
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
}
