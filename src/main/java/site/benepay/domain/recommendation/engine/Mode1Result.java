package site.benepay.domain.recommendation.engine;

/**
 * 카드 한 장에 대한 모드 1(즉시 할인) 평가 결과. category_search.py의 evaluate_now()가
 * 돌려주는 dict를 그대로 옮긴 것.
 */
public record Mode1Result(
	BenefitStatus status,
	double rate,
	double nominalRate,
	boolean capped,
	long discount,
	long amount,
	BenefitNode benefit,
	String note
) {

	static Mode1Result blank(BenefitStatus status, long typicalAmount, String note) {
		return new Mode1Result(status, 0.0, 0.0, false, 0L, typicalAmount, null, note);
	}
}
