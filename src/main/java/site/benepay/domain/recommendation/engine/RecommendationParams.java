package site.benepay.domain.recommendation.engine;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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

		// centers가 배열이라 record 기본 equals/hashCode는 내용이 아니라 참조를 비교한다
		// (java:S6218) - 배열 내용을 비교하도록 직접 오버라이드한다. counts는 Map<String, long[]>라
		// Map.equals()에 그냥 맡기면 long[] 값도 참조 비교가 되므로 항목별로 Arrays.equals를 돌린다.
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof TicketHistogram other)) {
				return false;
			}
			if (!Arrays.equals(centers, other.centers) || !counts.keySet().equals(other.counts.keySet())) {
				return false;
			}
			for (Map.Entry<String, long[]> entry : counts.entrySet()) {
				if (!Arrays.equals(entry.getValue(), other.counts.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			// counts는 순서 보장이 없는 Map이라, 항목 해시를 곱셈 누적이 아니라 합산해
			// 순서와 무관하게 같은 값이 나오도록 한다(hashCode 계약: 같은 equals면 같은 hashCode).
			int countsHash = 0;
			for (Map.Entry<String, long[]> entry : counts.entrySet()) {
				countsHash += Objects.hash(entry.getKey(), Arrays.hashCode(entry.getValue()));
			}
			return Objects.hash(Arrays.hashCode(centers), countsHash);
		}

		@Override
		public String toString() {
			return "TicketHistogram[centers=" + Arrays.toString(centers) + ", counts=" + counts + "]";
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
