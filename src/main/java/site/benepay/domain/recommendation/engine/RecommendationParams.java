package site.benepay.domain.recommendation.engine;

import java.util.Map;

/**
 * CsvProcessing/export_params.py 산출물(recommendation_params.json)의 런타임 표현.
 * 모드 1·2에서 쓰는 필드만 담는다 - segmentShare/weekdayOrder는 이번 범위 밖이라 안 씀
 * (JSON에는 있지만 여기 모델엔 없음, 로더가 무시하도록 설정함).
 */
public record RecommendationParams(
	Map<String, Long> typicalPaymentAmount,
	TicketHistogram ticketHistogram,
	// 대분류 -> 요일별(월~일) 가중치 7개, 평균 1. weekdayOrder가 항상 월~일 순이라는
	// 전제로 index 0을 월요일로 가정한다(파일 자체를 파싱해 확인하지 않음).
	Map<String, double[]> weekdayIndex,
	Constants constants
) {

	public record TicketHistogram(double[] centers, Map<String, long[]> counts) {

		/**
		 * INTEGRATION.md의 passRate 공식 그대로: 이 대분류 결제 중 threshold 이상인 비율.
		 */
		public double passRate(String major, long threshold) {
			long[] counts = this.counts.get(major);
			if (counts == null) {
				return 1.0;
			}
			long total = 0;
			long hit = 0;
			for (int i = 0; i < centers.length; i++) {
				total += counts[i];
				if (centers[i] >= threshold) {
					hit += counts[i];
				}
			}
			return total == 0 ? 1.0 : (double) hit / total;
		}
	}

	/**
	 * 모드 2 확률·기대값 계산에 쓰는 상수. historyPrior~buildReachThreshold는 전부
	 * CsvProcessing/category_search.py의 모듈 상수와 값이 같다(파라미터화된 것뿐).
	 */
	public record Constants(
		double fuelPricePerLiter,
		double historyPrior,
		double priorStrength,
		double defaultCv,
		double minCv,
		double pFlowMin,
		double pFlowMax,
		double buildReachThreshold
	) {
	}
}
