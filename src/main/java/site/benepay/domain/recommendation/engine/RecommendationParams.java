package site.benepay.domain.recommendation.engine;

import java.util.Map;

/**
 * CsvProcessing/export_params.py 산출물(recommendation_params.json)의 런타임 표현.
 * 모드 1에서 실제로 쓰는 필드만 담는다 - weekdayIndex/segmentShare 등 모드 2/3 전용
 * 필드는 아직 안 씀(JSON에는 있지만 여기 모델엔 없음, 로더가 무시하도록 설정함).
 */
public record RecommendationParams(
	Map<String, Long> typicalPaymentAmount,
	TicketHistogram ticketHistogram,
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

	public record Constants(double fuelPricePerLiter) {
	}
}
