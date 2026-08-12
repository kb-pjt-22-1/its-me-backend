package site.benepay.domain.recommendation.engine;

/**
 * 모드 3(우선순위 비교) 평가 결과. category_search.py의 evaluate_priority()가 돌려주는
 * dict를 그대로 옮긴 것. now(이번 달 확정)와 future(다음 달 기대치)를 beta로 합산한
 * total이 카드 랭킹 기준이다 - 상태 분류(BenefitStatus/BuildStatus)가 없다는 게
 * 모드 1/2와의 차이다.
 */
public record Mode3Result(
	long now,
	double future,
	double pRoute,
	double pFlow,
	double pHist,
	long gap,
	long gain,
	double total,
	String note
) {

	static Mode3Result blank(String note) {
		return new Mode3Result(0L, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0.0, note);
	}
}
