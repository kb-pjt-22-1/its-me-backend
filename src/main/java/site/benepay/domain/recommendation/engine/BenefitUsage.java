package site.benepay.domain.recommendation.engine;

/**
 * 카드 한 장이 혜택 하나(serviceName)에 대해 이미 소진한 사용량 - card_benefit_monthly_usage
 * 집계 결과를 옮긴 것. usedAmount는 monthlyDiscountLimit과, usedMonthlyCount/usedAnnualCount는
 * monthlyCountLimit/annualCountLimit과 비교하는 데 쓴다. monthlyEligibleLimit(월 이용금액
 * 한도)은 "소진된 할인액"이 아니라 "소진된 적용대상 매출액" 기준이라 이 레코드로는 추적하지
 * 않는다(별도 누적치가 없음 - card_benefit_monthly_usage.used_amount는 CsvProcessing의
 * CardState.used_discount와 이름을 맞춘 설계라 할인액 기준으로 확정했다).
 */
public record BenefitUsage(long usedAmount, int usedMonthlyCount, int usedAnnualCount) {

	public static final BenefitUsage NONE = new BenefitUsage(0L, 0, 0);
}
