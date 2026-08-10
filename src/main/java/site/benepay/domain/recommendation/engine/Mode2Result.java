package site.benepay.domain.recommendation.engine;

/**
 * 카드 한 장에 대한 모드 2(실적 채우기) 평가 결과. category_search.py의 evaluate_build()가
 * 돌려주는 dict를 그대로 옮긴 것.
 */
public record Mode2Result(
	BuildStatus status,
	double reachProbability,
	double pFlow,
	double pHist,
	long gapAmount,
	long gainAmount,
	long expectedValue,
	int hitsMonths,
	int observedMonths,
	String note
) {

	static Mode2Result blank(BuildStatus status, String note) {
		return new Mode2Result(status, 0.0, 0.0, 0.0, 0L, 0L, 0L, 0, 0, note);
	}
}
